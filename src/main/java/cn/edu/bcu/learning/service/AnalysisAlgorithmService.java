package cn.edu.bcu.learning.service;

import cn.edu.bcu.learning.domain.entity.Course;
import cn.edu.bcu.learning.domain.entity.KnowledgeMastery;
import cn.edu.bcu.learning.domain.entity.UserAnswerRecord;
import cn.edu.bcu.learning.domain.vo.KnowledgePointDetailVO;
import cn.edu.bcu.learning.domain.vo.LearningPathItemVO;
import cn.edu.bcu.learning.domain.vo.RecommendationVO;
import cn.edu.bcu.learning.domain.vo.WeaknessTraceVO;
import cn.edu.bcu.learning.repository.mysql.KnowledgeMasteryMapper;
import cn.edu.bcu.learning.repository.mysql.UserAnswerRecordMapper;
import cn.edu.bcu.learning.repository.neo4j.KnowledgeGraphRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 高级学习分析算法集合。
 * 1. calculateDecayedMastery    : 时间衰减掌握度（越早的答题记录权重越低）
 * 2. traceWeaknessByGraph       : 基于知识图谱的缺陷溯源（BFS 从弱节点向上追溯先修节点）
 * 3. recommendLearningPath      : 学习路径推荐（PC-TOP：先修闭包约束的确定性拓扑排序）
 */
@Service
@RequiredArgsConstructor
public class AnalysisAlgorithmService {

    /** 判定"已掌握"的阈值（与 DiagnosisService 保持一致） */
    private static final int MASTER_THRESHOLD = 60;

    /** PC-TOP 掌握态判定阈值：掌握概率 θ ≥ 0.60 视为已掌握 */
    private static final double MASTER_PROB = MASTER_THRESHOLD / 100.0;

    /** 时间衰减系数：每天衰减 exp(-0.035)，约 20 天前记录的权重降为一半 */
    private static final double DECAY_RATE = 0.035;

    private final UserAnswerRecordMapper userAnswerRecordMapper;
    private final KnowledgeMasteryMapper knowledgeMasteryMapper;
    private final KnowledgeGraphRepository knowledgeGraphRepository;
    private final CourseService courseService;
    private final CognitiveDiagnosisService cognitiveDiagnosisService;

    /**
     * 知识点掌握度时间衰减算法：
     * 对答题记录按时间指数加权，越早的回答权重越低，再计算加权正确率。
     * mastery = Σ(w_i * correct_i) / Σ(w_i) * 100，其中 w_i = e^(-λ * 距今天数)
     */
    public double calculateDecayedMastery(Long userId, Long knowledgePointId) {
        List<UserAnswerRecord> records = userAnswerRecordMapper.selectList(
                new LambdaQueryWrapper<UserAnswerRecord>()
                        .eq(UserAnswerRecord::getUserId, userId)
                        .eq(UserAnswerRecord::getKnowledgePointId, knowledgePointId)
                        .orderByAsc(UserAnswerRecord::getCreateTime));
        if (records.isEmpty()) {
            return 0.0;
        }

        LocalDate today = LocalDate.now();
        double weightSum = 0.0;
        double correctWeightSum = 0.0;
        for (UserAnswerRecord r : records) {
            LocalDateTime time = r.getCreateTime();
            double days = time == null ? 0.0
                    : Math.max(0, ChronoUnit.DAYS.between(time.toLocalDate(), today));
            double w = Math.exp(-DECAY_RATE * days);
            weightSum += w;
            if (Boolean.TRUE.equals(r.getIsCorrect())) {
                correctWeightSum += w;
            }
        }
        return weightSum == 0 ? 0.0 : correctWeightSum / weightSum * 100.0;
    }

