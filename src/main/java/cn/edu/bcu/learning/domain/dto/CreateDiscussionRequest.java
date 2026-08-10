package cn.edu.bcu.learning.domain.dto;

import lombok.Data;

@Data
public class CreateDiscussionRequest {
    private Integer courseId;
    private Integer videoId;
    private String knowledgePointId;
    private String title;
    private String content;
}
