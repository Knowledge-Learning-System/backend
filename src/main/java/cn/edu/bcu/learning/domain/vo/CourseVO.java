package cn.edu.bcu.learning.domain.vo;

import lombok.Data;

@Data
public class CourseVO {
    private Integer id;
    private String name;
    private String description;
    private String cover;
    private Integer status;
}
