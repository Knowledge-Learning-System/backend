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
}
