package cn.edu.bcu.learning.domain.vo;

import lombok.Data;
import java.util.List;

/**
 * 教师端-学生诊断报告：雷达图 + 薄弱点 + 学习建议。
 * 复用 DiagnosisService.getRadar / getWeakPoints（基于 DINA 认知诊断）。
 */
@Data
public class StudentDiagnosisVO {

    private Integer courseId;

    private Integer studentId;

    private String studentName;

    /** 综合评分 0-100（雷达各知识点掌握度均值） */
    private Double overallScore;

    /** 雷达图数据 */
    private List<RadarItemVO> radar;

    /** 薄弱点列表 */
    private List<WeakPointVO> weakPoints;

    /** 学习建议 */
    private List<String> suggestions;
}
