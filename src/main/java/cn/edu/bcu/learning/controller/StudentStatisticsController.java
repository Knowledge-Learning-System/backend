package cn.edu.bcu.learning.controller;

import cn.edu.bcu.learning.domain.vo.StudentOverviewVO;
import cn.edu.bcu.learning.service.StudentStatisticsService;
import cn.edu.bcu.learning.utils.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学生端学习统计真实链路（功能3）。
 * GET /statistics/student/overview?userId=xxx&courseId=xxx
 * 返回真实聚合数据；无数据时各字段为 0/空列表，不再有 Mock 兜底。
 */
@RestController
@RequestMapping("/statistics/student")
@RequiredArgsConstructor
public class StudentStatisticsController {

    private final StudentStatisticsService studentStatisticsService;

    @GetMapping("/overview")
    public Result<StudentOverviewVO> overview(@RequestParam Integer userId, @RequestParam Integer courseId) {
        return Result.success(studentStatisticsService.getOverview(userId, courseId));
    }
}
