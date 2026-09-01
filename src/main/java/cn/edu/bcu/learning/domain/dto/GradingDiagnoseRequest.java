package cn.edu.bcu.learning.domain.dto;

import lombok.Data;

/**
 * 大模型诊断推荐请求。
 * 支持两种用法（二选一）：
 *   - submissionId：基于已提交作业判分，自动取作答与参考答案
 *   - userAnswer + standardAnswer + questionId：直接提供作答与参考答案
 */
@Data
public class GradingDiagnoseRequest {

    /** 提交ID（可选，自动读取提交内容与参考答案） */
    private Long submissionId;

    /** 题目ID（可选，用于取参考答案/题目上下文） */
    private Integer questionId;

    /** 用户作答内容（直接提供时使用） */
    private String userAnswer;

    /** 参考答案（直接提供时使用） */
    private String standardAnswer;
}
