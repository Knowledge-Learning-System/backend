package cn.edu.bcu.learning.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 掌握度时间趋势上的一个数据点（按自然日聚合）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrendPointVO {
    /** 日期 yyyy-MM-dd */
    private String date;
    /** 截至当日的累计掌握度 0-100 */
    private Integer mastery;
    /** 当日答题数 */
    private Integer totalAttempts;
    /** 当日答对数 */
    private Integer correctAttempts;
    /** 当日正确率 0-100（当日无作答时为 0） */
    private Integer accuracy;
}
