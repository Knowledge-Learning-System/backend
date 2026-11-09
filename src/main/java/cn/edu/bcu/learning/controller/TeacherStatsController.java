package cn.edu.bcu.learning.controller;

import cn.edu.bcu.learning.annotation.RequireRole;
import cn.edu.bcu.learning.domain.vo.ClassOverviewVO;
import cn.edu.bcu.learning.domain.vo.StudentDiagnosisVO;
import cn.edu.bcu.learning.domain.vo.StudentLearningDataVO;
import cn.edu.bcu.learning.domain.vo.TeacherCourseStudentVO;
import cn.edu.bcu.learning.domain.vo.WarningStudentVO;
import cn.edu.bcu.learning.service.TeacherStatsService;
import cn.edu.bcu.learning.utils.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 教师端教学洞察接口（教师端 5 项功能）：
 * 选课学生 / 班级学情统计 / 学生学习数据 / 预警学生识别 / 学生诊断报告。
 * 全部限定教师角色，且仅允许访问本人负责课程。
 */
@RestController
@RequestMapping("/teacher")
@RequiredArgsConstructor
public class TeacherStatsController {

    private final TeacherStatsService teacherStatsService;

    /** 功能1：查看选课学生 — GET /teacher/courses/{courseId}/students */
    @RequireRole("teacher")
    @GetMapping("/courses/{courseId}/students")
    public Result<List<TeacherCourseStudentVO>> listCourseStudents(
            @PathVariable Integer courseId, HttpServletRequest request) {
        Integer teacherId = (Integer) request.getAttribute("userId");
        return Result.success(teacherStatsService.listCourseStudents(teacherId, courseId));
    }

    /** 功能2：班级学情统计 — GET /teacher/courses/{courseId}/overview */
    @RequireRole("teacher")
    @GetMapping("/courses/{courseId}/overview")
    public Result<ClassOverviewVO> getClassOverview(
            @PathVariable Integer courseId, HttpServletRequest request) {
        Integer teacherId = (Integer) request.getAttribute("userId");
        return Result.success(teacherStatsService.getClassOverview(teacherId, courseId));
    }

    /** 功能3：学生学习数据 — GET /teacher/courses/{courseId}/students/{studentId}/learning-data */
    @RequireRole("teacher")
    @GetMapping("/courses/{courseId}/students/{studentId}/learning-data")
    public Result<StudentLearningDataVO> getStudentLearningData(
            @PathVariable Integer courseId,
            @PathVariable Integer studentId,
            HttpServletRequest request) {
        Integer teacherId = (Integer) request.getAttribute("userId");
        return Result.success(teacherStatsService.getStudentLearningData(teacherId, courseId, studentId));
    }

    /** 功能4：预警学生识别 — GET /teacher/courses/{courseId}/warnings */
    @RequireRole("teacher")
    @GetMapping("/courses/{courseId}/warnings")
    public Result<List<WarningStudentVO>> getWarningStudents(
            @PathVariable Integer courseId, HttpServletRequest request) {
        Integer teacherId = (Integer) request.getAttribute("userId");
        return Result.success(teacherStatsService.getWarningStudents(teacherId, courseId));
    }

    /** 功能5：学生诊断报告 — GET /teacher/courses/{courseId}/students/{studentId}/diagnosis */
    @RequireRole("teacher")
    @GetMapping("/courses/{courseId}/students/{studentId}/diagnosis")
    public Result<StudentDiagnosisVO> getStudentDiagnosis(
            @PathVariable Integer courseId,
            @PathVariable Integer studentId,
            HttpServletRequest request) {
        Integer teacherId = (Integer) request.getAttribute("userId");
        return Result.success(teacherStatsService.getStudentDiagnosis(teacherId, courseId, studentId));
    }
}
