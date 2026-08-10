package cn.edu.bcu.learning.domain.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class StudyPlanVO {
    private Integer id;
    private Integer userId;
    private Integer courseId;
    private String courseName;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal dailyHours;
    private LocalDateTime createTime;
}
