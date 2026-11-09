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

    /** view_kp / click_kp / complete_quiz / start_learning / study_time */
    private String action;

    /** 学习停留时长（累计秒），仅 action=study_time 时有效，其余行为默认 0 */
    private Integer durationSeconds;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
