package cn.edu.bcu.learning.repository.mysql;

import cn.edu.bcu.learning.domain.entity.UserBehavior;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserBehaviorMapper extends BaseMapper<UserBehavior> {

    @Select("SELECT COUNT(DISTINCT DATE(create_time)) FROM user_behavior WHERE user_id = #{userId}")
    int countActiveDays(Integer userId);
}
