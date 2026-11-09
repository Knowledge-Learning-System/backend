package cn.edu.bcu.learning.controller;

import cn.edu.bcu.learning.annotation.RequireRole;
import cn.edu.bcu.learning.config.JwtInterceptor;
import cn.edu.bcu.learning.domain.dto.ImportStudentResultVO;
import cn.edu.bcu.learning.domain.dto.SwitchCourseRequest;
import cn.edu.bcu.learning.domain.dto.UpdateStudentRequest;
import cn.edu.bcu.learning.domain.entity.Course;
import cn.edu.bcu.learning.domain.entity.User;
import cn.edu.bcu.learning.domain.entity.UserCourse;
import cn.edu.bcu.learning.domain.vo.UserVO;
import cn.edu.bcu.learning.repository.mysql.CourseMapper;
import cn.edu.bcu.learning.repository.mysql.UserCourseMapper;
import cn.edu.bcu.learning.repository.mysql.UserMapper;
import cn.edu.bcu.learning.service.CourseService;
import cn.edu.bcu.learning.service.StudentImportService;
import cn.edu.bcu.learning.service.UserBehaviorService;
import cn.edu.bcu.learning.utils.Result;
import cn.edu.bcu.learning.utils.UserConverter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final CourseService courseService;
    private final UserBehaviorService userBehaviorService;
    private final CourseMapper courseMapper;
    private final UserMapper userMapper;
    private final UserCourseMapper userCourseMapper;
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

    /** 学生列表（教师端）— GET /users/students，仅返回当前教师负责课程下的学生（按 course.teacher_id → user_course 关联过滤） */
    @RequireRole("teacher")
    @GetMapping("/students")
    public Result<List<UserVO>> listStudents(HttpServletRequest request) {
        Integer teacherId = (Integer) request.getAttribute(JwtInterceptor.USER_ID_ATTR);
        List<Integer> courseIds = getTeacherCourseIds(teacherId);
        if (courseIds.isEmpty()) {
            return Result.success(Collections.emptyList());
        }
        List<UserCourse> userCourses = userCourseMapper.selectList(new LambdaQueryWrapper<UserCourse>()
                .in(UserCourse::getCourseId, courseIds));
        List<Integer> studentIds = userCourses.stream()
                .map(UserCourse::getUserId)
                .distinct()
                .toList();
        if (studentIds.isEmpty()) {
            return Result.success(Collections.emptyList());
        }
        List<User> students = userMapper.selectBatchIds(studentIds).stream()
                .filter(user -> "student".equals(user.getRole()))
                .sorted(Comparator.comparing(User::getId))
                .toList();
        return Result.success(students.stream().map(UserConverter::toVO).collect(Collectors.toList()));
    }

    /** Excel 批量导入学生 — POST /users/import，仅允许导入到当前教师负责的课程 */
    @RequireRole("teacher")
    @PostMapping("/import")
    public Result<ImportStudentResultVO> importStudents(@RequestParam("file") MultipartFile file,
                                                        @RequestParam Long courseId,
                                                        HttpServletRequest request) {
        Integer teacherId = (Integer) request.getAttribute(JwtInterceptor.USER_ID_ATTR);
        Course course = courseMapper.selectById(courseId.intValue());
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }
        if (!course.getTeacherId().equals(teacherId)) {
            throw new RuntimeException("无权向该课程导入学生");
        }
        return Result.success(studentImportService.importFromExcel(file, courseId));
    }

    /**
     * 教师端：下载学生批量导入 Excel 模板（第一列为学号，第二列为姓名，与 /users/import 解析约定一致）。
     */
    @RequireRole("teacher")
    @GetMapping("/import-template")
    public void downloadImportTemplate(HttpServletResponse response) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("学生导入模板");
            String[] headers = {"学号", "姓名"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            Object[][] samples = {
                    {"20230001", "张三"},
                    {"20230002", "李四"},
            };
            for (int r = 0; r < samples.length; r++) {
                Row row = sheet.createRow(r + 1);
                Object[] sample = samples[r];
                for (int c = 0; c < sample.length; c++) {
                    row.createCell(c).setCellValue(String.valueOf(sample[c]));
                }
            }
            int[] widths = {16, 16};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String encoded = URLEncoder.encode("学生导入模板.xlsx", StandardCharsets.UTF_8);
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
            workbook.write(response.getOutputStream());
            response.getOutputStream().flush();
        }
    }

    /**
     * 编辑学生信息（教师端）— PUT /users/{id}
     * 仅允许修改姓名 nickname，学号 username 与角色 role 不可修改；
     * 且仅允许操作当前教师负责课程下的学生。
     */
    @RequireRole("teacher")
    @PutMapping("/{id}")
    public Result<Void> updateStudent(@PathVariable Integer id, @RequestBody UpdateStudentRequest request,
                                      HttpServletRequest httpRequest) {
        if (request.getNickname() == null || request.getNickname().isBlank()) {
            return Result.fail("姓名不能为空");
        }
        Integer teacherId = (Integer) httpRequest.getAttribute(JwtInterceptor.USER_ID_ATTR);
        checkStudentOwnership(teacherId, id);
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("学生不存在");
        }
        if (!"student".equals(user.getRole())) {
            throw new RuntimeException("只能编辑学生信息");
        }
        user.setNickname(request.getNickname().trim());
        userMapper.updateById(user);
        return Result.success();
    }

    /** 删除学生（教师端）— DELETE /users/{id}，先清选课关联再删用户；仅允许操作当前教师负责课程下的学生 */
    @RequireRole("teacher")
    @DeleteMapping("/{id}")
    public Result<Void> deleteStudent(@PathVariable Integer id, HttpServletRequest httpRequest) {
        Integer teacherId = (Integer) httpRequest.getAttribute(JwtInterceptor.USER_ID_ATTR);
        checkStudentOwnership(teacherId, id);
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("学生不存在");
        }
        if (!"student".equals(user.getRole())) {
            throw new RuntimeException("只能删除学生");
        }
        userCourseMapper.delete(new LambdaQueryWrapper<UserCourse>()
                .eq(UserCourse::getUserId, id));
        userMapper.deleteById(id);
        return Result.success();
    }

    /** 当前教师负责的课程 ID 列表（course.teacher_id 关联，仅有效课程） */
    private List<Integer> getTeacherCourseIds(Integer teacherId) {
        return courseMapper.selectList(new LambdaQueryWrapper<Course>()
                        .eq(Course::getTeacherId, teacherId)
                        .eq(Course::getStatus, 1))
                .stream()
                .map(Course::getId)
                .toList();
    }

    /** 校验学生归属：目标学生必须已加入当前教师负责的某门课程 */
    private void checkStudentOwnership(Integer teacherId, Integer studentId) {
        List<Integer> courseIds = getTeacherCourseIds(teacherId);
        if (courseIds.isEmpty()) {
            throw new RuntimeException("无权管理该学生（不在您负责的课程中）");
        }
        Long count = userCourseMapper.selectCount(new LambdaQueryWrapper<UserCourse>()
                .eq(UserCourse::getUserId, studentId)
                .in(UserCourse::getCourseId, courseIds));
        if (count == null || count == 0) {
            throw new RuntimeException("无权管理该学生（不在您负责的课程中）");
        }
    }
}
