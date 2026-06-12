package cn.edu.bcu.learning.domain.dto;

import lombok.Data;

@Data
public class AddNoteRequest {

    private Integer videoId;

    private String knowledgePointId;

    private Double timestamp;

    private String content;
}
