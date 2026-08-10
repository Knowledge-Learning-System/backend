package cn.edu.bcu.learning.repository.mysql;

import cn.edu.bcu.learning.domain.entity.Discussion;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DiscussionMapper extends BaseMapper<Discussion> {
}
