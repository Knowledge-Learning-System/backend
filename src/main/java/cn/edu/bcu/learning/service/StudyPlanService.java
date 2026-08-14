package cn.edu.bcu.learning.service;

import cn.edu.bcu.learning.domain.dto.CreateStudyPlanRequest;
import cn.edu.bcu.learning.domain.dto.UpdateStudyPlanRequest;
import cn.edu.bcu.learning.domain.entity.Course;
import cn.edu.bcu.learning.domain.entity.KnowledgeMastery;
import cn.edu.bcu.learning.domain.entity.StudyPlan;
import cn.edu.bcu.learning.domain.entity.UserAnswerRecord;
import cn.edu.bcu.learning.domain.vo.*;
import cn.edu.bcu.learning.repository.mysql.KnowledgeMasteryMapper;
import cn.edu.bcu.learning.repository.mysql.StudyPlanMapper;
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
    private final StudyPlanMapper studyPlanMapper;
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

        // 构建掌握度查找表（包含所有知识点，未掌握的为0）
        Map<String, Integer> masteryMap = masteries.stream()
                .collect(Collectors.toMap(KnowledgeMastery::getKnowledgePointId, KnowledgeMastery::getMasteryLevel, (a, b) -> a));

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
                int mastery = masteryMap.getOrDefault(item.getId(), 0);
                List<String> prereqNames = item.getPrerequisites() == null ? Collections.emptyList() :
                        item.getPrerequisites().stream()
                                .map(prereqId -> knowledgeGraphRepository.findDetailById(prereqId)
                                        .map(KnowledgePointDetailVO::getName)
                                        .orElse(prereqId))
                                .collect(Collectors.toList());
                result.add(new RecommendationVO(
                        item.getId(), item.getName(), item.getDescription(), reason,
                        prereqNames, mastery));
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

    // ========== 学习计划 CRUD ==========

    public StudyPlanVO createPlan(Integer userId, CreateStudyPlanRequest request) {
        if (request.getEndDate() != null && request.getStartDate() != null
                && !request.getEndDate().isAfter(request.getStartDate())) {
            throw new RuntimeException("结束日期必须大于开始日期");
        }

        // 同一用户同一课程不允许重复创建活跃计划
        Long existing = studyPlanMapper.selectCount(
                new LambdaQueryWrapper<StudyPlan>()
                        .eq(StudyPlan::getUserId, userId)
                        .eq(StudyPlan::getCourseId, request.getCourseId()));
        if (existing != null && existing > 0) {
            throw new RuntimeException("该课程已有学习计划，请先删除或修改");
        }

        StudyPlan plan = new StudyPlan();
        plan.setUserId(userId);
        plan.setCourseId(request.getCourseId());
        plan.setStartDate(request.getStartDate());
        plan.setEndDate(request.getEndDate());
        plan.setDailyHours(request.getDailyHours());
        studyPlanMapper.insert(plan);

        return toPlanVO(plan);
    }

    public StudyPlanVO updatePlan(Integer userId, Integer planId, UpdateStudyPlanRequest request) {
        StudyPlan plan = studyPlanMapper.selectById(planId);
        if (plan == null || !plan.getUserId().equals(userId)) {
            throw new RuntimeException("学习计划不存在或无权修改");
        }

        if (request.getEndDate() != null && request.getStartDate() != null
                && !request.getEndDate().isAfter(request.getStartDate())) {
            throw new RuntimeException("结束日期必须大于开始日期");
        }

        if (request.getStartDate() != null) plan.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) plan.setEndDate(request.getEndDate());
        if (request.getDailyHours() != null) plan.setDailyHours(request.getDailyHours());
        studyPlanMapper.updateById(plan);

        return toPlanVO(plan);
    }

    public boolean deletePlan(Integer userId, Integer planId) {
        StudyPlan plan = studyPlanMapper.selectById(planId);
        if (plan == null || !plan.getUserId().equals(userId)) {
            return false;
        }
        studyPlanMapper.deleteById(planId);
        return true;
    }

    public List<StudyPlanVO> getMyPlans(Integer userId, Integer courseId) {
        LambdaQueryWrapper<StudyPlan> wrapper = new LambdaQueryWrapper<StudyPlan>()
                .eq(StudyPlan::getUserId, userId)
                .eq(courseId != null, StudyPlan::getCourseId, courseId)
                .orderByDesc(StudyPlan::getCreateTime);
        List<StudyPlan> plans = studyPlanMapper.selectList(wrapper);
        return plans.stream().map(this::toPlanVO).collect(Collectors.toList());
    }

    public StudyPlanVO getPlan(Integer userId, Integer planId) {
        StudyPlan plan = studyPlanMapper.selectById(planId);
        if (plan == null || !plan.getUserId().equals(userId)) {
            return null;
        }
        return toPlanVO(plan);
    }

    private StudyPlanVO toPlanVO(StudyPlan plan) {
        StudyPlanVO vo = new StudyPlanVO();
        vo.setId(plan.getId());
        vo.setUserId(plan.getUserId());
        vo.setCourseId(plan.getCourseId());
        vo.setStartDate(plan.getStartDate());
        vo.setEndDate(plan.getEndDate());
        vo.setDailyHours(plan.getDailyHours());
        vo.setCreateTime(plan.getCreateTime());

        try {
            Course course = courseService.getCourseById(plan.getCourseId());
            vo.setCourseName(course.getName());
        } catch (Exception e) {
            vo.setCourseName("未知课程");
        }
        return vo;
    }
}
