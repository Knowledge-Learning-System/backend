package cn.edu.bcu.learning.domain.vo;

import lombok.Data;

@Data
public class VideoDetailVO {

    private Integer id;

    private Integer courseId;

    private String knowledgePointId;

    private String title;

    private String filePath;

    private String streamUrl;

    private Integer duration;

    private String createTime;
}
