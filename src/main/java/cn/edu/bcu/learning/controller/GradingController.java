package cn.edu.bcu.learning.controller;

import cn.edu.bcu.learning.domain.dto.GradingDiagnoseRequest;
import cn.edu.bcu.learning.domain.dto.GradingRequest;
import cn.edu.bcu.learning.domain.vo.GradingRecordVO;
import cn.edu.bcu.learning.domain.vo.GradingResultVO;
import cn.edu.bcu.learning.service.GradingService;
import cn.edu.bcu.learning.utils.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 主观题判分 + 大模型诊断推荐接口。
 */
@RestController
@RequestMapping("/grading")
@RequiredArgsConstructor
public class GradingController {

    private final GradingService gradingService;

    /**
     * 判分触发 — POST /grading/grade
     * body: { homeworkId? | submissionId?, questionId? }
     *   - 按作业维度：homeworkId，对该作业全部提交批量判分
     *   - 按提交维度：submissionId，对单条提交判分
     */
    @PostMapping("/grade")
    public Result<List<GradingResultVO>> grade(@RequestBody GradingRequest request) {
        return Result.success(gradingService.grade(request));
    }

    /**
     * 判分记录查询 — GET /grading/records?submissionId=&userId=
     * 两个参数至少传一个。
     */
    @GetMapping("/records")
    public Result<List<GradingRecordVO>> records(
            @RequestParam(required = false) Long submissionId,
            @RequestParam(required = false) Long userId) {
        return Result.success(gradingService.listRecords(submissionId, userId));
    }

    /**
     * 大模型诊断推荐 — POST /grading/diagnose
     * body: { submissionId? | userAnswer, standardAnswer?, questionId? }
     * 基于主观题作答生成诊断结论与学习推荐（不落库，仅返回）。
     */
    @PostMapping("/diagnose")
    public Result<GradingResultVO> diagnose(@RequestBody GradingDiagnoseRequest request) {
        return Result.success(gradingService.diagnose(request));
    }
}
