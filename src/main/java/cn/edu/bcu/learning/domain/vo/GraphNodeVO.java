package cn.edu.bcu.learning.domain.vo;

import lombok.Data;

@Data
public class GraphNodeVO {
    private String id;
    private String name;
    private String courseId;
    private Integer group;
    /** 在 PARENT_KP 树中的层级深度（0 为根） */
    private Integer level;
}
