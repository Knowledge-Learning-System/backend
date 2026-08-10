package cn.edu.bcu.learning.domain.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class HomeworkVO {

    private Long id;

    private Long courseId;

    private Long knowledgePointId;

    private String title;

    private String description;

    private LocalDateTime deadline;

    private String status;

    private Integer submissionCount;

    private LocalDateTime createTime;
}
