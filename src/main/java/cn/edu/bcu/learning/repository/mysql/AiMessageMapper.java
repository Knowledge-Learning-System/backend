package cn.edu.bcu.learning.repository.mysql;

import cn.edu.bcu.learning.domain.entity.AiMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface AiMessageMapper extends BaseMapper<AiMessage> {

    @Select("SELECT * FROM ai_message WHERE user_id = #{userId} ORDER BY create_time DESC LIMIT #{limit}")
    List<AiMessage> selectRecentByUser(@Param("userId") Long userId, @Param("limit") int limit);

    @Select("SELECT * FROM ai_message WHERE user_id = #{userId} AND course_id = #{courseId} ORDER BY create_time ASC")
    List<AiMessage> selectByUserAndCourse(@Param("userId") Long userId, @Param("courseId") Long courseId);
}
