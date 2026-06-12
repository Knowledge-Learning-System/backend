package cn.edu.bcu.learning.repository.mysql;

import cn.edu.bcu.learning.domain.entity.VideoNote;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VideoNoteMapper extends BaseMapper<VideoNote> {
}