    /**
     * 基于知识图谱的缺陷溯源：
     * 先找出掌握度 < 60 的薄弱知识点，再对每个薄弱点从 Neo4j 沿 PREREQUISITE 边
     * 向上 BFS 追溯所有仍未掌握的祖先先修节点，形成"缺陷链"。
     */
    public List<WeaknessTraceVO> traceWeaknessByGraph(Long userId, Long courseId) {
        Course course = courseService.getCourseById(courseId.intValue());
        String source = course.getSource();

        // 该用户在该课程下的全部掌握度记录
        Map<String, Integer> masteryMap = new HashMap<>();
        List<KnowledgeMastery> masteries = knowledgeMasteryMapper.selectList(
                new LambdaQueryWrapper<KnowledgeMastery>()
                        .eq(KnowledgeMastery::getUserId, userId)
                        .eq(KnowledgeMastery::getCourseId, courseId.intValue()));
        for (KnowledgeMastery km : masteries) {
            masteryMap.put(km.getKnowledgePointId(), km.getMasteryLevel());
        }

        // 找出薄弱知识点（有记录且掌握度 < 60）
        List<String> weakKpIds = new ArrayList<>();
        for (KnowledgeMastery km : masteries) {
            if (km.getMasteryLevel() == null || km.getMasteryLevel() < MASTER_THRESHOLD) {
                weakKpIds.add(km.getKnowledgePointId());
            }
        }

        List<WeaknessTraceVO> result = new ArrayList<>();
        for (String weakKpId : weakKpIds) {
            // BFS：从薄弱节点向上追溯未掌握的先修链
            Set<String> visited = new HashSet<>();
            List<String> chain = new ArrayList<>();
            Deque<String> queue = new ArrayDeque<>();
            queue.add(weakKpId);

            while (!queue.isEmpty()) {
                String cur = queue.poll();
                if (!visited.add(cur)) continue;
                chain.add(cur);

                for (KnowledgePointDetailVO prereq : knowledgeGraphRepository.findPrerequisites(cur)) {
                    int m = masteryMap.getOrDefault(prereq.getId(), 0);
                    if (m < MASTER_THRESHOLD) {
                        queue.add(prereq.getId());
                    }
                }
            }

            String name = knowledgeGraphRepository.findDetailById(weakKpId)
                    .map(KnowledgePointDetailVO::getName)
                    .orElse(weakKpId);
            int mastery = masteryMap.getOrDefault(weakKpId, 0);
            result.add(new WeaknessTraceVO(weakKpId, name, mastery, chain));
        }
        return result;
    }

