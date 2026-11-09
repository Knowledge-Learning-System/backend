package cn.edu.bcu.learning.domain.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 教师端-选课学生列表项：某课程下的学生及学习进度。
 */
@Data
public class TeacherCourseStudentVO {

    private Integer studentId;

    private String username;

    private String nickname;

    /** 动态计算的学习进度 0-100 */
    private Integer progress;

    /** 加入课程时间 */
    private LocalDateTime selectedAt;
}
