package cn.edu.bcu.learning.controller;

import cn.edu.bcu.learning.domain.vo.RadarItemVO;
import cn.edu.bcu.learning.domain.vo.WeakPointVO;
import cn.edu.bcu.learning.service.CourseService;
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
    private final CourseService courseService;

    /**
     * 雷达图数据
     */
    @GetMapping("/radar")
    public Result<List<RadarItemVO>> getRadar(
            @RequestParam Integer userId,
            @RequestParam Integer courseId) {
        String source = courseService.getCourseById(courseId).getSource();
        return Result.success(diagnosisService.getRadar(userId, source));
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
}