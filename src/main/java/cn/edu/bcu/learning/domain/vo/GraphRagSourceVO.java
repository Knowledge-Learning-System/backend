package cn.edu.bcu.learning.domain.vo;

import lombok.Data;

/**
 * GraphRAG 回答的知识溯源来源（对应前端 QASource）
 */
@Data
public class GraphRagSourceVO {
    /** 知识点 ID（Neo4j 节点 id） */
    private String id;
    /** 知识点名称 */
    private String name;
    /** 所属章节（KnowledgeArea / SubTopic） */
    private String chapter;
    /** 该知识点在回答中的角色（如：命中知识点/前置知识） */
    private String relation;
    /** 匹配分数（0~100） */
    private Double score;
}
