package cn.edu.bcu.learning.domain.dto;

import lombok.Data;

@Data
public class UpdateQuestionRequest {

    private Long courseId;

    private Long knowledgePointId;

    private String type;

    private String content;

    private String options;

    private String answer;

    private String analysis;
}
