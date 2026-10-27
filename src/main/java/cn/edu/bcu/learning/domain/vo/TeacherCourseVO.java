package cn.edu.bcu.learning.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class TeacherCourseVO {
    private Integer teacherId;
    private String teacherUsername;
    private String teacherName;
    private List<TeacherCourseItemVO> courses;
}
