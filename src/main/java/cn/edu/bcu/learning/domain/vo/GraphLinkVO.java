package cn.edu.bcu.learning.domain.vo;

import lombok.Data;

@Data
public class GraphLinkVO {
    private String source;
    private String target;
    private String type;
}
