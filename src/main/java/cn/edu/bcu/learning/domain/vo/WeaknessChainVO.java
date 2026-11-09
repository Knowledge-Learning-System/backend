package cn.edu.bcu.learning.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 学生端薄弱点"原因分析"展示 VO。
 * chainNames 为该薄弱知识点向图谱上游溯源得到的缺陷链名称序列（含薄弱点自身）：
 * 例：["集成测试", "系统设计"] 表示"集成测试"掌握薄弱，源于前置"系统设计"也未掌握。
 */
@Data
@AllArgsConstructor
public class WeaknessChainVO {
    private String id;
    private String name;
    private Integer mastery;
    private List<String> chainNames;
}
