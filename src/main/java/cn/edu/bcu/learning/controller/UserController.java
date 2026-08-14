package cn.edu.bcu.learning.controller;

import cn.edu.bcu.learning.annotation.RequireRole;
import cn.edu.bcu.learning.config.JwtInterceptor;
import cn.edu.bcu.learning.domain.dto.ImportStudentResultVO;
import cn.edu.bcu.learning.domain.dto.SwitchCourseRequest;
import cn.edu.bcu.learning.domain.entity.User;
import cn.edu.bcu.learning.domain.vo.UserVO;
import cn.edu.bcu.learning.repository.mysql.UserMapper;
import cn.edu.bcu.learning.service.CourseService;
import cn.edu.bcu.learning.service.StudentImportService;
import cn.edu.bcu.learning.service.UserBehaviorService;
import cn.edu.bcu.learning.utils.Result;
import cn.edu.bcu.learning.utils.UserConverter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final CourseService courseService;
    private final UserBehaviorService userBehaviorService;
    private final UserMapper userMapper;
    private final StudentImportService studentImportService;

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

    /** 学生列表（教师端）— GET /users/students */
    @RequireRole("teacher")
    @GetMapping("/students")
    public Result<List<UserVO>> listStudents() {
        List<User> students = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getRole, "student")
                .orderByAsc(User::getId));
        return Result.success(students.stream().map(UserConverter::toVO).collect(Collectors.toList()));
    }

    /** Excel 批量导入学生 — POST /users/import */
    @RequireRole("teacher")
    @PostMapping("/import")
    public Result<ImportStudentResultVO> importStudents(@RequestParam("file") MultipartFile file,
                                                        @RequestParam(required = false) Long courseId) {
        return Result.success(studentImportService.importFromExcel(file, courseId));
    }
}
