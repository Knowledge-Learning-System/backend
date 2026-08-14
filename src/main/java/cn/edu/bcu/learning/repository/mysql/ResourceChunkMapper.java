package cn.edu.bcu.learning.repository.mysql;

import cn.edu.bcu.learning.domain.entity.ResourceChunk;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ResourceChunkMapper extends BaseMapper<ResourceChunk> {

    @Select("SELECT * FROM resource_chunk WHERE course_id = #{courseId}")
    List<ResourceChunk> selectByCourseId(@Param("courseId") Integer courseId);
}
