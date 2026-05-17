package cn.edu.bcu.learning.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_course")
public class UserCourse {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer userId;

    private Integer courseId;

    private Integer progress;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime selectedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}