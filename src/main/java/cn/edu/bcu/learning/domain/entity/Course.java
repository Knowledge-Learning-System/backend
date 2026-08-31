package cn.edu.bcu.learning.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("course")
public class Course {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String name;

    private String description;

    private String cover;

    private String source;

    private Integer status;

    /** 课程编号，格式 cs1001、cs1002...自动递增 */
    private String courseCode;

    /** 负责教师ID（课程分配给指定教师，仅该教师可上传学习资源） */
    private Integer teacherId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}