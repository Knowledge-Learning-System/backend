package cn.edu.bcu.learning.domain.vo;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class SubTopicVO {
    private String id;
    private String name;
    private String description;
    private int order;
    private List<KnowledgePointTreeNodeVO> knowledgePoints = new ArrayList<>();
}
