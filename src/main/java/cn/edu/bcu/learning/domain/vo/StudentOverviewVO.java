package cn.edu.bcu.learning.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 学生端学习统计真实数据 VO（功能3）。
 * 聚合 user_answer_record 答题、knowledge_mastery 掌握度、user_behavior 学习时长、
 * 课程进度（CourseService.calculateCourseProgress）。
 */
@Data
@NoArgsConstructor
public class StudentOverviewVO {

    /** 累计答题数 */
    private Integer totalQuestions;
    /** 总正确率 0-100 */
    private Integer accuracy;
    /** 覆盖知识点数（有答题记录的去重数） */
    private Integer kpCount;
    /** 累计学习时长（秒），来自 user_behavior action=study_time */
    private Integer totalStudySeconds;
    /** 课程进度 0-100（基于知识点掌握度均值，与 CourseService 一致） */
    private Integer courseProgress;
    /** 掌握度分布：已掌握(≥80) / 良好(60-79) / 待加强(<60) */
    private List<NameValue> masteryDistribution;
    /** 近 14 天（含今日）答题趋势：label / total / correct */
    private List<DailyStat> dailyTrend;
    /** 各知识点答题情况 */
    private List<KpStat> kpStats;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NameValue {
        private String name;
        private Integer value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyStat {
        private String label;
        private Integer total;
        private Integer correct;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KpStat {
        private String kpId;
        private String kpName;
        private Integer total;
        private Integer correct;
        private Integer rate;
    }
}
