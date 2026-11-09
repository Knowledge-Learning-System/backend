package cn.edu.bcu.learning.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 完整学习计划：按知识图谱 PREREQUISITE 依赖拓扑序生成的全部学习步骤，
 * 含总进度与当前完成状态。
 */
@Data
public class FullStudyPlanVO {

    private Integer courseId;

    private String courseName;

    /** 计划总步骤数 */
    private int totalSteps;

    /** 已完成步骤数 */
    private int completedSteps;

    /** 计划完成率 0-100 */
    private int completionRate;

    /** 预计学习天数（按每天 3 个知识点估算） */
    private int estimatedDays;

    /** 按拓扑序排列的学习步骤 */
    private List<StudyPlanStepVO> steps;
}
