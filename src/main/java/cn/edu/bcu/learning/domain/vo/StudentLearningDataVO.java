package cn.edu.bcu.learning.domain.vo;

import lombok.Data;
import java.util.List;

/**
 * 教师端-学生学习数据：按时间维度聚合的答题、学习行为与掌握度趋势。
 * 注：系统未采集真实"学习时长"，以 user_behavior 行为次数作为学习活跃度近似。
 */
@Data
public class StudentLearningDataVO {

    private Integer courseId;

    private Integer studentId;

    private String studentName;

    /** 累计答题数 */
    private Integer totalAnswers;

    /** 累计答对数 */
    private Integer correctAnswers;

    /** 总体正确率 0-100 */
    private Double accuracy;

    /** 学习行为次数（view_kp/click_kp/complete_quiz/start_learning 等，近似学习活跃度） */
    private Integer behaviorCount;

    /** 已掌握知识点数（掌握度>=60） */
    private Integer masteredKpCount;

    /** 课程知识点总数 */
    private Integer totalKpCount;

    /** 平均掌握度 0-100 */
    private Double avgMastery;

    /** 每日答题/行为趋势 */
    private List<DailyTrendItem> dailyTrend;

    /** 掌握度趋势（按答题记录累计正确率计算） */
    private List<MasteryTrendItem> masteryTrend;

    @Data
    public static class DailyTrendItem {
        private String date;
        private Integer answerCount;
        /** 当日正确率 0-100 */
        private Double accuracy;
        private Integer behaviorCount;
    }

    @Data
    public static class MasteryTrendItem {
        private String date;
        /** 截至该日累计掌握度 0-100 */
        private Double avgMastery;
    }
}
