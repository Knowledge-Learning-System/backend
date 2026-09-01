package cn.edu.bcu.learning.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI 判分记录（主观题逐次判分结果）
 * 对应表 ai_grading_record（v6_add_diagnosis_module.sql）
 */
@Data
@TableName("ai_grading_record")
public class AiGradingRecord {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 作业提交ID（关联 homework_submission.id） */
    private Integer submissionId;

    /** 用户ID */
    private Integer userId;

    /** 试题ID（关联 question.id） */
    private Integer questionId;

    /** 题型：subjective / objective */
    private String questionType;

    /** 用户作答内容 */
    private String userAnswer;

    /** 参考答案（来自 question.answer_text） */
    private String standardAnswer;

    /** AI 自动评分 0-100 */
    private Integer autoScore;

    /** 最终得分（人工复核后） */
    private Integer finalScore;

    /** 判分状态：pending / graded / confirmed */
    private String gradeStatus;

    /** AI 判分详情（维度得分、诊断与推荐，JSON 字符串） */
    private String gradingDetail;

    /** 判分模型标识 */
    private String modelName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