    /**
     * PC-TOP 学习路径推荐（Prerequisite-Closure Topological Ranking）。
     *
     * 确定性、可解释、无需训练的四阶段流水线：
     *   阶段 1 掌握态判定：θ_k 取 DINA 认知诊断掌握概率，并与 knowledge_mastery 中
     *          非答题来源的既有证据（标记完成=100、视频学习=30）取较大值，避免信息丢失；
     *   阶段 2 反向闭包：沿 PREREQUISITE 边求祖先闭包 Anc(k)，Anc(k) 全部掌握则 k 就绪，
     *          就绪且未掌握的节点构成"学习前沿 Frontier"，其余记为缺口数 Gap(k)；
     *   阶段 3 拓扑深度：先修链最长深度 depth(k)，保证基础知识点优先；
     *   阶段 4 加权排序：0.4 掌握缺口 + 0.3 后继覆盖度 + 0.2 时序衰减 + 0.1 基础性。
     *
     * 图结构一次性批量装载（节点与 PREREQUISITE 边各一次查询），
     * 闭包与覆盖度在内存中带记忆化求解，1608 节点规模可实时返回。
     */
    public List<RecommendationVO> recommendLearningPath(Long userId, Long courseId) {
        Course course = courseService.getCourseById(courseId.intValue());
        String source = course.getSource();

        // 阶段 0：一次性装载节点与先修边，避免逐点查询的 N+1 往返
        List<LearningPathItemVO> nodes = knowledgeGraphRepository.findAllKpNodesBySource(source);
        if (nodes.isEmpty()) {
            return Collections.emptyList();
        }
        List<String[]> edges = knowledgeGraphRepository.findAllPrereqEdges(source);

        Map<String, LearningPathItemVO> nodeMap = new LinkedHashMap<>();
        Map<String, List<String>> children = new HashMap<>();
        Map<String, List<String>> parents = new HashMap<>();
        for (LearningPathItemVO node : nodes) {
            nodeMap.put(node.getId(), node);
            children.put(node.getId(), new ArrayList<>());
            parents.put(node.getId(), new ArrayList<>());
        }
        for (String[] edge : edges) {
            if (!nodeMap.containsKey(edge[0]) || !nodeMap.containsKey(edge[1])) continue;
            children.get(edge[0]).add(edge[1]);
            parents.get(edge[1]).add(edge[0]);
        }

        // 阶段 1：掌握态判定 —— DINA 认知诊断概率与 knowledge_mastery 既有证据取较大值
        List<UserAnswerRecord> records = userAnswerRecordMapper.selectList(
                new LambdaQueryWrapper<UserAnswerRecord>()
                        .eq(UserAnswerRecord::getUserId, userId));
        Map<String, Double> probMap = cognitiveDiagnosisService.estimate(records);

        List<KnowledgeMastery> masteries = knowledgeMasteryMapper.selectList(
                new LambdaQueryWrapper<KnowledgeMastery>()
                        .eq(KnowledgeMastery::getUserId, userId)
                        .eq(KnowledgeMastery::getCourseId, courseId.intValue()));
        Map<String, Integer> masteryMap = new HashMap<>();
        for (KnowledgeMastery km : masteries) {
            if (km.getKnowledgePointId() == null) continue;
            masteryMap.put(km.getKnowledgePointId(),
                    km.getMasteryLevel() == null ? 0 : km.getMasteryLevel());
        }

        Map<String, LocalDateTime> lastTimeMap = new HashMap<>();
        for (UserAnswerRecord r : records) {
            if (r.getKnowledgePointId() == null || r.getCreateTime() == null) continue;
            lastTimeMap.merge(r.getKnowledgePointId(), r.getCreateTime(),
                    (a, b) -> b.isAfter(a) ? b : a);
        }

        Map<String, Double> theta = new HashMap<>();
        for (String kpId : nodeMap.keySet()) {
            double prob = probMap.getOrDefault(kpId, 0.0);
            double evidence = masteryMap.getOrDefault(kpId, 0) / 100.0;
            theta.put(kpId, Math.max(prob, evidence));
        }

        // 阶段 2：反向闭包求学习前沿 Frontier
        Map<String, Set<String>> ancestorCache = new HashMap<>();
        List<String> frontier = new ArrayList<>();
        Map<String, Integer> gapMap = new HashMap<>();
        for (String kpId : nodeMap.keySet()) {
            if (theta.getOrDefault(kpId, 0.0) >= MASTER_PROB) continue;
            Set<String> ancestors = ancestorsOf(kpId, parents, ancestorCache);
            int gaps = 0;
            for (String ancestor : ancestors) {
                // 环回边会让闭包包含自身，统计缺口时需排除，避免节点永远无法进入前沿
                if (ancestor.equals(kpId)) continue;
                if (theta.getOrDefault(ancestor, 0.0) < MASTER_PROB) gaps++;
            }
            if (gaps == 0) {
                frontier.add(kpId);
            } else {
                gapMap.put(kpId, gaps);
            }
        }

        // 兜底：前沿为空（候选全被前置阻塞）时退化为"缺口最少"的节点，避免推荐列表空转
        List<String> candidates;
        if (!frontier.isEmpty()) {
            candidates = frontier;
        } else {
            candidates = gapMap.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .limit(5)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        }
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }
        // 阶段 3：拓扑深度（先修链最长长度），maxDepth 用于归一化基础性因子
        Map<String, Integer> depthCache = new HashMap<>();
        int maxDepth = 1;
        for (String kpId : nodeMap.keySet()) {
            maxDepth = Math.max(maxDepth, depthOf(kpId, parents, depthCache));
        }

        // 阶段 4：加权优先级排序
        Map<String, Integer> descCache = new HashMap<>();
        double logBase = Math.log(1 + nodeMap.size());
        List<ScoredItem> scored = new ArrayList<>();
        for (String kpId : candidates) {
            LearningPathItemVO node = nodeMap.get(kpId);
            double prob = theta.getOrDefault(kpId, 0.0);
            int unlock = descendantsOf(kpId, children, descCache);
            LocalDateTime last = lastTimeMap.get(kpId);
            // 从未练习视为最需要接触，衰减因子取满值
            double decay = last == null ? 1.0
                    : Math.exp(-DECAY_RATE * Math.max(0,
                            ChronoUnit.DAYS.between(last.toLocalDate(), LocalDate.now())));
            double coverage = logBase <= 0 ? 0.0 : Math.log(1 + unlock) / logBase;
            double foundation = 1.0 - (double) depthOf(kpId, parents, depthCache) / maxDepth;

            double score = 0.40 * (1 - prob)
                    + 0.30 * coverage
                    + 0.20 * decay
                    + 0.10 * foundation;

            RecommendationVO vo = new RecommendationVO(
                    node.getId(), node.getName(), node.getDescription(),
                    buildReason(prob, unlock, last, gapMap.get(kpId)),
                    resolvePrereqNames(parents.getOrDefault(kpId, Collections.emptyList())),
                    (int) Math.round(prob * 100));
            vo.setUnlockCount(unlock);
            vo.setDaysSinceLastStudy(last == null ? -1
                    : (int) ChronoUnit.DAYS.between(last.toLocalDate(), LocalDate.now()));
            vo.setLevel(CognitiveDiagnosisService.mapToLevel(prob));
            scored.add(new ScoredItem(vo, score));
        }
        scored.sort((a, b) -> Double.compare(b.score, a.score));

