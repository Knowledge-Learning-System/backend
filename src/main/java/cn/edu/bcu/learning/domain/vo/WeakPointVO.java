package cn.edu.bcu.learning.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WeakPointVO {
    private String id;
    private String name;
    private Integer mastery;
    private Integer totalAttempts;
    private Integer errorCount;
}
