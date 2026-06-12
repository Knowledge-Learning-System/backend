package cn.edu.bcu.learning.domain.vo;

import lombok.Data;

@Data
public class SubmitAnswerResultVO {
    /** 总分 */
    private Integer totalQuestions;
    /** 正确数 */
    private Integer correctCount;
    /** 每题判分详情 */
    private java.util.List<ItemResult> items;

    @Data
    public static class ItemResult {
        private Integer questionId;
        private Boolean isCorrect;
        private String correctAnswer;
        private String analysis;
    }
}
