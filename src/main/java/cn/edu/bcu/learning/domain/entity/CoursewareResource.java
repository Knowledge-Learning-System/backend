package cn.edu.bcu.learning.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("courseware_resource")
public class CoursewareResource {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer courseId;

    private String knowledgePointId;

    private String title;

    private String filePath;

    private String fileType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
