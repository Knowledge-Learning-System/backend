package cn.edu.bcu.learning.controller;

import cn.edu.bcu.learning.domain.entity.Course;
import cn.edu.bcu.learning.domain.vo.AiAnalysisVO;
import cn.edu.bcu.learning.domain.vo.RadarItemVO;
import cn.edu.bcu.learning.domain.vo.TrendPointVO;
import cn.edu.bcu.learning.domain.vo.WeakPointVO;
import cn.edu.bcu.learning.service.CourseService;
import cn.edu.bcu.learning.service.DiagnosisAiService;
import cn.edu.bcu.learning.service.DiagnosisService;
import cn.edu.bcu.learning.utils.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/diagnosis")
@RequiredArgsConstructor
public class DiagnosisController {

    private final DiagnosisService diagnosisService;
    private final DiagnosisAiService diagnosisAiService;
    private final CourseService courseService;

    /**
     * 雷达图数据
     *
     * @param dimension 分析维度：overall 整体掌握度（默认）/ by_domain 按知识领域 / by_time 按时间趋势（此处回落到整体）
     */
    @GetMapping("/radar")
    public Result<List<RadarItemVO>> getRadar(
            @RequestParam Integer userId,
            @RequestParam Integer courseId,
            @RequestParam(required = false, defaultValue = "overall") String dimension) {
        String source = courseService.getCourseById(courseId).getSource();
        return Result.success(diagnosisService.getRadar(userId, source, dimension));
    }

    /**
     * 掌握度时间趋势（按自然日，含每日答题量与截至当日的累计掌握度）
     *
     * @param days 最多返回的天数（含今天），默认 30
     */
    @GetMapping("/trend")
    public Result<List<TrendPointVO>> getTrend(
            @RequestParam Integer userId,
            @RequestParam Integer courseId,
            @RequestParam(required = false, defaultValue = "30") Integer days) {
        String source = courseService.getCourseById(courseId).getSource();
        return Result.success(diagnosisService.getTrend(userId, source, days == null ? 30 : days));
    }

    /**
     * 薄弱点查询
     */
    @GetMapping("/weak-points")
    public Result<List<WeakPointVO>> getWeakPoints(
            @RequestParam Integer userId,
            @RequestParam Integer courseId) {
        String source = courseService.getCourseById(courseId).getSource();
        return Result.success(diagnosisService.getWeakPoints(userId, source));
    }

    /**
     * 大模型分析建议（诊断页底部卡片）
     *
     * @param days 趋势回看天数，默认 30
     */
    @GetMapping("/ai-analysis")
    public Result<AiAnalysisVO> getAiAnalysis(
            @RequestParam Integer userId,
            @RequestParam Integer courseId,
            @RequestParam(required = false, defaultValue = "30") Integer days) {
        Course course = courseService.getCourseById(courseId);
        return Result.success(diagnosisAiService.analyze(
                userId, courseId, course.getName(), course.getSource(), days == null ? 30 : days));
    }
}