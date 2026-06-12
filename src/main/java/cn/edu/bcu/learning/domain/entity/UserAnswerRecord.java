package cn.edu.bcu.learning.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_answer_record")
public class UserAnswerRecord {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer userId;

    private Integer questionId;

    private String knowledgePointId;

    private String userAnswer;

    private Boolean isCorrect;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
