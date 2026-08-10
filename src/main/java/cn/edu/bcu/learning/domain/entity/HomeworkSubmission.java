package cn.edu.bcu.learning.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("homework_submission")
public class HomeworkSubmission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long homeworkId;

    private Long userId;

    private String content;

    /** 附件路径 JSON */
    private String attachments;

    private Integer score;

    private String feedback;

    private LocalDateTime submitTime;

    private LocalDateTime gradeTime;
}
