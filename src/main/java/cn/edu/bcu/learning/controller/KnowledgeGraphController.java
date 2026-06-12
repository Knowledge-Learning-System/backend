package cn.edu.bcu.learning.controller;

import cn.edu.bcu.learning.domain.entity.Course;
import cn.edu.bcu.learning.domain.vo.KnowledgeGraphVO;
import cn.edu.bcu.learning.service.CourseService;
import cn.edu.bcu.learning.service.KnowledgeGraphService;
import cn.edu.bcu.learning.utils.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/knowledge-graph")
@RequiredArgsConstructor
public class KnowledgeGraphController {

    private final KnowledgeGraphService knowledgeGraphService;
    private final CourseService courseService;

    @GetMapping("/{courseId}")
    public Result<KnowledgeGraphVO> getGraph(@PathVariable Integer courseId) {
        Course course = courseService.getCourseById(courseId);
        return Result.success(knowledgeGraphService.getGraphBySource(course.getSource()));
    }
}
