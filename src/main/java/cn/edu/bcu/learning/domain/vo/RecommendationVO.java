package cn.edu.bcu.learning.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
@AllArgsConstructor
public class RecommendationVO {
    private String id;
    private String name;
    private String description;
    private String reason;
    /** 前置知识点名称列表 */
    private List<String> prerequisites;
    /** 当前掌握度 0-100 */
    private int masteryLevel;

    public RecommendationVO(String id, String name, String description, String reason) {
        this(id, name, description, reason, Collections.emptyList(), 0);
    }
}
