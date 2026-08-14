package cn.edu.bcu.learning.domain.vo;

import lombok.Data;

@Data
public class UserVO {
    private Integer id;
    private String username;
    private String role;
    private String avatar;
    private String nickname;
    private Integer currentCourseId;
}
