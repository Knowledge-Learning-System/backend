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

    /** AI 自动评分 0-100 */
    private Integer autoScore;

    /** 最终得分（人工复核后） */
    private Integer finalScore;

    /** 判分状态：pending / graded / confirmed */
    private String gradeStatus;

    private String feedback;

    private LocalDateTime submitTime;

    private LocalDateTime gradeTime;
}
