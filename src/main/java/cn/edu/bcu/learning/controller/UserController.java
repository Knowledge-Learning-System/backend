package cn.edu.bcu.learning.controller;

import cn.edu.bcu.learning.config.JwtInterceptor;
import cn.edu.bcu.learning.domain.dto.SwitchCourseRequest;
import cn.edu.bcu.learning.service.CourseService;
import cn.edu.bcu.learning.utils.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final CourseService courseService;

    @PutMapping("/current-course")
    public Result<Void> switchCurrentCourse(@RequestBody SwitchCourseRequest request,
                                            HttpServletRequest httpRequest) {
        Integer userId = (Integer) httpRequest.getAttribute(JwtInterceptor.USER_ID_ATTR);
        courseService.switchCurrentCourse(userId, request.getCourseId());
        return Result.success();
    }
}
