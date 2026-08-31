package cn.edu.bcu.learning.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * GraphRAG 问答响应：答案 + 知识溯源 + 过程说明
 */
@Data
public class GraphRagResponseVO {

    /** 最终回答（Markdown） */
    private String answer;

    /** 引用的知识点来源列表 */
    private List<GraphRagSourceVO> sources = new ArrayList<>();

    /** 检索过程说明（调试/展示用） */
    private String thinking;

    /** 构建的完整 Prompt（调试用） */
    private String promptBuilt;
}
