package cn.edu.bcu.learning.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 知识图谱缺陷溯源结果。
 * chain 为该薄弱知识点向上追溯得到的所有未掌握祖先先修节点 id 序列。
 */
@Data
@AllArgsConstructor
public class WeaknessTraceVO {
    private String id;
    private String name;
    private Integer mastery;
    private List<String> chain;
}
