package cn.edu.bcu.learning.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class LearningPathItemVO {
    private String id;
    private String name;
    private String description;
    private int order;
    private List<String> prerequisites;
    private String courseId;
    private int group;
}
