package cn.edu.bcu.learning.domain.vo;

import lombok.Data;

@Data
public class LoginVO {
    private String token;
    private UserVO user;
}
