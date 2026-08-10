package cn.edu.bcu.learning.domain.vo;

import lombok.Data;

@Data
public class CommonMistakeVO {

    private Long questionId;

    private String questionContent;

    /** 错误率百分比，如 67.5 */
    private Double errorRate;
}
