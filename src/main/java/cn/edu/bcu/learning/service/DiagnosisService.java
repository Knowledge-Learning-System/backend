package cn.edu.bcu.learning.service;

import cn.edu.bcu.learning.domain.entity.Course;
import cn.edu.bcu.learning.domain.entity.KnowledgeMastery;
import cn.edu.bcu.learning.domain.entity.UserAnswerRecord;
import cn.edu.bcu.learning.domain.vo.*;
import cn.edu.bcu.learning.repository.mysql.KnowledgeMasteryMapper;
import cn.edu.bcu.learning.repository.mysql.UserAnswerRecordMapper;
import cn.edu.bcu.learning.repository.neo4j.KnowledgeGraphRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiagnosisService {

    private final UserAnswerRecordMapper userAnswerRecordMapper;
    private final KnowledgeMasteryMapper knowledgeMasteryMapper;
    private final KnowledgeGraphRepository knowledgeGraphRepository;
    private final CourseService courseService;
    private final CognitiveDiagnosisService cognitiveDiagnosisService;

    /**
     * 根据答题记录计算所有知识点的掌握度，写入 knowledge_mastery 表。
     * 公式：mastery = correctCount / totalAttempts * 100
     */
    @Async
    public void calculateMastery(Integer userId, Integer courseId, String source) {
        try {
            List<String> allKpIds = knowledgeGraphRepository.findAllKpIdsBySource(source);
            if (allKpIds.isEmpty()) return;

            List<UserAnswerRecord> records = userAnswerRecordMapper.selectList(
                    new LambdaQueryWrapper<UserAnswerRecord>()
                            .eq(UserAnswerRecord::getUserId, userId)
                            .in(UserAnswerRecord::getKnowledgePointId, allKpIds));

            // 按知识点分组统计
            Map<String, Integer> totalMap = new HashMap<>();
            Map<String, Integer> correctMap = new HashMap<>();
            for (UserAnswerRecord r : records) {
                String kpId = r.getKnowledgePointId();
                totalMap.merge(kpId, 1, Integer::sum);
                if (Boolean.TRUE.equals(r.getIsCorrect())) {
                    correctMap.merge(kpId, 1, Integer::sum);
                }
            }

            for (String kpId : allKpIds) {
                int total = totalMap.getOrDefault(kpId, 0);
                int correct = correctMap.getOrDefault(kpId, 0);
                int mastery = total == 0 ? 0 : (int) Math.round((double) correct / total * 100);

                // upsert
                KnowledgeMastery km = knowledgeMasteryMapper.selectOne(
                        new LambdaQueryWrapper<KnowledgeMastery>()
                                .eq(KnowledgeMastery::getUserId, userId)
                                .eq(KnowledgeMastery::getKnowledgePointId, kpId));
                if (km == null) {
                    km = new KnowledgeMastery();
                    km.setUserId(userId);
                    km.setKnowledgePointId(kpId);
                    km.setCourseId(courseId);
                }
                km.setMasteryLevel(mastery);
                km.setTotalAttempts(total);
                km.setCorrectAttempts(correct);
                km.setLastAttemptTime(LocalDateTime.now());
                knowledgeMasteryMapper.insertOrUpdate(km);
            }
        } catch (Exception e) {
            log.error("异步计算掌握度失败 userId={} courseId={}", userId, courseId, e);
        }
    }

    /** 分析维度：整体掌握度 */
    public static final String DIMENSION_OVERALL = "overall";
    /** 分析维度：按知识领域（章节）聚合 */
    public static final String DIMENSION_DOMAIN = "by_domain";
    /** 分析维度：按时间趋势（序列由 getTrend 提供，radar 侧回落到整体掌握度） */
    public static final String DIMENSION_TIME = "by_time";

    /** 未归入任何章节的知识点在按领域聚合时的兜底领域 */
    private static final String UNCLASSIFIED_DOMAIN_ID = "UNCLASSIFIED";
    private static final String UNCLASSIFIED_DOMAIN_NAME = "未分类";

    /**
     * 雷达图数据：基于 DINA 认知诊断估计各知识点掌握概率（0-100）
     */
    public List<RadarItemVO> getRadar(Integer userId, String source) {
        return getRadar(userId, source, DIMENSION_OVERALL);
    }

    /**
     * 按分析维度取掌握度数据。
     * - overall / by_time：逐个知识点的掌握度（by_time 时间序列请调用 getTrend）
     * - by_domain：按知识领域（章节）聚合的掌握度，id 为章节 id、name 为章节名
     */
    public List<RadarItemVO> getRadar(Integer userId, String source, String dimension) {
        if (DIMENSION_DOMAIN.equalsIgnoreCase(dimension)) {
            return getDomainRadar(userId, source);
        }

        List<String> kpIds = knowledgeGraphRepository.findAllKpIdsBySource(source);
        if (kpIds.isEmpty()) return Collections.emptyList();

        Map<String, Double> probMap = estimateByKp(userId, kpIds);

        List<RadarItemVO> result = new ArrayList<>();
        for (String kpId : kpIds) {
            String name = knowledgeGraphRepository.findDetailById(kpId)
                    .map(KnowledgePointDetailVO::getName)
                    .orElse(kpId);
            double p = probMap.getOrDefault(kpId, 0.0);
            result.add(new RadarItemVO(kpId, name, (int) Math.round(p * 100),
                    CognitiveDiagnosisService.mapToLevel(p)));
        }
        return result;
    }

    /**
     * 按知识领域（章节）聚合掌握度：领域掌握度 = 该领域内各知识点掌握概率的均值（未作答按 0 计），
     * 与整体掌握度口径一致，便于两个维度横向对照。
     */
    public List<RadarItemVO> getDomainRadar(Integer userId, String source) {
        List<String> kpIds = knowledgeGraphRepository.findAllKpIdsBySource(source);
        if (kpIds.isEmpty()) return Collections.emptyList();

        Map<String, Double> probMap = estimateByKp(userId, kpIds);

        // 知识点 -> 所属领域（章节）
        Map<String, String> domainOfKp = new HashMap<>();
        Map<String, String> domainName = new HashMap<>();
        Map<String, Integer> domainOrder = new HashMap<>();
        for (Map<String, Object> row : knowledgeGraphRepository.findKpDomainMapBySource(source)) {
            Object kpIdObj = row.get("kpId");
            if (kpIdObj == null) continue;
            String kpId = String.valueOf(kpIdObj);
            Object subTopicIdObj = row.get("subTopicId");
            String domainId = subTopicIdObj == null ? UNCLASSIFIED_DOMAIN_ID : String.valueOf(subTopicIdObj);
            Object subTopicNameObj = row.get("subTopicName");
            String domainLabel = subTopicNameObj == null ? UNCLASSIFIED_DOMAIN_NAME : String.valueOf(subTopicNameObj);
            domainOfKp.put(kpId, domainId);
            domainName.put(domainId, domainLabel);
            if (row.get("subTopicOrder") instanceof Number n) {
                domainOrder.put(domainId, n.intValue());
            }
        }

        // 逐领域累加掌握概率与知识点数量
        Map<String, double[]> accByDomain = new HashMap<>();
        for (String kpId : kpIds) {
            String domainId = domainOfKp.getOrDefault(kpId, UNCLASSIFIED_DOMAIN_ID);
            domainName.putIfAbsent(domainId, UNCLASSIFIED_DOMAIN_NAME);
            double[] acc = accByDomain.computeIfAbsent(domainId, k -> new double[2]);
            acc[0] += probMap.getOrDefault(kpId, 0.0);
            acc[1] += 1;
        }

        List<RadarItemVO> result = new ArrayList<>();
        for (Map.Entry<String, double[]> e : accByDomain.entrySet()) {
            // 未归属任何图谱章节（SubTopic）的知识点不属于章节内容，不参与领域维度展示
            if (UNCLASSIFIED_DOMAIN_ID.equals(e.getKey())) continue;
            double mean = e.getValue()[1] == 0 ? 0.0 : e.getValue()[0] / e.getValue()[1];
            result.add(new RadarItemVO(e.getKey(), domainName.getOrDefault(e.getKey(), UNCLASSIFIED_DOMAIN_NAME),
                    (int) Math.round(mean * 100), CognitiveDiagnosisService.mapToLevel(mean)));
        }
        // 按章节顺序排列，未挂章节的知识点统一放末尾
        result.sort(Comparator.comparingInt(item -> {
            if (UNCLASSIFIED_DOMAIN_ID.equals(item.getId())) return Integer.MAX_VALUE;
            return domainOrder.getOrDefault(item.getId(), Integer.MAX_VALUE - 1);
        }));
        return result;
    }

    /**
     * 掌握度时间趋势：以自然日为单位，给出每日答题量与"截至当日"的累计掌握度。
     * 累计掌握度 = 截至当日累计答对数 / 累计答题数，可直观反映学习推进过程中掌握度的变化。
     *
     * @param days 最多返回的天数（含今天），按最近 N 天截断；记录跨度不足 N 天时以最早记录日起算
     */
    public List<TrendPointVO> getTrend(Integer userId, String source, int days) {
        List<String> kpIds = knowledgeGraphRepository.findAllKpIdsBySource(source);
        if (kpIds.isEmpty()) return Collections.emptyList();

        List<UserAnswerRecord> records = userAnswerRecordMapper.selectList(
                new LambdaQueryWrapper<UserAnswerRecord>()
                        .eq(UserAnswerRecord::getUserId, userId)
                        .in(UserAnswerRecord::getKnowledgePointId, kpIds));
        if (records.isEmpty()) return Collections.emptyList();

        // 按自然日聚合答题量与答对量
        Map<LocalDate, int[]> daily = new TreeMap<>();
        for (UserAnswerRecord r : records) {
            if (r.getCreateTime() == null) continue;
            int[] cnt = daily.computeIfAbsent(r.getCreateTime().toLocalDate(), k -> new int[2]);
            cnt[0]++;
            if (Boolean.TRUE.equals(r.getIsCorrect())) cnt[1]++;
        }
        if (daily.isEmpty()) return Collections.emptyList();

        LocalDate today = LocalDate.now();
        int span = Math.max(days, 1);
        LocalDate windowStart = today.minusDays(span - 1L);
        LocalDate earliest = daily.keySet().iterator().next();
        LocalDate from = earliest.isAfter(windowStart) ? earliest : windowStart;
        if (from.isAfter(today)) from = today;

        // 窗口之前的作答计入累计基线，避免趋势从 0 重新起算
        int cumTotal = 0;
        int cumCorrect = 0;
        for (Map.Entry<LocalDate, int[]> e : daily.entrySet()) {
            if (!e.getKey().isBefore(from)) break;
            cumTotal += e.getValue()[0];
            cumCorrect += e.getValue()[1];
        }

        List<TrendPointVO> result = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(today) && result.size() < span; d = d.plusDays(1)) {
            int[] cnt = daily.getOrDefault(d, new int[2]);
            cumTotal += cnt[0];
            cumCorrect += cnt[1];
            int mastery = cumTotal == 0 ? 0 : (int) Math.round(cumCorrect * 100.0 / cumTotal);
            int accuracy = cnt[0] == 0 ? 0 : (int) Math.round(cnt[1] * 100.0 / cnt[0]);
            result.add(new TrendPointVO(d.toString(), mastery, cnt[0], cnt[1], accuracy));
        }
        return result;
    }

    /**
     * 取某用户在某课程全部知识点上的 DINA 掌握概率估计。
     */
    private Map<String, Double> estimateByKp(Integer userId, List<String> kpIds) {
        List<UserAnswerRecord> records = userAnswerRecordMapper.selectList(
                new LambdaQueryWrapper<UserAnswerRecord>()
                        .eq(UserAnswerRecord::getUserId, userId)
                        .in(UserAnswerRecord::getKnowledgePointId, kpIds));
        return cognitiveDiagnosisService.estimate(records);
    }

    /**
     * 薄弱点查询：认知诊断掌握度 < 60 或答题错误率 > 40%
     */
    public List<WeakPointVO> getWeakPoints(Integer userId, String source) {
        List<String> kpIds = knowledgeGraphRepository.findAllKpIdsBySource(source);
        if (kpIds.isEmpty()) return Collections.emptyList();

        List<UserAnswerRecord> records = userAnswerRecordMapper.selectList(
                new LambdaQueryWrapper<UserAnswerRecord>()
                        .eq(UserAnswerRecord::getUserId, userId)
                        .in(UserAnswerRecord::getKnowledgePointId, kpIds));
        Map<String, Double> probMap = cognitiveDiagnosisService.estimate(records);
        Map<String, Integer> totalMap = new HashMap<>();
        Map<String, Integer> errorMap = new HashMap<>();
        for (UserAnswerRecord r : records) {
            if (r.getKnowledgePointId() == null) continue;
            totalMap.merge(r.getKnowledgePointId(), 1, Integer::sum);
            if (!Boolean.TRUE.equals(r.getIsCorrect())) {
                errorMap.merge(r.getKnowledgePointId(), 1, Integer::sum);
            }
        }

        List<WeakPointVO> result = new ArrayList<>();
        for (Map.Entry<String, Double> e : probMap.entrySet()) {
            String kpId = e.getKey();
            int mastery = (int) Math.round(e.getValue() * 100);
            if (mastery >= 60) continue;

            String name = knowledgeGraphRepository.findDetailById(kpId)
                    .map(KnowledgePointDetailVO::getName)
                    .orElse(kpId);
            int total = totalMap.getOrDefault(kpId, 0);
            int errors = errorMap.getOrDefault(kpId, 0);
            result.add(new WeakPointVO(kpId, name, mastery, total, errors));
        }
        return result;
    }
}
