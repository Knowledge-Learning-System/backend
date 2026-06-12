package cn.edu.bcu.learning.controller;

import cn.edu.bcu.learning.domain.vo.LearningPathItemVO;
import cn.edu.bcu.learning.domain.vo.RecommendationVO;
import cn.edu.bcu.learning.domain.vo.ReminderVO;
import cn.edu.bcu.learning.service.StudyPlanService;
import cn.edu.bcu.learning.utils.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/study")
@RequiredArgsConstructor
public class StudyPlanController {

    private final StudyPlanService studyPlanService;

    /**
     * 个性化推荐：推荐下一个可学习节点
     */
    @GetMapping("/recommendations")
    public Result<List<RecommendationVO>> getRecommendations(
            @RequestParam Integer userId,
            @RequestParam Integer courseId) {
        return Result.success(studyPlanService.getRecommendations(userId, courseId));
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
     * 复习提醒
     */
    @GetMapping("/reminders")
    public Result<List<ReminderVO>> getReminders(
            @RequestParam Integer userId,
            @RequestParam Integer courseId) {
        return Result.success(studyPlanService.getReminders(userId, courseId));
    }
}