package cn.edu.bcu.learning.domain.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CreateHomeworkRequest {

    private Long courseId;

    private Long knowledgePointId;

    private String title;

    private String description;

    private LocalDateTime deadline;
}
