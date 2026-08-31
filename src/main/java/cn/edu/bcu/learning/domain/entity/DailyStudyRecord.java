package cn.edu.bcu.learning.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("daily_study_record")
public class DailyStudyRecord {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private Integer courseId;
    private String knowledgePointId;
    private LocalDate studyDate;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
