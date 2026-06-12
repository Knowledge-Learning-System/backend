package cn.edu.bcu.learning.domain.vo;

import lombok.Data;

@Data
public class QaSearchResultVO {
    private Integer questionId;
    private String content;
    private String answer;
    private String analysis;
}