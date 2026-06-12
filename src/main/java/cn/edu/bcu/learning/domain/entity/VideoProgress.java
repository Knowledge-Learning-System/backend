package cn.edu.bcu.learning.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("video_progress")
public class VideoProgress {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer userId;

    private Integer videoId;

    private Double position;

    private Double progress;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
