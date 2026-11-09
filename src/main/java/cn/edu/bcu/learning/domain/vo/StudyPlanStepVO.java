package cn.edu.bcu.learning.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 完整学习计划中的单个学习步骤。
 * 顺序由知识图谱 PREREQUISITE 依赖的拓扑排序决定；
 * 完成状态由 knowledge_mastery.mastery_level >= 60 判定。
 */
@Data
public class StudyPlanStepVO {

    /** 学习顺序序号（从 1 开始） */
    private int order;

    /** 知识点 ID（Neo4j 节点 id） */
    private String kpId;

    /** 知识点名称 */
    private String name;

    /** 知识点描述 */
    private String description;

    /** 图谱层级（group 越小越基础，越应先学习） */
    private int group;

    /** 当前掌握度 0-100 */
    private int mastery;

    /** 是否已完成（掌握度 >= 60） */
    private boolean completed;

    /** 前置知识点 ID 列表 */
    private List<String> prerequisites;

    /** 前置知识点名称列表（用于前端展示依赖说明） */
    private List<String> prerequisiteNames;
}
