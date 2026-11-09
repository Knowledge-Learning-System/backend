package cn.edu.bcu.learning.service;

import cn.edu.bcu.learning.domain.vo.AiAnalysisVO;
import cn.edu.bcu.learning.domain.vo.RadarItemVO;
import cn.edu.bcu.learning.domain.vo.TrendPointVO;
import cn.edu.bcu.learning.domain.vo.WeakPointVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 诊断页「大模型分析建议」：汇聚 radar / trend / weak-points 三路诊断数据，
 * 交给大模型生成结构化的诊断结论与学习建议。仅本模块使用，不影响既有诊断逻辑。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiagnosisAiService {

    private final DiagnosisService diagnosisService;
    private final CourseService courseService;
    private final AiService aiService;

    /** 同一学生+课程+天数的分析结果缓存时长 */
    private static final long CACHE_TTL_MILLIS = 10 * 60 * 1000L;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    /** 送入大模型的知识点上限，避免超长 */
    private static final int MAX_POINTS_IN_PROMPT = 24;

    private static final String SYSTEM_PROMPT = String.join("\n",
            "你是高校个性化在线学习系统中的「学习诊断专家」。",
            "用户会给出某位学生在某门课程上的知识点掌握度、累计答题正确率、掌握度趋势和薄弱点数据。",
            "请基于这些数据给出针对性的诊断与学习建议，要求：",
            "1. 结论必须来自给定数据，不得编造未出现的知识点、数值或事件；",
            "2. 绝对禁止描述本平台不存在的功能、界面、事件、对象或资源，例如：已标红、已高亮、已推送、消息提醒、已预约、直播、课程表、日历、打卡、签到、徽章、排行榜、积分、证书、资源编号（如 DB-REL-01），以及教材、讲义、课件、微课、题库、试卷、真题等上文未给出的资源。只能建议学生自己去做的学习动作；",
            "3. 知识点名称必须逐字取自给定的知识点列表，不得把知识领域名称（如「数据库基础」）当作知识点，不得用近义词替换；建议里提到的具体技术术语、函数、语法、资源也必须是给定列表中出现过的，列表里没有的不要点名，改用不点名的通用动作（如「重做该知识点下的错题」）；",
            "4. 严格区分两个口径：「掌握度」只能引用知识点掌握度数据；累计答对率只能用于描述答题情况，不得说成掌握度；trend 字段只能描述累计答对率的变化，不得称为掌握度变化，两个百分比不得混用；",
            "5. 所有时间表述必须带口径（如「近 30 天」「历史累计」）；逐日样本的条数不代表连续天数，禁止推断「连续 N 天」；",
            "6. 薄弱点列表中的知识点一律不得写入 strengths；strengths 只能从【可引用的优势知识点】列表中选择，该列表为空时输出空数组；",
            "7. weaknesses 只能从【薄弱点】列表中选择；掌握度为 0 且从未作答的知识点不算薄弱环节，不得用来凑数，条数不足就少给；",
            "8. 提到某个知识点前先看它的作答次数：作答次数为 0 的只能说「开始学习」「完成基础练习」，严禁说「重做」「复习错题」「再练一遍」，因为它没有可重做的题目；",
            "9. 面向学生本人，用第二人称，专业但易懂；建议必须具体可执行，禁止空泛；",
            "10. 只输出一个 JSON 对象，不要输出 markdown 代码块或任何解释文字；",
            "JSON 结构：{\"overview\":\"60-120字的总体判断\",\"strengths\":[\"0-3条优势，引用具体知识点名称\"],"
                    + "\"weaknesses\":[\"2-3条薄弱环节，引用具体知识点名称\"],"
                    + "\"suggestions\":[\"3-4条下一步可执行的动作\"],\"trend\":\"40字以内对掌握度变化的点评\"}"
    );

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final class CacheEntry {
        final String fingerprint;
        final long time;
        final AiAnalysisVO value;

        CacheEntry(String fingerprint, long time, AiAnalysisVO value) {
            this.fingerprint = fingerprint;
            this.time = time;
            this.value = value;
        }
    }

    /**
     * 生成（或复用）学习诊断建议。
     *
     * @param days 趋势回看天数
     */
    public AiAnalysisVO analyze(Integer userId, Integer courseId, String courseName, String source, Integer days) {
        int safeDays = (days == null || days <= 0) ? 30 : days;
        String cacheKey = userId + ":" + courseId + ":" + safeDays;

        List<RadarItemVO> radar = safeList(diagnosisService.getRadar(userId, source, "overall"));
        List<RadarItemVO> domains = safeList(diagnosisService.getRadar(userId, source, "by_domain"));
        List<TrendPointVO> trend = safeList(diagnosisService.getTrend(userId, source, safeDays));
        List<WeakPointVO> weakPoints = safeList(diagnosisService.getWeakPoints(userId, source));

        if (radar.isEmpty() && domains.isEmpty() && trend.isEmpty()) {
            AiAnalysisVO empty = new AiAnalysisVO();
            empty.setOverview("该课程暂无掌握度数据，先完成一次课程测评，我才能结合你的答题情况给出诊断建议。");
            empty.setTrend("");
            empty.setGeneratedAt(now());
            return empty;
        }

        String fingerprint = buildFingerprint(radar, trend, weakPoints);
        CacheEntry hit = cache.get(cacheKey);
        if (hit != null
                && hit.fingerprint.equals(fingerprint)
                && System.currentTimeMillis() - hit.time < CACHE_TTL_MILLIS) {
            AiAnalysisVO cachedView = copyOf(hit.value);
            cachedView.setCached(Boolean.TRUE);
            return cachedView;
        }

        if (!aiService.isConfigured()) {
            AiAnalysisVO vo = new AiAnalysisVO();
            vo.setOverview("大模型服务尚未配置 API Key，暂时无法生成分析建议，请联系管理员配置后再试。");
            vo.setGeneratedAt(now());
            return vo;
        }

        String courseTitle = (courseName == null || courseName.isBlank()) ? "当前课程" : courseName;
        String userPrompt = buildUserPrompt(courseTitle, safeDays, radar, domains, trend, weakPoints);

        AiAnalysisVO vo;
        try {
            String answer = aiService.complete(SYSTEM_PROMPT, userPrompt);
            vo = parseAnswer(answer);
            if (vo == null) {
                throw new IllegalStateException("大模型返回内容无法解析");
            }
            Set<Integer> allowed = allowedPercents(radar, domains, trend, weakPoints);
            if (anyFabricated(vo, allowed)) {
                log.warn("大模型输出含虚构内容，触发一次重试: userId={}, courseId={}", userId, courseId);
                String retryPrompt = userPrompt
                        + "\n上一次输出出现了上面数据中不存在的数值或平台不存在的功能，请只使用上面给出的数值和知识点名称重新输出。";
                AiAnalysisVO retryVo = parseAnswer(aiService.complete(SYSTEM_PROMPT, retryPrompt));
                if (retryVo != null) {
                    vo = retryVo;
                }
            }
            vo = sanitize(vo, radar, trend, weakPoints, allowed);
            vo.setGeneratedAt(now());
            vo.setCached(false);
            cache.put(cacheKey, new CacheEntry(fingerprint, System.currentTimeMillis(), vo));
            return vo;
        } catch (Exception e) {
            log.warn("大模型诊断分析生成失败: userId={}, courseId={}, err={}", userId, courseId, e.getMessage());
            AiAnalysisVO fail = new AiAnalysisVO();
            fail.setOverview("大模型分析暂时不可用，请稍后点击「重新分析」。");
            fail.setGeneratedAt(now());
            return fail;
        }
    }

    // ---- 内部方法 ----

    private <T> List<T> safeList(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }

    private String now() {
        return LocalDateTime.now().format(TIME_FMT);
    }

    private String buildFingerprint(List<RadarItemVO> radar, List<TrendPointVO> trend, List<WeakPointVO> weakPoints) {
        int masterySum = radar.stream().mapToInt(r -> r.getMastery() == null ? 0 : r.getMastery()).sum();
        int lastMastery = trend.isEmpty() || trend.get(trend.size() - 1).getMastery() == null
                ? 0 : trend.get(trend.size() - 1).getMastery();
        return radar.size() + "|" + masterySum + "|" + trend.size() + "|" + lastMastery + "|" + weakPoints.size();
    }

    private String buildUserPrompt(String courseTitle, int days,
                                   List<RadarItemVO> radar, List<RadarItemVO> domains,
                                   List<TrendPointVO> trend, List<WeakPointVO> weakPoints) {
        StringBuilder sb = new StringBuilder();
        sb.append("课程：").append(courseTitle).append("\n");
        sb.append("统计口径：近 ").append(days).append(" 天\n\n");

        // 知识点掌握度（按掌握度从低到高，突出短板）
        List<RadarItemVO> sorted = new ArrayList<>(radar);
        sorted.sort(Comparator.comparingInt(r -> r.getMastery() == null ? 0 : r.getMastery()));
        sb.append("【知识点掌握度】共 ").append(sorted.size()).append(" 个");
        if (sorted.size() > MAX_POINTS_IN_PROMPT) {
            sb.append("（下列出掌握度最低的 ").append(MAX_POINTS_IN_PROMPT).append(" 个）");
        }
        sb.append("：\n");
        int limit = Math.min(sorted.size(), MAX_POINTS_IN_PROMPT);
        for (int i = 0; i < limit; i++) {
            RadarItemVO r = sorted.get(i);
            sb.append("- ").append(r.getName()).append("：").append(r.getMastery() == null ? 0 : r.getMastery()).append("%");
            if (r.getLevel() != null && !r.getLevel().isBlank()) {
                sb.append("（").append(r.getLevel()).append("）");
            }
            sb.append("\n");
        }
        if (sorted.size() > limit) {
            int answered = (int) radar.stream().filter(r -> r.getMastery() != null && r.getMastery() > 0).count();
            sb.append("- 另有 ").append(sorted.size() - limit).append(" 个知识点掌握度不低于上列最低值，其中已作答过的知识点 ").append(answered).append(" 个\n");
        }

        // 可引用的优势知识点（掌握度较高，供 strengths 引用，避免模型看不到而输出空）
        List<RadarItemVO> strong = radar.stream()
                .filter(r -> r.getMastery() != null && r.getMastery() >= 60)
                .sorted(Comparator.comparingInt((RadarItemVO r) -> r.getMastery()).reversed())
                .limit(10)
                .collect(java.util.stream.Collectors.toList());
        if (!strong.isEmpty()) {
            sb.append("\n【可引用的优势知识点（掌握度 ≥60%）】\n");
            for (RadarItemVO r : strong) {
                sb.append("- ").append(r.getName()).append("：").append(r.getMastery()).append("%");
                if (r.getLevel() != null && !r.getLevel().isBlank()) {
                    sb.append("（").append(r.getLevel()).append("）");
                }
                sb.append("\n");
            }
            sb.append("strengths 只能从这个列表中选择，最多 3 条，且不得同时出现在薄弱点里。\n");
        } else {
            sb.append("\n【可引用的优势知识点】本课程暂无掌握度 ≥60% 的知识点，strengths 必须输出空数组。\n");
        }

        // 知识领域聚合
        if (!domains.isEmpty()) {
            sb.append("\n【知识领域掌握度】\n");
            for (RadarItemVO d : domains) {
                sb.append("- ").append(d.getName()).append("：").append(d.getMastery() == null ? 0 : d.getMastery()).append("%\n");
            }
        }

        // 薄弱点
        if (!weakPoints.isEmpty()) {
            sb.append("\n【薄弱点（历史累计口径，不受近 ").append(days).append(" 天限制；含答题统计）】\n");
            for (WeakPointVO w : weakPoints) {
                sb.append("- ").append(w.getName())
                        .append("：掌握度 ").append(w.getMastery() == null ? 0 : w.getMastery()).append("%")
                        .append("，作答 ").append(w.getTotalAttempts() == null ? 0 : w.getTotalAttempts()).append(" 次")
                        .append("，答错 ").append(w.getErrorCount() == null ? 0 : w.getErrorCount()).append(" 次\n");
            }
        }

        // 趋势
        if (!trend.isEmpty()) {
            TrendPointVO first = trend.get(0);
            TrendPointVO last = trend.get(trend.size() - 1);
            int attempts = trend.stream().mapToInt(t -> t.getTotalAttempts() == null ? 0 : t.getTotalAttempts()).sum();
            int correct = trend.stream().mapToInt(t -> t.getCorrectAttempts() == null ? 0 : t.getCorrectAttempts()).sum();
            int accuracy = attempts == 0 ? 0 : (int) Math.round(correct * 100.0 / attempts);
            int delta = (last.getMastery() == null ? 0 : last.getMastery()) - (first.getMastery() == null ? 0 : first.getMastery());
            sb.append("\n【趋势：累计答对率变化（近 ").append(days).append(" 天；这是答对率，不是知识点掌握度）】\n");
            sb.append("- ").append(first.getDate()).append("：").append(first.getMastery()).append("%")
                    .append(" → ").append(last.getDate()).append("：").append(last.getMastery()).append("%")
                    .append("（均为当日累计答对率），累计变化 ").append(delta >= 0 ? "+" : "").append(delta).append(" 个百分点\n");
            sb.append("- 区间答题 ").append(attempts).append(" 道，答对 ").append(correct).append(" 道，累计答题正确率 ").append(accuracy)
                    .append("%（这是答题正确率，不是掌握度，不得混用）\n");
            int activeDays = (int) trend.stream().filter(t -> t.getTotalAttempts() != null && t.getTotalAttempts() > 0).count();
            sb.append("- 近 ").append(days).append(" 天内有答题记录的天数：").append(activeDays).append(" 天\n");
            StringBuilder detail = new StringBuilder();
            int startIdx = Math.max(0, trend.size() - 14);
            for (int i = startIdx; i < trend.size(); i++) {
                TrendPointVO p = trend.get(i);
                if (i > startIdx) detail.append("，");
                detail.append(p.getDate().substring(Math.max(0, p.getDate().length() - 5)))
                        .append(" ").append(p.getMastery()).append("%");
            }
            sb.append("- 最近 ").append(trend.size() - startIdx).append(" 个逐日累计答对率采样点（采样点，不代表连续学习天数）：").append(detail).append("\n");
        } else {
            sb.append("\n【趋势：累计答对率变化（近 ").append(days).append(" 天）】\n- 近 ").append(days).append(" 天暂无答题记录\n");
        }

        sb.append("\n注意：上面给出的知识点名称、百分比、次数是本课程真实存在的全部数据。")
                .append("只能使用这些名称和数值；不得提到任何平台功能、页面元素、通知、直播、编号等上面未出现的内容。\n");
        sb.append("\n请严格按系统要求的 JSON 结构输出诊断结果。");
        return sb.toString();
    }

    private AiAnalysisVO parseAnswer(String answer) {
        if (answer == null || answer.isBlank()) return null;
        String text = answer.trim();
        // 容错：剥离可能出现的 markdown 代码块标记
        if (text.startsWith("```")) {
            int nl = text.indexOf('\n');
            if (nl > 0) text = text.substring(nl + 1);
            if (text.endsWith("```")) text = text.substring(0, text.length() - 3);
            text = text.trim();
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) return null;
        String json = text.substring(start, end + 1);
        try {
            JsonNode root = objectMapper.readTree(json);
            AiAnalysisVO vo = new AiAnalysisVO();
            vo.setOverview(textOf(root, "overview"));
            vo.setTrend(textOf(root, "trend"));
            vo.setStrengths(listOf(root, "strengths"));
            vo.setWeaknesses(listOf(root, "weaknesses"));
            vo.setSuggestions(listOf(root, "suggestions"));
            if (vo.getOverview().isBlank()
                    && vo.getStrengths().isEmpty()
                    && vo.getWeaknesses().isEmpty()
                    && vo.getSuggestions().isEmpty()) {
                return null;
            }
            if (vo.getOverview().isBlank()) {
                vo.setOverview("已根据你当前的掌握度数据生成分析，详见下方各项。");
            }
            return vo;
        } catch (Exception e) {
            log.warn("大模型诊断结果解析失败: {}", e.getMessage());
            return null;
        }
    }

    private String textOf(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) return "";
        if (node.isArray()) {
            List<String> parts = new ArrayList<>();
            node.forEach(n -> {
                String s = n.asText("").trim();
                if (!s.isEmpty()) parts.add(s);
            });
            return String.join(" ", parts);
        }
        return node.asText("").trim();
    }

    private List<String> listOf(JsonNode root, String field) {
        List<String> result = new ArrayList<>();
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) return result;
        if (node.isArray()) {
            node.forEach(n -> {
                String s = n.asText("").trim();
                if (!s.isEmpty()) result.add(s);
            });
        } else {
            String s = node.asText("").trim();
            if (!s.isEmpty()) result.add(s);
        }
        return result;
    }

    /** 平台不存在的功能 / 对象，命中即视为虚构（本系统没有标红、推送、直播、资源编号等能力） */
    private static final Pattern FABRICATED_FEATURE = Pattern.compile(
            "标红|高亮|已推送|推送|站内信|消息提醒|系统提醒|已提醒|提醒卡片|提醒功能|"
                    + "站内通知|系统通知|已通知|通知栏|已预约|预约记录|预约直播|预约功能|直播|"
                    + "课程表|教学计划|日历|打卡|签到|徽章|排行榜|积分|证书|学习报告|"
                    + "资源编号|编号为|[A-Z]{2,5}-\\d+|教材|讲义|课件|微课|题库|试卷|真题|习题集");

    private static final Pattern PERCENT = Pattern.compile("(\\d{1,3})\\s*[%％]");

    /** 采集本题真实数据里出现过的全部百分比，只有这些数值才允许出现在输出中 */
    private Set<Integer> allowedPercents(List<RadarItemVO> radar, List<RadarItemVO> domains,
                                         List<TrendPointVO> trend, List<WeakPointVO> weakPoints) {
        Set<Integer> allowed = new HashSet<>();
        allowed.add(0);
        allowed.add(100);
        radar.forEach(r -> {
            if (r.getMastery() != null) allowed.add(r.getMastery());
        });
        if (!radar.isEmpty()) {
            allowed.add((int) Math.round(radar.stream()
                    .mapToInt(r -> r.getMastery() == null ? 0 : r.getMastery()).average().orElse(0)));
            allowed.add(radar.size());
            allowed.add((int) radar.stream().filter(r -> r.getMastery() != null && r.getMastery() > 0).count());
        }
        domains.forEach(d -> {
            if (d.getMastery() != null) allowed.add(d.getMastery());
        });
        weakPoints.forEach(w -> {
            if (w.getMastery() != null) allowed.add(w.getMastery());
            if (w.getTotalAttempts() != null) allowed.add(w.getTotalAttempts());
            if (w.getErrorCount() != null) allowed.add(w.getErrorCount());
        });
        List<TrendPointVO> safeTrend = safeList(trend);
        int attempts = 0;
        int correct = 0;
        for (TrendPointVO t : safeTrend) {
            if (t.getMastery() != null) allowed.add(t.getMastery());
            attempts += t.getTotalAttempts() == null ? 0 : t.getTotalAttempts();
            correct += t.getCorrectAttempts() == null ? 0 : t.getCorrectAttempts();
        }
        allowed.add(attempts);
        allowed.add(correct);
        allowed.add(safeTrend.size());
        if (attempts > 0) {
            allowed.add((int) Math.round(correct * 100.0 / attempts));
        }
        // 允许相对变化量（百分点差值）
        if (!safeTrend.isEmpty()) {
            int first = safeTrend.get(0).getMastery() == null ? 0 : safeTrend.get(0).getMastery();
            int last = safeTrend.get(safeTrend.size() - 1).getMastery() == null
                    ? 0 : safeTrend.get(safeTrend.size() - 1).getMastery();
            allowed.add(Math.abs(last - first));
        }
        return allowed;
    }

    /** 阈值类表述（如「60% 以上」「至少 10%」「约 5%」）中的数字不是数据点，无需字面溯源 */
    private static final Pattern THRESHOLD_CTX = Pattern.compile(
            "以上|以下|超过|不足|至少|低于|高于|达到|达|接近|约|左右|多于|少于");

    /** 文本是否含虚构内容：不存在的功能词，或数据中不存在的百分比 */
    private boolean fabricated(String text, Set<Integer> allowed) {
        if (text == null || text.isBlank()) return false;
        if (FABRICATED_FEATURE.matcher(text).find()) return true;
        Matcher m = PERCENT.matcher(text);
        while (m.find()) {
            int v = Integer.parseInt(m.group(1));
            if (allowed.contains(v)) continue;
            int s = Math.max(0, m.start() - 5);
            int e = Math.min(text.length(), m.end() + 3);
            if (THRESHOLD_CTX.matcher(text.substring(s, e)).find()) continue;
            return true;
        }
        return false;
    }

    private boolean anyFabricated(AiAnalysisVO vo, Set<Integer> allowed) {
        if (vo == null) return true;
        if (fabricated(vo.getOverview(), allowed) || fabricated(vo.getTrend(), allowed)) return true;
        for (String s : safeList(vo.getStrengths())) if (fabricated(s, allowed)) return true;
        for (String s : safeList(vo.getWeaknesses())) if (fabricated(s, allowed)) return true;
        for (String s : safeList(vo.getSuggestions())) if (fabricated(s, allowed)) return true;
        return false;
    }

    /**
     * 后置硬校验：剔除一切虚构条目，剔除落在薄弱点里的“优势”，
     * 缺口用真实数据拼装的纯数据描述兜底，保证输出不出现平台上不存在的东西。
     */
    private AiAnalysisVO sanitize(AiAnalysisVO vo, List<RadarItemVO> radar,
                                  List<TrendPointVO> trend, List<WeakPointVO> weakPoints,
                                  Set<Integer> allowed) {
        List<WeakPointVO> safeWeak = safeList(weakPoints);
        Set<String> weakNames = new HashSet<>();
        for (WeakPointVO w : safeWeak) {
            if (w.getName() != null && !w.getName().isBlank()) weakNames.add(w.getName().trim());
        }

        List<String> strengths = new ArrayList<>();
        for (String s : safeList(vo.getStrengths())) {
            if (fabricated(s, allowed)) continue;
            boolean hitsWeak = weakNames.stream().anyMatch(s::contains);
            if (hitsWeak) continue;
            strengths.add(s);
        }

        List<String> weaknesses = new ArrayList<>();
        for (String s : safeList(vo.getWeaknesses())) {
            if (!fabricated(s, allowed)) weaknesses.add(s);
        }

        List<String> suggestions = new ArrayList<>();
        for (String s : safeList(vo.getSuggestions())) {
            if (!fabricated(s, allowed)) suggestions.add(s);
        }
        if (suggestions.isEmpty()) suggestions = fallbackSuggestions(safeWeak);
        if (weaknesses.isEmpty()) weaknesses = fallbackWeaknesses(safeWeak);

        String overview = vo.getOverview();
        if (fabricated(overview, allowed)) {
            log.warn("[ai-analysis] overview 被判定为虚构，已改用数据兜底。原文={}", abbreviate(overview));
            overview = "";
        }
        // 断言“掌握度均为 0%”但雷达里存在非零项时，点名该项，避免文本与数据自相矛盾
        String topName = safeList(radar).stream()
                .filter(r -> r.getMastery() != null && r.getMastery() > 0)
                .max(java.util.Comparator.comparingInt(RadarItemVO::getMastery))
                .map(RadarItemVO::getName).orElse(null);
        if (topName != null && overview != null && !overview.contains("除「" + topName + "」外")) {
            if (overview.matches("(?s).*所有知识点掌握度[均都]为\\s*0%.*")) {
                overview = overview.replaceFirst("所有知识点掌握度[均都]为\\s*0%",
                        "除「" + topName + "」外其余知识点掌握度均为 0%");
            } else if (overview.matches("(?s).*掌握度[均都]为\\s*0%.*")) {
                overview = overview.replaceFirst("掌握度[均都]为\\s*0%",
                        "除「" + topName + "」外掌握度均为 0%");
            }
        }
        if (overview == null || overview.isBlank()) overview = fallbackOverview(safeList(radar), safeWeak);

        String trendText = fabricated(vo.getTrend(), allowed) ? "" : vo.getTrend();
        if (trendText.isBlank()) trendText = fallbackTrend(trend);

        AiAnalysisVO out = new AiAnalysisVO();
        out.setOverview(overview);
        out.setStrengths(strengths);
        out.setWeaknesses(weaknesses);
        out.setSuggestions(suggestions);
        out.setTrend(trendText);
        out.setGeneratedAt(vo.getGeneratedAt());
        out.setCached(vo.getCached());
        return out;
    }

    /** 日志用：截断过长的模型原文 */
    private String abbreviate(String s) {
        if (s == null) return "null";
        return s.length() <= 200 ? s : s.substring(0, 200) + "...";
    }

    private String fallbackOverview(List<RadarItemVO> radar, List<WeakPointVO> weakPoints) {
        int total = radar.size();
        int started = (int) radar.stream().filter(r -> r.getMastery() != null && r.getMastery() > 0).count();
        int avg = total == 0 ? 0 : (int) Math.round(radar.stream()
                .mapToInt(r -> r.getMastery() == null ? 0 : r.getMastery()).average().orElse(0));
        StringBuilder sb = new StringBuilder();
        sb.append("本课程共 ").append(total).append(" 个知识点，已开始学习 ").append(started)
                .append(" 个，平均掌握度约 ").append(avg).append("%。");
        if (!weakPoints.isEmpty()) {
            sb.append("薄弱环节集中在");
            for (int i = 0; i < Math.min(3, weakPoints.size()); i++) {
                if (i > 0) sb.append("、");
                sb.append("《").append(weakPoints.get(i).getName()).append("》(")
                        .append(weakPoints.get(i).getMastery() == null ? 0 : weakPoints.get(i).getMastery()).append("%)");
            }
            sb.append("，建议按掌握度从低到高依次补齐。");
        } else {
            sb.append("暂无被判定为薄弱的环节，建议扩大练习覆盖面。");
        }
        return sb.toString();
    }

    private List<String> fallbackWeaknesses(List<WeakPointVO> weakPoints) {
        List<String> list = new ArrayList<>();
        List<WeakPointVO> sorted = new ArrayList<>(weakPoints);
        sorted.sort(Comparator.comparingInt(w -> w.getMastery() == null ? 0 : w.getMastery()));
        for (int i = 0; i < Math.min(3, sorted.size()); i++) {
            WeakPointVO w = sorted.get(i);
            list.add("（" + w.getName() + "）掌握度偏低，错题集中在基础概念上");
        }
        return list;
    }

    private List<String> fallbackSuggestions(List<WeakPointVO> weakPoints) {
        List<String> list = new ArrayList<>();
        List<WeakPointVO> sorted = new ArrayList<>(weakPoints);
        sorted.sort(Comparator.comparingInt(w -> w.getMastery() == null ? 0 : w.getMastery()));
        for (int i = 0; i < Math.min(3, sorted.size()); i++) {
            WeakPointVO w = sorted.get(i);
            list.add("先复习《" + w.getName() + "》的核心概念，再做 3 道该知识点的基础题，做完回诊断页刷新掌握度");
        }
        if (list.isEmpty()) {
            list.add("先完成一次课程测评，拿到掌握度数据后再按薄弱点安排学习");
        }
        return list;
    }

    private String fallbackTrend(List<TrendPointVO> trend) {
        List<TrendPointVO> safeTrend = safeList(trend);
        if (safeTrend.isEmpty()) return "近 30 天暂无答题记录";
        TrendPointVO first = safeTrend.get(0);
        TrendPointVO last = safeTrend.get(safeTrend.size() - 1);
        int f = first.getMastery() == null ? 0 : first.getMastery();
        int l = last.getMastery() == null ? 0 : last.getMastery();
        return "近 " + safeTrend.size() + " 个采样日的累计答对率从 " + f + "% 变为 " + l + "%，累计变化 "
                + (l - f >= 0 ? "+" : "") + (l - f) + " 个百分点";
    }

    private AiAnalysisVO copyOf(AiAnalysisVO src) {
        AiAnalysisVO vo = new AiAnalysisVO();
        vo.setOverview(src.getOverview());
        vo.setTrend(src.getTrend());
        vo.setStrengths(new ArrayList<>(src.getStrengths()));
        vo.setWeaknesses(new ArrayList<>(src.getWeaknesses()));
        vo.setSuggestions(new ArrayList<>(src.getSuggestions()));
        vo.setGeneratedAt(src.getGeneratedAt());
        vo.setCached(src.getCached());
        return vo;
    }
}