        List<RecommendationVO> result = new ArrayList<>();
        int limit = Math.min(5, scored.size());
        for (int i = 0; i < limit; i++) {
            result.add(scored.get(i).vo);
        }
        return result;
    }

    /**
     * 知识点粒度的 PC-TOP 推荐：算法内核与 {@link #recommendLearningPath} 完全一致
     * （同样的掌握态判定、反向闭包前沿、拓扑深度、0.4/0.3/0.2/0.1 四因子打分与 Top5 截断），
     * 唯一区别是候选域由「整门课全部知识点」收窄为「目标知识点沿 PREREQUISITE 边反向 BFS
     * 得到的先修闭包」，闭包之外的节点一律不进入候选，因此点击哪个知识点就只推荐它的真实前置。
     */
    public List<RecommendationVO> recommendPrerequisites(Integer userId, Integer courseId, String kpId) {
        if (kpId == null || kpId.isBlank()) {
            return Collections.emptyList();
        }
        Course course = courseService.getCourseById(courseId);
        if (course == null) {
            return Collections.emptyList();
        }
        String source = course.getSource();

        // 阶段 0：一次性装载节点与先修边，避免逐点查询的 N+1 往返
        List<LearningPathItemVO> nodes = knowledgeGraphRepository.findAllKpNodesBySource(source);
        if (nodes.isEmpty()) {
            return Collections.emptyList();
        }
        List<String[]> edges = knowledgeGraphRepository.findAllPrereqEdges(source);

        Map<String, LearningPathItemVO> nodeMap = new LinkedHashMap<>();
        Map<String, List<String>> children = new HashMap<>();
        Map<String, List<String>> parents = new HashMap<>();
        for (LearningPathItemVO node : nodes) {
            nodeMap.put(node.getId(), node);
            children.put(node.getId(), new ArrayList<>());
            parents.put(node.getId(), new ArrayList<>());
        }
        if (!nodeMap.containsKey(kpId)) {
            return Collections.emptyList();
        }
        for (String[] edge : edges) {
            if (!nodeMap.containsKey(edge[0]) || !nodeMap.containsKey(edge[1])) continue;
            children.get(edge[0]).add(edge[1]);
            parents.get(edge[1]).add(edge[0]);
        }

        // 候选域：目标知识点的先修闭包（反向 BFS 记录最短跳数），无前置即返回空
        Map<String, Integer> hops = new LinkedHashMap<>();
        Deque<String> queue = new ArrayDeque<>();
        for (String parent : parents.getOrDefault(kpId, Collections.emptyList())) {
            if (parent.equals(kpId) || hops.containsKey(parent)) continue;
            hops.put(parent, 1);
            queue.add(parent);
        }
        while (!queue.isEmpty()) {
            String current = queue.poll();
            int currentHop = hops.getOrDefault(current, 1);
            for (String parent : parents.getOrDefault(current, Collections.emptyList())) {
                if (parent.equals(kpId) || hops.containsKey(parent)) continue;
                hops.put(parent, currentHop + 1);
                queue.add(parent);
            }
        }
        if (hops.isEmpty()) {
            return Collections.emptyList();
        }

        // 阶段 1：掌握态判定 —— DINA 认知诊断概率与 knowledge_mastery 既有证据取较大值
        List<UserAnswerRecord> records = userAnswerRecordMapper.selectList(
                new LambdaQueryWrapper<UserAnswerRecord>()
                        .eq(UserAnswerRecord::getUserId, userId));
        Map<String, Double> probMap = cognitiveDiagnosisService.estimate(records);

        List<KnowledgeMastery> masteries = knowledgeMasteryMapper.selectList(
                new LambdaQueryWrapper<KnowledgeMastery>()
                        .eq(KnowledgeMastery::getUserId, userId)
                        .eq(KnowledgeMastery::getCourseId, courseId));
        Map<String, Integer> masteryMap = new HashMap<>();
        for (KnowledgeMastery km : masteries) {
            if (km.getKnowledgePointId() == null) continue;
            masteryMap.put(km.getKnowledgePointId(),
                    km.getMasteryLevel() == null ? 0 : km.getMasteryLevel());
        }

        Map<String, LocalDateTime> lastTimeMap = new HashMap<>();
        for (UserAnswerRecord r : records) {
            if (r.getKnowledgePointId() == null || r.getCreateTime() == null) continue;
            lastTimeMap.merge(r.getKnowledgePointId(), r.getCreateTime(),
                    (a, b) -> b.isAfter(a) ? b : a);
        }

        Map<String, Double> theta = new HashMap<>();
        for (String id : hops.keySet()) {
            double prob = probMap.getOrDefault(id, 0.0);
            double evidence = masteryMap.getOrDefault(id, 0) / 100.0;
            theta.put(id, Math.max(prob, evidence));
        }

        // 阶段 2：在闭包内求学习前沿 —— 本身未掌握且其全部祖先已掌握
        Map<String, Set<String>> ancestorCache = new HashMap<>();
        List<String> frontier = new ArrayList<>();
        Map<String, Integer> gapMap = new HashMap<>();
        for (String id : hops.keySet()) {
            if (theta.getOrDefault(id, 0.0) >= MASTER_PROB) continue;
            Set<String> ancestors = ancestorsOf(id, parents, ancestorCache);
            int gaps = 0;
            for (String ancestor : ancestors) {
                if (ancestor.equals(id)) continue;
                if (theta.getOrDefault(ancestor, 0.0) < MASTER_PROB) gaps++;
            }
            if (gaps == 0) {
                frontier.add(id);
            } else {
                gapMap.put(id, gaps);
            }
        }

        // 兜底：闭包内前沿为空（前置被更上层前置阻塞）时退化为"缺口最少"的节点
        List<String> candidates;
        if (!frontier.isEmpty()) {
            candidates = frontier;
        } else {
            candidates = gapMap.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .limit(5)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        }
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        // 阶段 3：拓扑深度（先修链最长长度），maxDepth 用于归一化基础性因子
        Map<String, Integer> depthCache = new HashMap<>();
        int maxDepth = 1;
        for (String id : nodeMap.keySet()) {
            maxDepth = Math.max(maxDepth, depthOf(id, parents, depthCache));
        }

        // 阶段 4：加权优先级排序（0.4 掌握缺口 + 0.3 后继覆盖度 + 0.2 时序衰减 + 0.1 基础性）
        String targetName = nodeMap.get(kpId).getName();
        Map<String, Integer> descCache = new HashMap<>();
        double logBase = Math.log(1 + nodeMap.size());
        List<ScoredItem> scored = new ArrayList<>();
        for (String id : candidates) {
            LearningPathItemVO node = nodeMap.get(id);
            double prob = theta.getOrDefault(id, 0.0);
            int unlock = descendantsOf(id, children, descCache);
            LocalDateTime last = lastTimeMap.get(id);
            double decay = last == null ? 1.0
                    : Math.exp(-DECAY_RATE * Math.max(0,
                            ChronoUnit.DAYS.between(last.toLocalDate(), LocalDate.now())));
            double coverage = logBase <= 0 ? 0.0 : Math.log(1 + unlock) / logBase;
            double foundation = 1.0 - (double) depthOf(id, parents, depthCache) / maxDepth;

            double score = 0.40 * (1 - prob)
                    + 0.30 * coverage
                    + 0.20 * decay
                    + 0.10 * foundation;

            String prefix = "「" + targetName + "」的第 " + hops.getOrDefault(id, 1) + " 层前置；";
            RecommendationVO vo = new RecommendationVO(
                    node.getId(), node.getName(), node.getDescription(),
                    prefix + buildReason(prob, unlock, last, gapMap.get(id)),
                    resolvePrereqNames(parents.getOrDefault(id, Collections.emptyList())),
                    (int) Math.round(prob * 100));
            vo.setUnlockCount(unlock);
            vo.setDaysSinceLastStudy(last == null ? -1
                    : (int) ChronoUnit.DAYS.between(last.toLocalDate(), LocalDate.now()));
            vo.setLevel(CognitiveDiagnosisService.mapToLevel(prob));
            scored.add(new ScoredItem(vo, score));
        }
        scored.sort((a, b) -> Double.compare(b.score, a.score));

        List<RecommendationVO> result = new ArrayList<>();
        int limit = Math.min(5, scored.size());
        for (int i = 0; i < limit; i++) {
            result.add(scored.get(i).vo);
        }
        return result;
    }

    /** 反向闭包：沿 PREREQUISITE 边向上取全部祖先知识点（记忆化 + 环保护） */
    private Set<String> ancestorsOf(String kpId, Map<String, List<String>> parents,
                                    Map<String, Set<String>> cache) {
        Set<String> cached = cache.get(kpId);
        if (cached != null) return cached;
        Set<String> result = new LinkedHashSet<>();
        // 先放入缓存，遇到环时直接命中当前集合，避免无限递归
        cache.put(kpId, result);
        for (String parent : parents.getOrDefault(kpId, Collections.emptyList())) {
            if (result.add(parent)) {
                result.addAll(ancestorsOf(parent, parents, cache));
            }
        }
        return result;
    }

    /** 后代闭包大小：学完该知识点可解锁的后续知识点数量 */
    private int descendantsOf(String kpId, Map<String, List<String>> children, Map<String, Integer> cache) {
        Integer cached = cache.get(kpId);
        if (cached != null) return cached;
        Set<String> visited = new LinkedHashSet<>();
        Deque<String> queue = new ArrayDeque<>(children.getOrDefault(kpId, Collections.emptyList()));
        while (!queue.isEmpty()) {
            String cur = queue.poll();
            if (!visited.add(cur)) continue;
            queue.addAll(children.getOrDefault(cur, Collections.emptyList()));
        }
        cache.put(kpId, visited.size());
        return visited.size();
    }

    /** 拓扑深度：先修链最长长度，无先修记为 0（记忆化 + 环保护） */
    private int depthOf(String kpId, Map<String, List<String>> parents, Map<String, Integer> cache) {
        Integer cached = cache.get(kpId);
        if (cached != null) return cached;
        cache.put(kpId, 0);
        int best = 0;
        for (String parent : parents.getOrDefault(kpId, Collections.emptyList())) {
            best = Math.max(best, depthOf(parent, parents, cache) + 1);
        }
        cache.put(kpId, best);
        return best;
    }

    /** 推荐理由：掌握缺口 + 前置缺口 + 覆盖度 + 时序衰减，保证推荐结果可解释 */
    private String buildReason(double theta, int unlock, LocalDateTime last, Integer gaps) {
        StringBuilder sb = new StringBuilder();
        if (theta < 0.40) {
            sb.append(String.format("掌握概率仅 %.0f%%", theta * 100));
        } else if (theta < MASTER_PROB) {
            sb.append(String.format("掌握概率 %.0f%%，已接近达标线", theta * 100));
        } else {
            sb.append(String.format("掌握概率 %.0f%%", theta * 100));
        }
        if (gaps != null && gaps > 0) {
            sb.append(String.format("；仍有 %d 个前置知识点未掌握", gaps));
        }
        sb.append(String.format("；学完可解锁 %d 个后续知识点", unlock));
        sb.append(last == null ? "；尚未练习"
                : String.format("；已 %d 天未练习",
                        ChronoUnit.DAYS.between(last.toLocalDate(), LocalDate.now())));
        return sb.toString();
    }

    private List<String> resolvePrereqNames(List<String> prereqIds) {
        List<String> names = new ArrayList<>();
        for (String id : prereqIds) {
            names.add(knowledgeGraphRepository.findDetailById(id)
                    .map(KnowledgePointDetailVO::getName)
                    .orElse(id));
        }
        return names;
    }

    /** 用于推荐排序的内部结构 */
    private static class ScoredItem {
        final RecommendationVO vo;
        final double score;

        ScoredItem(RecommendationVO vo, double score) {
            this.vo = vo;
            this.score = score;
        }
    }
}
