package cn.edu.bcu.learning.domain.vo;

import lombok.Data;

@Data
public class NoteVO {

    private Integer id;

    private Integer videoId;

    private String knowledgePointId;

    private Double timestamp;

    private String content;

    private String createTime;

    private String updateTime;
}
