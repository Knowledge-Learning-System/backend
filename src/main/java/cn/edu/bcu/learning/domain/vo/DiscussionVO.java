package cn.edu.bcu.learning.domain.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DiscussionVO {
    private Integer id;
    private Integer courseId;
    private Integer videoId;
    private String knowledgePointId;
    private Integer userId;
    private String username;
    private String nickname;
    private String role;
    private String title;
    private String content;
    private Integer replyCount;
    private LocalDateTime createTime;
    private List<DiscussionReplyVO> replies;
}
