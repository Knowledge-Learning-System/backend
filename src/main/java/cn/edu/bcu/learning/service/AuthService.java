package cn.edu.bcu.learning.service;

import cn.edu.bcu.learning.domain.dto.LoginRequest;
import cn.edu.bcu.learning.domain.dto.RegisterRequest;
import cn.edu.bcu.learning.domain.entity.User;
import cn.edu.bcu.learning.domain.vo.LoginVO;
import cn.edu.bcu.learning.domain.vo.UserVO;
import cn.edu.bcu.learning.repository.mysql.UserMapper;
import cn.edu.bcu.learning.utils.JwtUtil;
import cn.edu.bcu.learning.utils.UserConverter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public void register(RegisterRequest request) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (count != null && count > 0) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole() != null && !request.getRole().isEmpty()
                ? request.getRole() : "student");
        userMapper.insert(user);
    }

    public LoginVO login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole()));
        loginVO.setUser(UserConverter.toVO(user));
        return loginVO;
    }

    public UserVO getCurrentUser(Integer userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return UserConverter.toVO(user);
    }
}
