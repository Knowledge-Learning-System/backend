package cn.edu.bcu.learning.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("question")
public class Question {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer courseId;

    private String knowledgePointId;

    /** single / multiple */
    private String type;

    /** 题型细分：choice / judge / fill / subjective */
    private String questionType;

    private String content;

    /** JSON: ["A.xxx","B.xxx","C.xxx","D.xxx"] */
    private String options;

    /** 正确答案，如 "A" */
    private String answer;

    /** 参考答案（主观题使用，客观题可为空） */
    private String answerText;

    private String analysis;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
