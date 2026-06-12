package cn.edu.bcu.learning.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("video_resource")
public class VideoResource {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer courseId;

    private String knowledgePointId;

    private String title;

    private String filePath;

    private Integer duration;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
