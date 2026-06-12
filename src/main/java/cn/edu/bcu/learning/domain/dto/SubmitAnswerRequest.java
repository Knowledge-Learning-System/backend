package cn.edu.bcu.learning.domain.dto;

import lombok.Data;
import java.util.List;

@Data
public class SubmitAnswerRequest {

    private Integer userId;

    private Integer courseId;

    /** 批量提交 */
    private List<AnswerItem> answers;

    @Data
    public static class AnswerItem {
        private Integer questionId;
        private String knowledgePointId;
        private String answer;
    }
}
