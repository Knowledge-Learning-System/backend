package cn.edu.bcu.learning.service;

import cn.edu.bcu.learning.domain.dto.AiChatRequest;
import cn.edu.bcu.learning.domain.entity.AiMessage;
import cn.edu.bcu.learning.domain.vo.AiChatResponseVO;
import cn.edu.bcu.learning.repository.mysql.AiMessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiService {

    @Autowired
    private AiMessageMapper aiMessageMapper;

    /** 发送消息并获取 AI 回复 */
    public AiChatResponseVO chat(Long userId, AiChatRequest request) {
        // TODO: 读取最近 10 条历史上下文
        // TODO: 拼接系统提示词（含课程上下文）
        // TODO: 调用千问 Agent API
        // TODO: 保存问答记录到 ai_message 表
        // TODO: 返回 AiChatResponseVO
        return new AiChatResponseVO();
    }

    /** 获取当前课程的问答历史 */
    public List<AiMessage> getHistory(Long userId, Long courseId) {
        return aiMessageMapper.selectRecentByUser(userId, 50);
    }
}
