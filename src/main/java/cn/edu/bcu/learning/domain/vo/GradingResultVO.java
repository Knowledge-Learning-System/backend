package cn.edu.bcu.learning.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 主观题判分结果（判分 + 大模型诊断推荐一体返回）。
 */
@Data
public class GradingResultVO {

    /** 判分记录ID（ai_grading_record.id） */
    private Integer recordId;

    /** 作业提交ID */
    private Integer submissionId;

    /** 用户ID */
    private Integer userId;

    /** 题目ID */
    private Integer questionId;

    /** 题型 */
    private String questionType;

    /** 用户作答 */
    private String userAnswer;

    /** 参考答案 */
    private String standardAnswer;

    /** AI 自动评分 0-100 */
    private Integer autoScore;

    /** 最终得分 */
    private Integer finalScore;

    /** 判分状态 */
    private String gradeStatus;

    /** 是否有问题（大模型诊断） */
    private Boolean hasProblem;

    /** 具体问题点（大模型诊断） */
    private List<String> problems;

    /** 诊断结论 */
    private String diagnosis;

    /** 学习推荐 */
    private List<String> recommendations;

    /** 维度判分详情 */
    private List<DimensionScore> dimensions;

    /** 判分模型 */
    private String modelName;

    private LocalDateTime createTime;

    @Data
    public static class DimensionScore {
        private String name;
        private Integer score;
        private String comment;
    }
}
