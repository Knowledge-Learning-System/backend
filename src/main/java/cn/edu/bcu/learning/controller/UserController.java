package cn.edu.bcu.learning.controller;

import cn.edu.bcu.learning.config.JwtInterceptor;
import cn.edu.bcu.learning.domain.dto.SwitchCourseRequest;
import cn.edu.bcu.learning.service.CourseService;
import cn.edu.bcu.learning.service.UserBehaviorService;
import cn.edu.bcu.learning.utils.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final CourseService courseService;
    private final UserBehaviorService userBehaviorService;

    @PutMapping("/current-course")
    public Result<Void> switchCurrentCourse(@RequestBody SwitchCourseRequest request,
                                            HttpServletRequest httpRequest) {
        Integer userId = (Integer) httpRequest.getAttribute(JwtInterceptor.USER_ID_ATTR);
        courseService.switchCurrentCourse(userId, request.getCourseId());
        return Result.success();
    }

    @GetMapping("/active-days")
    public Result<Map<String, Integer>> getActiveDays(@RequestParam Integer userId) {
        int days = userBehaviorService.getActiveDays(userId);
        return Result.success(Map.of("activeDays", days));
    }
}
