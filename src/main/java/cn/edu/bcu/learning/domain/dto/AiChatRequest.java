package cn.edu.bcu.learning.domain.dto;

import lombok.Data;

@Data
public class AiChatRequest {

    private Long courseId;

    private Long knowledgePointId; // 可选

    private String message;
}
