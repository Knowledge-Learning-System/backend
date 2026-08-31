package cn.edu.bcu.learning.repository.mysql;

import cn.edu.bcu.learning.domain.entity.DailyStudyRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DailyStudyRecordMapper extends BaseMapper<DailyStudyRecord> {
}
