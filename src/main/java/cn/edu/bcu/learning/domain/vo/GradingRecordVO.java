package cn.edu.bcu.learning.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 判分记录查询返回（含诊断推荐详情）。
 */
@Data
public class GradingRecordVO {

    private Integer id;

    private Integer submissionId;

    private Integer userId;

    private Integer questionId;

    private String questionType;

    private String userAnswer;

    private String standardAnswer;

    private Integer autoScore;

    private Integer finalScore;

    private String gradeStatus;

    /** 是否有问题（大模型诊断） */
    private Boolean hasProblem;

    /** 具体问题点 */
    private List<String> problems;

    /** 诊断结论 */
    private String diagnosis;

    /** 学习推荐 */
    private List<String> recommendations;

    /** 维度判分详情 */
    private List<GradingResultVO.DimensionScore> dimensions;

    private String modelName;

    private LocalDateTime createTime;
}
