package cn.edu.bcu.learning.domain.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
}
