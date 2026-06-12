package cn.edu.bcu.learning.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("video_notes")
public class VideoNote {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer userId;

    private Integer videoId;

    private String knowledgePointId;

    private Double timestamp;

    private String content;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
