package cn.edu.bcu.learning.domain.vo;

import lombok.Data;
import java.util.List;

@Data
public class AiChatResponseVO {

    private String reply;

    private List<String> sources;
}
