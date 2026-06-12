package cn.edu.bcu.learning.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReminderVO {
    private String id;
    private String name;
    private Integer errorCount;
    private Integer lastAttemptDaysAgo;
}
