package cn.edu.bcu.learning.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class KnowledgeGraphVO {
    private List<GraphNodeVO> nodes = new ArrayList<>();
    private List<GraphLinkVO> links = new ArrayList<>();
}
