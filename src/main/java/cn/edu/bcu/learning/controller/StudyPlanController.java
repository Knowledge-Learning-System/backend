package cn.edu.bcu.learning.controller;

import cn.edu.bcu.learning.domain.dto.CreateStudyPlanRequest;
import cn.edu.bcu.learning.domain.dto.UpdateStudyPlanRequest;
import cn.edu.bcu.learning.domain.vo.AnswerDetailVO;
import cn.edu.bcu.learning.domain.vo.FullStudyPlanVO;
import cn.edu.bcu.learning.domain.vo.LearningPathItemVO;
import cn.edu.bcu.learning.domain.vo.RecommendationVO;
import cn.edu.bcu.learning.domain.vo.ReminderVO;
import cn.edu.bcu.learning.domain.vo.StudyPlanVO;
import cn.edu.bcu.learning.service.StudyPlanService;
import cn.edu.bcu.learning.service.UserBehaviorService;
import cn.edu.bcu.learning.utils.Result;
import cn.edu.bcu.learning.utils.ThreadLocalUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/study")
@RequiredArgsConstructor
public class StudyPlanController {

    private final StudyPlanService studyPlanService;
    private final UserBehaviorService userBehaviorService;

    /**
     * 个性化推荐：推荐下一个可学习节点
     */
    @GetMapping("/recommendations")
    public Result<List<RecommendationVO>> getRecommendations(
            @RequestParam Integer userId,
            @RequestParam Integer courseId,
            @RequestParam(required = false) String kpId) {
        return Result.success(studyPlanService.getRecommendations(userId, courseId, kpId));
    }

    /**
     * 学习路径规划（按天数分组）
     */
    @GetMapping("/plan")
    public Result<Map<Integer, List<LearningPathItemVO>>> getStudyPlan(
            @RequestParam Integer userId,
            @RequestParam Integer courseId) {
        return Result.success(studyPlanService.getStudyPlan(userId, courseId));
    }

    /**
     * 完整学习计划：按知识图谱 PREREQUISITE 依赖拓扑序生成全部学习步骤，
     * 含依赖说明、完成状态（knowledge_mastery >= 60）与总进度。学生按当前课程获取。
     */
    @GetMapping("/full-plan")
    public Result<FullStudyPlanVO> getFullPlan(@RequestParam Integer courseId) {
        Integer userId = ThreadLocalUtil.getUserId();
        return Result.success(studyPlanService.getFullPlan(userId, courseId));
    }

    /**
     * 标记学习步骤完成/取消完成
     * body: { courseId, kpId, completed }
     * 落库 knowledge_mastery（完成置 100、取消置 0），并同步 user_course.progress。
     */
    @PostMapping("/plan/complete")
    public Result<?> completePlanStep(@RequestBody Map<String, Object> body) {
        Integer userId = ThreadLocalUtil.getUserId();
        Integer courseId = body.get("courseId") == null ? 0 : Integer.valueOf(body.get("courseId").toString());
        String kpId = body.get("kpId") == null ? null : body.get("kpId").toString();
        boolean completed = body.get("completed") == null
                || Boolean.parseBoolean(body.get("completed").toString());
        studyPlanService.completePlanStep(userId, courseId, kpId, completed);
        return Result.success();
    }

    /**
     * 复习提醒
     */
    @GetMapping("/reminders")
    public Result<List<ReminderVO>> getReminders(
            @RequestParam Integer userId,
            @RequestParam Integer courseId) {
        return Result.success(studyPlanService.getReminders(userId, courseId));
    }

    // ========== 学习计划 CRUD ==========

    @PostMapping("/plan")
    public Result<StudyPlanVO> createPlan(@RequestBody CreateStudyPlanRequest request) {
        Integer userId = ThreadLocalUtil.getUserId();
        return Result.success(studyPlanService.createPlan(userId, request));
    }

    @PutMapping("/plan/{id}")
    public Result<StudyPlanVO> updatePlan(@PathVariable Integer id,
                                           @RequestBody UpdateStudyPlanRequest request) {
        Integer userId = ThreadLocalUtil.getUserId();
        return Result.success(studyPlanService.updatePlan(userId, id, request));
    }

    @DeleteMapping("/plan/{id}")
    public Result<?> deletePlan(@PathVariable Integer id) {
        Integer userId = ThreadLocalUtil.getUserId();
        boolean ok = studyPlanService.deletePlan(userId, id);
        return ok ? Result.success() : Result.fail("无权删除或计划不存在");
    }

    @GetMapping("/plans")
    public Result<List<StudyPlanVO>> getMyPlans(@RequestParam(required = false) Integer courseId) {
        Integer userId = ThreadLocalUtil.getUserId();
        return Result.success(studyPlanService.getMyPlans(userId, courseId));
    }

    /**
     * 记录当天学习（视频播放结束触发）
     */
    @PostMapping("/record")
    public Result<?> recordStudy(@RequestBody Map<String, Object> body) {
        Integer userId = ThreadLocalUtil.getUserId();
        Integer courseId = body.get("courseId") == null ? 0 : Integer.valueOf(body.get("courseId").toString());
        String knowledgePointId = body.get("knowledgePointId") == null ? null : body.get("knowledgePointId").toString();
        studyPlanService.recordStudy(userId, courseId, knowledgePointId);
        return Result.success();
    }

    /**
     * 上报学习停留时长（功能3-学习时长采集）
     * body: { durationSeconds: 本次累计秒, knowledgePointId?: 可选 }
     * 以 action=study_time 落库 user_behavior，统计接口按 userId 聚合。
     */
    @PostMapping("/time")
    public Result<?> recordStudyTime(@RequestBody Map<String, Object> body) {
        Integer userId = ThreadLocalUtil.getUserId();
        Integer durationSeconds = body.get("durationSeconds") == null ? 0
                : Integer.valueOf(body.get("durationSeconds").toString());
        String knowledgePointId = body.get("knowledgePointId") == null
                ? null : body.get("knowledgePointId").toString();
        if (durationSeconds == null || durationSeconds <= 0) {
            return Result.success();
        }
        userBehaviorService.recordStudyTime(userId, knowledgePointId, durationSeconds);
        return Result.success();
    }

    /**
     * 今日测试：从当天学习过的章节随机抽 5 道题
     */
    @GetMapping("/daily-quiz")
    public Result<List<AnswerDetailVO>> getDailyQuiz(@RequestParam Integer courseId) {
        Integer userId = ThreadLocalUtil.getUserId();
        return Result.success(studyPlanService.getDailyQuiz(userId, courseId));
    }
}