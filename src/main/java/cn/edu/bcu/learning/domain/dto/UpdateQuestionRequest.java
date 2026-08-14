package cn.edu.bcu.learning.domain.dto;

import lombok.Data;

@Data
public class UpdateQuestionRequest {

    private Integer courseId;

    private String knowledgePointId;

    private String type;

    private String content;

    private String options;

    private String answer;

    private String analysis;
}
