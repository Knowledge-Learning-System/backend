package cn.edu.bcu.learning.controller;

import cn.edu.bcu.learning.domain.dto.AiChatRequest;
import cn.edu.bcu.learning.domain.entity.AiMessage;
import cn.edu.bcu.learning.domain.vo.AiChatResponseVO;
import cn.edu.bcu.learning.service.AiService;
import cn.edu.bcu.learning.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Autowired
    private AiService aiService;

    /** 发送消息并获取 AI 回复 — POST /ai/chat */
    @PostMapping("/chat")
    public Result<AiChatResponseVO> chat(@RequestAttribute Long userId, @RequestBody AiChatRequest request) {
        return Result.success(aiService.chat(userId, request));
    }

    /** 获取当前课程的问答历史 — GET /ai/history?courseId= */
    @GetMapping("/history")
    public Result<List<AiMessage>> getHistory(@RequestAttribute Long userId, @RequestParam Long courseId) {
        return Result.success(aiService.getHistory(userId, courseId));
    }
}
