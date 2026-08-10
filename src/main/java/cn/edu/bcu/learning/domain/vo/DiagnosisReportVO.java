package cn.edu.bcu.learning.domain.vo;

import lombok.Data;
import java.util.List;

@Data
public class DiagnosisReportVO {

    private Long userId;

    private Long courseId;

    /** 综合评分 0-100 */
    private Double overallScore;

    /** 雷达图数据 */
    private List<RadarItemVO> radarData;

    /** 薄弱点 Top5 */
    private List<WeakPointItemVO> weakPoints;

    /** 强项 Top3 */
    private List<StrengthItemVO> strengths;

    /** 学习行为摘要 */
    private BehaviorSummary behaviorSummary;

    @Data
    public static class RadarItemVO {
        private String name;
        private Double value;
    }

    @Data
    public static class WeakPointItemVO {
        private Long knowledgePointId;
        private String knowledgePointName;
        private Double score;
        private String suggestion;
    }

    @Data
    public static class StrengthItemVO {
        private Long knowledgePointId;
        private String knowledgePointName;
        private Double score;
    }

    @Data
    public static class BehaviorSummary {
        private Double videoWatchRatio;
        private List<DailyDuration> dailyDurations;
    }

    @Data
    public static class DailyDuration {
        private String date;
        private Integer minutes;
    }
}
