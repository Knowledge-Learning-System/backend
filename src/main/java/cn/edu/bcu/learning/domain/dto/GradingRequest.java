package cn.edu.bcu.learning.domain.dto;

import lombok.Data;

/**
 * 主观题 AI 判分触发请求。
 * 支持两种维度（二选一）：
 *   - homeworkId：按作业维度批量判分该作业下所有提交
 *   - submissionId：按提交维度判分单条提交
 * questionId 可选：显式指定判分的题目；为空时由服务端按作业课程/知识点自动解析主观题。
 */
@Data
public class GradingRequest {

    /** 作业ID（按作业维度批量判分） */
    private Long homeworkId;

    /** 提交ID（按提交维度判分单条） */
    private Long submissionId;

    /** 可选：指定判分的题目 */
    private Integer questionId;
}
