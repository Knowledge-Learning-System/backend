package cn.edu.bcu.learning.utils;

import cn.edu.bcu.learning.domain.entity.User;
import cn.edu.bcu.learning.domain.vo.UserVO;

public final class UserConverter {

    private UserConverter() {
    }

    public static UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRole(user.getRole());
        vo.setAvatar(user.getAvatar());
        vo.setNickname(user.getNickname());
        vo.setCurrentCourseId(user.getCurrentCourseId());
        return vo;
    }
}
