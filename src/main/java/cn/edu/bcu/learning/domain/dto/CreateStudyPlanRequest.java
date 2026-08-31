package cn.edu.bcu.learning.domain.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateStudyPlanRequest {
    private Integer courseId;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal dailyHours;
    /** 每日学习目标（分钟） */
    private Integer dailyTarget;
    /** 每日提醒时间 HH:mm */
    private String remindTime;
}
