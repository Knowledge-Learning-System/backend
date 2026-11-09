package cn.edu.bcu.learning.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class RecommendationVO {
    private String id;
    private String name;
    private String description;
    private String reason;
    /** 前置知识点名称列表 */
    private List<String> prerequisites;
    /** 当前掌握度 0-100 */
    private int masteryLevel;
    /** PC-TOP 覆盖度因子：学完该知识点可解锁的后继知识点数量 */
    private int unlockCount;
    /** PC-TOP 时序衰减因子：距上次练习天数，-1 表示从未练习 */
    private int daysSinceLastStudy;
    /** RSM 等级：未掌握 / 初步掌握 / 熟练 / 精通 */
    private String level;

    public RecommendationVO(String id, String name, String description, String reason) {
        this(id, name, description, reason, Collections.emptyList(), 0);
    }

    public RecommendationVO(String id, String name, String description, String reason,
                            List<String> prerequisites, int masteryLevel) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.reason = reason;
        this.prerequisites = prerequisites == null ? Collections.emptyList() : prerequisites;
        this.masteryLevel = masteryLevel;
        this.unlockCount = 0;
        this.daysSinceLastStudy = -1;
        this.level = "未掌握";
    }
}
