package cn.edu.bcu.learning.utils;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 敏感词脱敏组件。
 * 内置政治类、血腥暴力类常见敏感词库，命中后将该词替换为等长 * 号。
 */
@Component
public class SensitiveWordFilter {

    /** 政治类敏感词 */
    private static final List<String> POLITICAL_WORDS = Arrays.asList(
            "法轮功", "台独", "藏独", "疆独", "港独",
            "邪教", "反共", "反华", "颠覆国家", "分裂国家",
            "纳粹", "法西斯", "军国主义", "恐怖分子", "暴恐"
    );

    /** 血腥暴力类敏感词 */
    private static final List<String> VIOLENCE_WORDS = Arrays.asList(
            "血腥", "虐杀", "屠杀", "肢解", "斩首",
            "爆头", "碎尸", "凌迟", "剥皮", "挖眼",
            "割喉", "放血", "血洗", "灭门", "杀人如麻"
    );

    /**
     * 过滤文本：命中敏感词后替换为等长 * 号；输入为 null 或空白时原样返回。
     */
    public String filter(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String result = text;
        for (String word : POLITICAL_WORDS) {
            result = result.replace(word, mask(word));
        }
        for (String word : VIOLENCE_WORDS) {
            result = result.replace(word, mask(word));
        }
        return result;
    }

    private String mask(String word) {
        return "*".repeat(word.length());
    }
}
