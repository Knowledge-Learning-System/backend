package cn.edu.bcu.learning.domain.vo;

import lombok.Data;
import java.util.List;

/**
 * 教师端-班级学情统计：课程整体进度与掌握度分布。
 */
@Data
public class ClassOverviewVO {

    private Integer courseId;

    private String courseName;

    private Integer studentCount;

    /** 班级平均进度 0-100 */
    private Double avgProgress;

    /** 掌握度分布：优秀（>=80）/良好（60-79）/待加强（<60） */
    private Integer excellentCount;

    private Integer goodCount;

    private Integer weakCount;

    private List<TeacherCourseStudentVO> students;
}
