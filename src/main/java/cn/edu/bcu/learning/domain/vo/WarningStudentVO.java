package cn.edu.bcu.learning.domain.vo;

import lombok.Data;
import java.util.List;

/**
 * 教师端-预警学生：进度滞后或掌握度显著偏低的学生。
 */
@Data
public class WarningStudentVO {

    private Integer studentId;

    private String username;

    private String nickname;

    /** 动态学习进度 0-100 */
    private Integer progress;

    /** 薄弱知识点数量（掌握度<60） */
    private Integer weakPointCount;

    /** 预警原因列表 */
    private List<String> reasons;
}
