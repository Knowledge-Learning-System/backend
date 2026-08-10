package cn.edu.bcu.learning.domain.dto;

import lombok.Data;

@Data
public class VideoUploadRequest {

    private Long courseId;

    private Long knowledgePointId;

    private String title;

    private String description;
}
