package cn.edu.bcu.learning.config;

import cn.edu.bcu.learning.annotation.RequireRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RoleInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }

        RequireRole annotation = method.getMethodAnnotation(RequireRole.class);
        if (annotation == null) {
            annotation = method.getBeanType().getAnnotation(RequireRole.class);
        }
        if (annotation == null) {
            return true;
        }

        String requiredRole = annotation.value();
        String userRole = (String) request.getAttribute(JwtInterceptor.USER_ROLE_ATTR);

        // admin 拥有全部角色访问权（可访问 teacher 等角色接口）；admin 专属接口通过 @RequireRole("admin") 与普通教师隔离
        if (!requiredRole.equals(userRole) && !"admin".equals(userRole)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }

        return true;
    }
}
