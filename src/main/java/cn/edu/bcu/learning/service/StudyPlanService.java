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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyPlanService {

    private final KnowledgeGraphRepository knowledgeGraphRepository;
    private final KnowledgeMasteryMapper knowledgeMasteryMapper;
    private final UserAnswerRecordMapper userAnswerRecordMapper;
    private final CourseService courseService;

    /**
     * 个性化推荐：拓扑排序中，前置已满足（掌握度 >= 60）的下一节点。
     */
    public List<RecommendationVO> getRecommendations(Integer userId, Integer courseId) {
        Course course = courseService.getCourseById(courseId);
        String source = course.getSource();

        List<LearningPathItemVO> path = knowledgeGraphRepository.findLearningPathBySource(source);
        if (path.isEmpty()) return Collections.emptyList();

        // 获取已掌握的知识点
        List<KnowledgeMastery> masteries = knowledgeMasteryMapper.selectList(
                new LambdaQueryWrapper<KnowledgeMastery>()
                        .eq(KnowledgeMastery::getUserId, userId)
                        .eq(KnowledgeMastery::getCourseId, courseId));
        Set<String> mastered = masteries.stream()
                .filter(m -> m.getMasteryLevel() != null && m.getMasteryLevel() >= 60)
                .map(KnowledgeMastery::getKnowledgePointId)
                .collect(Collectors.toSet());

        List<RecommendationVO> result = new ArrayList<>();
        for (LearningPathItemVO item : path) {
            if (mastered.contains(item.getId())) continue;

            // 检查前置是否全部满足
            boolean prereqsMet = item.getPrerequisites() == null || item.getPrerequisites().isEmpty()
                    || mastered.containsAll(item.getPrerequisites());

            if (prereqsMet) {
                String reason = mastered.isEmpty()
                        ? "学习路径起点"
                        : "前置知识点已掌握，可以开始学习";
                result.add(new RecommendationVO(item.getId(), item.getName(), item.getDescription(), reason));
                if (result.size() >= 5) break; // 最多推荐5个
            }
        }
        return result;
    }

    /**
     * 学习路径规划：拓扑排序结果，按建议学习天数分组（每天 3 个知识点）。
     * 返回 Map: day -> [LearningPathItemVO, ...]
     */
    public Map<Integer, List<LearningPathItemVO>> getStudyPlan(Integer userId, Integer courseId) {
        Course course = courseService.getCourseById(courseId);
        String source = course.getSource();

        List<LearningPathItemVO> path = knowledgeGraphRepository.findLearningPathBySource(source);

        // 获取掌握度，过滤已掌握的知识点
        List<KnowledgeMastery> masteries = knowledgeMasteryMapper.selectList(
                new LambdaQueryWrapper<KnowledgeMastery>()
                        .eq(KnowledgeMastery::getUserId, userId)
                        .eq(KnowledgeMastery::getCourseId, courseId));
        Set<String> mastered = masteries.stream()
                .filter(m -> m.getMasteryLevel() != null && m.getMasteryLevel() >= 60)
                .map(KnowledgeMastery::getKnowledgePointId)
                .collect(Collectors.toSet());

        List<LearningPathItemVO> remaining = path.stream()
                .filter(item -> !mastered.contains(item.getId()))
                .collect(Collectors.toList());

        int perDay = 3;
        Map<Integer, List<LearningPathItemVO>> plan = new LinkedHashMap<>();
        for (int i = 0; i < remaining.size(); i++) {
            int day = i / perDay + 1;
            plan.computeIfAbsent(day, k -> new ArrayList<>()).add(remaining.get(i));
        }
        return plan;
    }

    /**
     * 复习提醒：根据错题记录，返回需要复习的知识点。
     */
    public List<ReminderVO> getReminders(Integer userId, Integer courseId) {
        Course course = courseService.getCourseById(courseId);
        String source = course.getSource();

        List<String> allKpIds = knowledgeGraphRepository.findAllKpIdsBySource(source);
        if (allKpIds.isEmpty()) return Collections.emptyList();

        List<UserAnswerRecord> wrongRecords = userAnswerRecordMapper.selectList(
                new LambdaQueryWrapper<UserAnswerRecord>()
                        .eq(UserAnswerRecord::getUserId, userId)
                        .eq(UserAnswerRecord::getIsCorrect, false)
                        .in(UserAnswerRecord::getKnowledgePointId, allKpIds)
                        .orderByDesc(UserAnswerRecord::getCreateTime));

        // 按知识点分组，统计错误次数和最近一次答错时间
        Map<String, Integer> errorCountMap = new HashMap<>();
        Map<String, LocalDateTime> latestErrorMap = new HashMap<>();
        for (UserAnswerRecord r : wrongRecords) {
            errorCountMap.merge(r.getKnowledgePointId(), 1, Integer::sum);
            LocalDateTime time = r.getCreateTime();
            if (time != null) {
                latestErrorMap.merge(r.getKnowledgePointId(), time,
                        (old, t) -> t.isAfter(old) ? t : old);
            }
        }

        List<ReminderVO> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : errorCountMap.entrySet()) {
            String kpId = entry.getKey();
            String name = knowledgeGraphRepository.findDetailById(kpId)
                    .map(KnowledgePointDetailVO::getName)
                    .orElse(kpId);
            int errorCount = entry.getValue();
            LocalDateTime last = latestErrorMap.get(kpId);
            int daysAgo = last == null ? 999 : (int) ChronoUnit.DAYS.between(last, LocalDateTime.now());
            result.add(new ReminderVO(kpId, name, errorCount, daysAgo));
        }
        // 按错误次数降序
        result.sort((a, b) -> Integer.compare(b.getErrorCount(), a.getErrorCount()));
        return result;
    }
}
