package cn.edu.bcu.learning.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("discussion_reply")
public class DiscussionReply {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer discussionId;

    private Integer userId;

    private Integer replyToId;

    private String content;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
