package cn.edu.bcu.learning.controller;

import cn.edu.bcu.learning.config.JwtInterceptor;
import cn.edu.bcu.learning.domain.dto.LoginRequest;
import cn.edu.bcu.learning.domain.dto.RegisterRequest;
import cn.edu.bcu.learning.domain.vo.LoginVO;
import cn.edu.bcu.learning.domain.vo.UserVO;
import cn.edu.bcu.learning.service.AuthService;
import cn.edu.bcu.learning.utils.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Result<Void> register(@RequestBody RegisterRequest request) {
        authService.register(request);
        return Result.success();
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @GetMapping("/me")
    public Result<UserVO> me(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute(JwtInterceptor.USER_ID_ATTR);
        return Result.success(authService.getCurrentUser(userId));
    }
}
