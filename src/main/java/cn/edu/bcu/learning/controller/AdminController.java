package cn.edu.bcu.learning.controller;

import cn.edu.bcu.learning.annotation.RequireRole;
import cn.edu.bcu.learning.config.JwtInterceptor;
import cn.edu.bcu.learning.service.AdminService;
import cn.edu.bcu.learning.utils.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 管理端接口：仅 admin 角色可访问（RoleInterceptor 按类上 @RequireRole 校验）。
 */
@RestController
@RequestMapping("/admin")
@RequireRole("admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /** 系统数据概览 */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.success(adminService.overview());
    }

    /** 用户管理列表 */
    @GetMapping("/users")
    public Result<Map<String, Object>> users(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        return Result.success(adminService.listUsers(role, keyword, page, size));
    }

    /** 知识图谱总览 */
    @GetMapping("/graph")
    public Result<Map<String, Object>> graph(@RequestParam(required = false) Integer courseId) {
        return Result.success(adminService.graphOverview(courseId));
    }

    /** 资源管理列表 */
    @GetMapping("/resources")
    public Result<Map<String, Object>> resources(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer courseId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        return Result.success(adminService.listResources(type, courseId, keyword, page, size));
    }

    /** 学情分析 */
    @GetMapping("/analysis")
    public Result<Map<String, Object>> analysis() {
        return Result.success(adminService.learningAnalysis());
    }

    /** 系统信息 */
    @GetMapping("/system")
    public Result<Map<String, Object>> system() {
        return Result.success(adminService.systemInfo());
    }

    /** 推荐策略配置读取 */
    @GetMapping("/strategy")
    public Result<Map<String, Object>> strategy() {
        return Result.success(adminService.recommendStrategy());
    }

    /** 推荐策略配置保存 */
    @PutMapping("/strategy")
    @SuppressWarnings("unchecked")
    public Result<Void> updateStrategy(@RequestBody Map<String, Object> body) {
        try {
            Object items = body == null ? null : body.get("items");
            adminService.updateRecommendStrategy((List<Map<String, Object>>) items);
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 课程总览 */
    @GetMapping("/courses")
    public Result<List<Map<String, Object>>> courses() {
        return Result.success(adminService.listCourses());
    }

    /** 重置用户密码 */
    @PutMapping("/users/{id}/password")
    public Result<Void> resetPassword(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        try {
            adminService.resetPassword(id, body == null ? null : body.get("password"));
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 调整用户角色 */
    @PutMapping("/users/{id}/role")
    public Result<Void> updateRole(@PathVariable Integer id,
                                   @RequestBody Map<String, String> body,
                                   HttpServletRequest request) {
        try {
            Integer operatorId = (Integer) request.getAttribute(JwtInterceptor.USER_ID_ATTR);
            adminService.updateRole(id, body == null ? null : body.get("role"), operatorId);
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }
}
