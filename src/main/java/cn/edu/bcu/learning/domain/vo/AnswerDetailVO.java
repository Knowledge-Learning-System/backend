package cn.edu.bcu.learning.domain.vo;

import lombok.Data;

@Data
public class AnswerDetailVO {
    private Integer id;
    private String type;
    private String content;
    private String options;
    private String answer;
    private String analysis;
    private String knowledgePointId;
}
