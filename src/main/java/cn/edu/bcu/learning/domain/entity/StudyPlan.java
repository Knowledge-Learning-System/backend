package cn.edu.bcu.learning.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("study_plan")
public class StudyPlan {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer userId;

    private Integer courseId;

    private LocalDate startDate;

    private LocalDate endDate;

    /** 每日学习小时数 */
    private java.math.BigDecimal dailyHours;

    /** 每日学习目标（分钟） */
    private Integer dailyTarget;

    /** 每日提醒时间 HH:mm */
    private String remindTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
