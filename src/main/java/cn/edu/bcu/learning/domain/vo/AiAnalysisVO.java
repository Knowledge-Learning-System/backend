package cn.edu.bcu.learning.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 诊断页底部「大模型分析建议」返回体。
 */
@Data
public class AiAnalysisVO {
    /** 总体判断（60-120字） */
    private String overview;
    /** 掌握较好的部分（引用具体知识点名称） */
    private List<String> strengths = new ArrayList<>();
    /** 需要重点关注的薄弱环节 */
    private List<String> weaknesses = new ArrayList<>();
    /** 下一步可执行的学习建议 */
    private List<String> suggestions = new ArrayList<>();
    /** 掌握度变化趋势点评 */
    private String trend;
    /** 是否命中服务端缓存 */
    private Boolean cached = false;
    /** 生成时间 yyyy-MM-dd HH:mm */
    private String generatedAt;
}
