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

        if (!requiredRole.equals(userRole)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }

        return true;
    }
}
