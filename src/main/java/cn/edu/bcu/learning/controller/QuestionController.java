package cn.edu.bcu.learning.controller;

import cn.edu.bcu.learning.domain.dto.SubmitAnswerRequest;
import cn.edu.bcu.learning.domain.entity.Course;
import cn.edu.bcu.learning.domain.vo.AnswerDetailVO;
import cn.edu.bcu.learning.domain.vo.SubmitAnswerResultVO;
import cn.edu.bcu.learning.service.CourseService;
import cn.edu.bcu.learning.service.DiagnosisService;
import cn.edu.bcu.learning.service.QuestionService;
import cn.edu.bcu.learning.service.UserBehaviorService;
import cn.edu.bcu.learning.utils.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;
    private final DiagnosisService diagnosisService;
    private final UserBehaviorService userBehaviorService;
    private final CourseService courseService;

    @GetMapping
    public Result<List<AnswerDetailVO>> getQuestions(
            @RequestParam Integer courseId,
            @RequestParam(required = false) String knowledgePointId,
            @RequestParam Integer userId) {
        userBehaviorService.record(userId, knowledgePointId, "view_kp");
        return Result.success(questionService.getQuestions(courseId, knowledgePointId));
    }

    @PostMapping("/submit")
    public Result<SubmitAnswerResultVO> submit(@RequestBody SubmitAnswerRequest request) {
        SubmitAnswerResultVO result = questionService.submit(request);
        Course course = courseService.getCourseById(request.getCourseId());
        diagnosisService.calculateMastery(request.getUserId(), request.getCourseId(), course.getSource());
        userBehaviorService.record(request.getUserId(), null, "complete_quiz");
        return Result.success(result);
    }
}
