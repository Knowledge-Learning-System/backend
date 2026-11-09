package cn.edu.bcu.learning.controller;

import cn.edu.bcu.learning.annotation.RequireRole;
import cn.edu.bcu.learning.domain.dto.EnrollRequest;
import cn.edu.bcu.learning.domain.dto.SwitchCourseRequest;
import cn.edu.bcu.learning.domain.entity.Course;
import cn.edu.bcu.learning.domain.entity.KnowledgeMastery;
import cn.edu.bcu.learning.domain.vo.*;
import cn.edu.bcu.learning.repository.mysql.KnowledgeMasteryMapper;
import cn.edu.bcu.learning.repository.neo4j.KnowledgeGraphRepository;
import cn.edu.bcu.learning.service.AnalysisAlgorithmService;
import cn.edu.bcu.learning.service.CourseService;
import cn.edu.bcu.learning.service.KnowledgeGraphService;
import cn.edu.bcu.learning.utils.Result;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/courses")
public class CourseController {

    private final CourseService courseService;
    private final KnowledgeGraphService knowledgeGraphService;
    private final KnowledgeGraphRepository knowledgeGraphRepository;
    private final KnowledgeMasteryMapper knowledgeMasteryMapper;
    private final AnalysisAlgorithmService analysisAlgorithmService;

    public CourseController(CourseService courseService, KnowledgeGraphService knowledgeGraphService,
                            KnowledgeGraphRepository knowledgeGraphRepository, KnowledgeMasteryMapper knowledgeMasteryMapper,
                            AnalysisAlgorithmService analysisAlgorithmService) {
        this.courseService = courseService;
        this.knowledgeGraphService = knowledgeGraphService;
        this.knowledgeGraphRepository = knowledgeGraphRepository;
        this.knowledgeMasteryMapper = knowledgeMasteryMapper;
        this.analysisAlgorithmService = analysisAlgorithmService;
        System.out.println("CourseController 初始化成功！");
    }

    @GetMapping
    public Result<?> listCourses() {
        return Result.success().setData(courseService.listCourses());
    }

    @GetMapping("/my")
    public Result<?> listMyCourses(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        return Result.success().setData(courseService.listMyCourses(userId));
    }

    /** 教师"我的课程"（按 course.teacher_id 归属）— GET /courses/my-teaching */
    @RequireRole("teacher")
    @GetMapping("/my-teaching")
    public Result<List<CourseVO>> listMyTeachingCourses(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        return Result.success(courseService.listTeachingCourses(userId));
    }

    /** 管理员：所有教师及其负责课程（教师-课程分配）— GET /courses/teachers */
    @RequireRole("admin")
    @GetMapping("/teachers")
    public Result<List<TeacherCourseVO>> listTeacherCourses() {
        return Result.success(courseService.listTeacherCourses());
    }

    @PostMapping("/enroll")
    public Result<?> enroll(@RequestBody EnrollRequest enrollRequest, HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        courseService.enroll(userId, enrollRequest.getCourseId());
        return Result.success();
    }

    @PutMapping("/current")
    public Result<?> switchCurrentCourse(@RequestBody SwitchCourseRequest switchRequest, HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        courseService.switchCurrentCourse(userId, switchRequest.getCourseId());
        return Result.success();
    }

    @DeleteMapping("/enroll/{courseId}")
    public Result<?> unenroll(@PathVariable Integer courseId, HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        courseService.unenroll(userId, courseId);
        return Result.success();
    }

    // 课程进度（动态计算，基于 KnowledgeMastery）
    @GetMapping("/{courseId}/progress")
    public Result<?> getCourseProgress(@PathVariable Integer courseId, HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        int progress = courseService.calculateCourseProgress(userId, courseId);
        return Result.success().setData(Map.of("progress", progress));
    }

    // 学习路径导航
    @GetMapping("/{courseId}/learning-path")
    public Result<List<LearningPathItemVO>> getLearningPath(@PathVariable Integer courseId) {
        Course course = courseService.getCourseById(courseId);
        return Result.success(knowledgeGraphService.getLearningPathBySource(course.getSource()));
    }

    // 章-知识点层级结构（含视频/课件/测试题）
    @GetMapping("/{courseId}/chapters")
    public Result<List<SubTopicVO>> getChapters(@PathVariable Integer courseId) {
        Course course = courseService.getCourseById(courseId);
        String source = (course.getSource() == null || course.getSource().isBlank())
                ? "course_" + courseId : course.getSource();
        return Result.success(knowledgeGraphService.getChapterStructure(courseId, source));
    }

    // 学习推荐：指定 kpId 时返回该知识点的前置推荐；未指定时返回整课推荐
    @GetMapping("/{courseId}/recommendation")
    public Result<List<RecommendationVO>> getRecommendation(@PathVariable Integer courseId,
                                                           @RequestParam(required = false) String kpId,
                                                           HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            return Result.success(Collections.emptyList());
        }
        if (kpId == null || kpId.isBlank()) {
            return Result.success(analysisAlgorithmService.recommendLearningPath(userId.longValue(), courseId.longValue()));
        }
        return Result.success(analysisAlgorithmService.recommendPrerequisites(userId, courseId, kpId.trim()));
    }

    @RequireRole("teacher")
    @PostMapping("/add")
    public Result<Course> addCourse(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String name = body.get("name");
        String description = body.get("description");
        String cover = body.get("cover");
        String source = body.get("source");
        if (name == null || name.isBlank()) {
            return Result.fail("课程名称不能为空");
        }
        Integer teacherId = (Integer) request.getAttribute("userId");
        Course course = courseService.addCourse(name, description, cover, source, teacherId);
        return Result.success(course);
    }

    /** 编辑课程（教师端）— PUT /courses/{courseId} */
    @RequireRole("teacher")
    @PutMapping("/{courseId}")
    public Result<Course> updateCourse(@PathVariable Integer courseId, @RequestBody Map<String, String> body) {
        if (body.containsKey("name") && (body.get("name") == null || body.get("name").isBlank())) {
            return Result.fail("课程名称不能为空");
        }
        return Result.success(courseService.updateCourse(courseId, body));
    }

    /** 删除课程（教师端，软删除）— DELETE /courses/{courseId} */
    @RequireRole("teacher")
    @DeleteMapping("/{courseId}")
    public Result<Void> deleteCourse(@PathVariable Integer courseId) {
        courseService.deleteCourse(courseId);
        return Result.success();
    }
}