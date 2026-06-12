package cn.edu.bcu.learning.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_behavior")
public class UserBehavior {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer userId;

    private String knowledgePointId;

    /** view_kp / click_kp / complete_quiz / start_learning */
    private String action;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
