package cn.edu.bcu.learning.domain.dto;

import lombok.Data;

@Data
public class CreateQuestionRequest {

    private Long courseId;

    private Long knowledgePointId;

    /** single / multiple */
    private String type;

    private String content;

    /** JSON 数组字符串 */
    private String options;

    private String answer;

    private String analysis;
}
