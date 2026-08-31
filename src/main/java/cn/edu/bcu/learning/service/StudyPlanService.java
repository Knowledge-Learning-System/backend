package cn.edu.bcu.learning.service;

import cn.edu.bcu.learning.domain.dto.CreateStudyPlanRequest;
import cn.edu.bcu.learning.domain.dto.UpdateStudyPlanRequest;
import cn.edu.bcu.learning.domain.entity.Course;
import cn.edu.bcu.learning.domain.entity.DailyStudyRecord;
import cn.edu.bcu.learning.domain.entity.KnowledgeMastery;
import cn.edu.bcu.learning.domain.entity.StudyPlan;
import cn.edu.bcu.learning.domain.entity.UserAnswerRecord;
import cn.edu.bcu.learning.domain.vo.*;
import cn.edu.bcu.learning.repository.mysql.DailyStudyRecordMapper;
import cn.edu.bcu.learning.repository.mysql.KnowledgeMasteryMapper;
import cn.edu.bcu.learning.repository.mysql.StudyPlanMapper;
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

@Service
@RequiredArgsConstructor
public class StudyPlanService {

    private final KnowledgeGraphRepository knowledgeGraphRepository;
    private final KnowledgeMasteryMapper knowledgeMasteryMapper;
    private final UserAnswerRecordMapper userAnswerRecordMapper;
    private final StudyPlanMapper studyPlanMapper;
    private final DailyStudyRecordMapper dailyStudyRecordMapper;
    private final CourseService courseService;
    private final QuestionService questionService;
    private final AnalysisAlgorithmService analysisAlgorithmService;

    /**
     * 个性化推荐：综合打分排序（先修满足度 + 难度权重 + 掌握度接近阈值），
     * 由 AnalysisAlgorithmService.recommendLearningPath 实现。
     */
    public List<RecommendationVO> getRecommendations(Integer userId, Integer courseId) {
        return analysisAlgorithmService.recommendLearningPath(userId.longValue(), courseId.longValue());
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
        plan.setDailyTarget(request.getDailyTarget());
        plan.setRemindTime(request.getRemindTime());
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
        if (request.getDailyTarget() != null) plan.setDailyTarget(request.getDailyTarget());
        if (request.getRemindTime() != null) plan.setRemindTime(request.getRemindTime());
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
        vo.setDailyTarget(plan.getDailyTarget());
        vo.setRemindTime(plan.getRemindTime());
        vo.setCreateTime(plan.getCreateTime());

        try {
            Course course = courseService.getCourseById(plan.getCourseId());
            vo.setCourseName(course.getName());
        } catch (Exception e) {
            vo.setCourseName("未知课程");
        }
        return vo;
    }

    /**
     * 记录当天学习（视频播放结束触发），按知识点维度记录，每天每个知识点一条。
     */
    public void recordStudy(Integer userId, Integer courseId, String knowledgePointId) {
        if (knowledgePointId == null || knowledgePointId.isEmpty()) {
            return;
        }
        LocalDate today = LocalDate.now();
        Long count = dailyStudyRecordMapper.selectCount(
                new LambdaQueryWrapper<DailyStudyRecord>()
                        .eq(DailyStudyRecord::getUserId, userId)
                        .eq(DailyStudyRecord::getKnowledgePointId, knowledgePointId)
                        .eq(DailyStudyRecord::getStudyDate, today));
        if (count != null && count > 0) {
            return;
        }
        DailyStudyRecord record = new DailyStudyRecord();
        record.setUserId(userId);
        record.setCourseId(courseId);
        record.setKnowledgePointId(knowledgePointId);
        record.setStudyDate(today);
        dailyStudyRecordMapper.insert(record);
    }

    /**
     * 今日测试：从当天学习过的章节随机抽 5 道题。
     */
    public List<AnswerDetailVO> getDailyQuiz(Integer userId, Integer courseId) {
        LocalDate today = LocalDate.now();
        List<DailyStudyRecord> records = dailyStudyRecordMapper.selectList(
                new LambdaQueryWrapper<DailyStudyRecord>()
                        .eq(DailyStudyRecord::getUserId, userId)
                        .eq(DailyStudyRecord::getCourseId, courseId)
                        .eq(DailyStudyRecord::getStudyDate, today));
        if (records.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> kpIds = records.stream()
                .map(DailyStudyRecord::getKnowledgePointId)
                .distinct()
                .collect(Collectors.toList());
        List<AnswerDetailVO> questions = questionService.getQuestionsByKpIds(courseId, kpIds);
        Collections.shuffle(questions);
        return questions.stream().limit(5).collect(Collectors.toList());
    }
}
