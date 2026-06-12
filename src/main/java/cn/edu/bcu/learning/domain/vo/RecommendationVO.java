package cn.edu.bcu.learning.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RecommendationVO {
    private String id;
    private String name;
    private String description;
    private String reason;
}
