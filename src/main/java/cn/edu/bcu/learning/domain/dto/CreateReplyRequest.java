package cn.edu.bcu.learning.domain.dto;

import lombok.Data;

@Data
public class CreateReplyRequest {
    private String content;
    private Integer replyToId;
}
