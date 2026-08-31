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

/**
 * 高级学习分析算法集合。
 * 1. calculateDecayedMastery    : 时间衰减掌握度（越早的答题记录权重越低）
 * 2. traceWeaknessByGraph       : 基于知识图谱的缺陷溯源（BFS 从弱节点向上追溯先修节点）
 * 3. recommendLearningPath      : 学习路径推荐排序（综合掌握度 + 先修依赖 + 难度权重）
 */
@Service
@RequiredArgsConstructor
public class AnalysisAlgorithmService {

    /** 判定"已掌握"的阈值（与 DiagnosisService 保持一致） */
    private static final int MASTER_THRESHOLD = 60;

    /** 时间衰减系数：每天衰减 exp(-0.035)，约 20 天前记录的权重降为一半 */
    private static final double DECAY_RATE = 0.035;

    private final UserAnswerRecordMapper userAnswerRecordMapper;
    private final KnowledgeMasteryMapper knowledgeMasteryMapper;
    private final KnowledgeGraphRepository knowledgeGraphRepository;
    private final CourseService courseService;

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
     * 学习路径推荐排序（综合掌握度 + 先修依赖 + 难度权重）：
     * 在拓扑排序得到的候选路径上，对未掌握节点打分并按分推荐：
     * score = 先修满足度(0~40) + 难度权重基于 group(0~30) + 掌握度接近阈值优先(0~30)。
     * group 由图谱层级决定，group 越小越基础、越优先学习。
     */
    public List<RecommendationVO> recommendLearningPath(Long userId, Long courseId) {
        Course course = courseService.getCourseById(courseId.intValue());
        String source = course.getSource();

        List<LearningPathItemVO> path = knowledgeGraphRepository.findLearningPathBySource(source);
        if (path.isEmpty()) {
            return Collections.emptyList();
        }

        // 已掌握集合 + 掌握度查找表
        List<KnowledgeMastery> masteries = knowledgeMasteryMapper.selectList(
                new LambdaQueryWrapper<KnowledgeMastery>()
                        .eq(KnowledgeMastery::getUserId, userId)
                        .eq(KnowledgeMastery::getCourseId, courseId.intValue()));
        Set<String> mastered = new HashSet<>();
        Map<String, Integer> masteryMap = new HashMap<>();
        for (KnowledgeMastery km : masteries) {
            masteryMap.put(km.getKnowledgePointId(), km.getMasteryLevel());
            if (km.getMasteryLevel() != null && km.getMasteryLevel() >= MASTER_THRESHOLD) {
                mastered.add(km.getKnowledgePointId());
            }
        }

        // 候选节点打分
        List<ScoredItem> scored = new ArrayList<>();
        for (LearningPathItemVO item : path) {
            if (mastered.contains(item.getId())) continue;

            List<String> prereqs = item.getPrerequisites() == null
                    ? Collections.emptyList() : item.getPrerequisites();
            int totalPrereq = prereqs.size();
            int metPrereq = 0;
            for (String p : prereqs) {
                if (mastered.contains(p)) metPrereq++;
            }

            int mastery = masteryMap.getOrDefault(item.getId(), 0);

            double score = 0;
            score += totalPrereq == 0 ? 40.0 : 40.0 * metPrereq / (double) totalPrereq;
            score += Math.min(30.0, Math.max(0.0, 30.0 - item.getGroup() * 2.0));
            score += Math.max(0.0, 30.0 - Math.abs(mastery - MASTER_THRESHOLD) * 0.5);

            String reason = buildReason(prereqs, metPrereq, totalPrereq, mastery);
            scored.add(new ScoredItem(item, score, reason));
        }

        scored.sort((a, b) -> Double.compare(b.score, a.score));

        List<RecommendationVO> result = new ArrayList<>();
        int limit = Math.min(5, scored.size());
        for (int i = 0; i < limit; i++) {
            ScoredItem s = scored.get(i);
            result.add(new RecommendationVO(
                    s.item.getId(), s.item.getName(), s.item.getDescription(), s.reason,
                    resolvePrereqNames(s.item.getPrerequisites()), masteryMap.getOrDefault(s.item.getId(), 0)));
        }
        return result;
    }

    private String buildReason(List<String> prereqs, int met, int total, int mastery) {
        if (prereqs.isEmpty()) {
            return "学习路径起点，无前置依赖，可直接开始";
        }
        if (met == total) {
            return String.format("前置知识点已掌握(%d/%d)，可以开始学习", met, total);
        }
        return String.format("尚有 %d 个前置知识点未掌握，需先补齐", total - met);
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
        final LearningPathItemVO item;
        final double score;
        final String reason;

        ScoredItem(LearningPathItemVO item, double score, String reason) {
            this.item = item;
            this.score = score;
            this.reason = reason;
        }
    }
}
