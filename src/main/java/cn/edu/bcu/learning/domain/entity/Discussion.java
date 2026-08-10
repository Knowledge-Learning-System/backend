package cn.edu.bcu.learning.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("discussion")
public class Discussion {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer courseId;

    private Integer videoId;

    private String knowledgePointId;

    private Integer userId;

    private String title;

    private String content;

    private Integer replyCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
