package cn.edu.bcu.learning.controller;

import cn.edu.bcu.learning.annotation.RequireRole;
import cn.edu.bcu.learning.domain.dto.CreateHomeworkRequest;
import cn.edu.bcu.learning.domain.entity.HomeworkSubmission;
import cn.edu.bcu.learning.domain.vo.HomeworkSubmissionVO;
import cn.edu.bcu.learning.domain.vo.HomeworkVO;
import cn.edu.bcu.learning.service.HomeworkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/homework")
public class HomeworkController {

    @Autowired
    private HomeworkService homeworkService;

    /** 创建作业 — POST /homework */
    @RequireRole("teacher")
    @PostMapping
    public HomeworkVO create(@RequestBody CreateHomeworkRequest request) {
        return null; // TODO
    }

    /** 发布作业 — PUT /homework/{id}/publish */
    @RequireRole("teacher")
    @PutMapping("/{id}/publish")
    public void publish(@PathVariable Long id) {
        homeworkService.publishHomework(id);
    }

    /** 作业列表 — GET /homework?courseId= */
    @GetMapping
    public List<HomeworkVO> list(@RequestParam Long courseId) {
        return homeworkService.listHomeworkByCourse(courseId);
    }

    /** 作业详情 — GET /homework/{id} */
    @GetMapping("/{id}")
    public HomeworkVO detail(@PathVariable Long id) {
        return homeworkService.getHomeworkDetail(id);
    }

    /** 提交作业 — POST /homework/{id}/submit */
    @PostMapping("/{id}/submit")
    public void submit(@PathVariable Long id, @RequestAttribute Long userId,
                       @RequestBody HomeworkSubmission submission) {
        homeworkService.submitHomework(id, userId, submission);
    }

    /** 查看提交列表 — GET /homework/{id}/submissions */
    @RequireRole("teacher")
    @GetMapping("/{id}/submissions")
    public List<HomeworkSubmissionVO> submissions(@PathVariable Long id) {
        return homeworkService.getSubmissionsByHomework(id);
    }

    /** 评分+反馈 — PUT /homework/submission/{id}/grade */
    @RequireRole("teacher")
    @PutMapping("/submission/{id}/grade")
    public void grade(@PathVariable Long id,
                      @RequestParam Integer score,
                      @RequestParam(required = false) String feedback) {
        homeworkService.gradeSubmission(id, score, feedback);
    }
}
