package cn.edu.bcu.learning.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("knowledge_mastery")
public class KnowledgeMastery {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer userId;

    private String knowledgePointId;

    private Integer courseId;

    /** 掌握度 0-100 */
    private Integer masteryLevel;

    private Integer totalAttempts;

    private Integer correctAttempts;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime lastAttemptTime;
}
