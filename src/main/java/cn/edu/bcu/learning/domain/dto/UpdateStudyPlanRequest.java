package cn.edu.bcu.learning.domain.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdateStudyPlanRequest {
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal dailyHours;
}
