package cn.edu.bcu.learning.domain.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * GraphRAG 问答请求体（问答功能专用，不影响 /ai/chat 对话）
 */
@Data
public class GraphRagRequest {

    /** 用户问题 */
    private String question;

    /** 对话历史（role: user/assistant，content: 文本） */
    private List<HistoryMsg> history = new ArrayList<>();

    /** 课程 ID（可选；为空时后端自动识别课程） */
    private Integer courseId;

    @Data
    public static class HistoryMsg {
        private String role;
        private String content;
    }
}
