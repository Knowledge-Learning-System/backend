package cn.edu.bcu.learning.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RadarItemVO {
    /** 知识点id */
    private String id;
    /** 知识点名称 */
    private String name;
    /** 掌握度 0-100 */
    private Integer mastery;
    /** RSM 式等级评定：未掌握 / 初步掌握 / 熟练 / 精通 */
    private String level;

    public RadarItemVO(String id, String name, Integer mastery) {
        this(id, name, mastery, null);
    }
}
