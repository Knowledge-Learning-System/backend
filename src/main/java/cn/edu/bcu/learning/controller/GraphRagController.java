package cn.edu.bcu.learning.controller;

import cn.edu.bcu.learning.domain.dto.GraphRagRequest;
import cn.edu.bcu.learning.domain.vo.GraphRagResponseVO;
import cn.edu.bcu.learning.service.GraphRagService;
import cn.edu.bcu.learning.utils.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GraphRAG 问答接口（导航栏「问答」功能，独立于 /ai/chat 对话）
 */
@RestController
@RequestMapping("/qa")
@RequiredArgsConstructor
public class GraphRagController {

    private final GraphRagService graphRagService;

    @PostMapping("/graphrag")
    public Result<GraphRagResponseVO> graphrag(@RequestBody GraphRagRequest request) {
        return Result.success(graphRagService.chat(request));
    }
}
