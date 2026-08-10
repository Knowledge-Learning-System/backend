package cn.edu.bcu.learning.domain.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DiscussionReplyVO {
    private Integer id;
    private Integer discussionId;
    private Integer userId;
    private String username;
    private Integer replyToId;
    private String replyToUsername;
    private String content;
    private LocalDateTime createTime;
}
