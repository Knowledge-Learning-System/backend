package cn.edu.bcu.learning.domain.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class HomeworkSubmissionVO {

    private Long id;

    private Long homeworkId;

    private Long userId;

    private String username;

    private String content;

    private String attachments;

    private Integer score;

    private String feedback;

    private LocalDateTime submitTime;
}
