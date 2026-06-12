package cn.edu.bcu.learning.controller;

import cn.edu.bcu.learning.domain.entity.KnowledgeMastery;
import cn.edu.bcu.learning.domain.vo.KnowledgePointDetailVO;
import cn.edu.bcu.learning.repository.mysql.KnowledgeMasteryMapper;
import cn.edu.bcu.learning.service.KnowledgeGraphService;
import cn.edu.bcu.learning.utils.Result;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/knowledge-point")
@RequiredArgsConstructor
public class KnowledgePointController {

    private final KnowledgeGraphService knowledgeGraphService;
    private final KnowledgeMasteryMapper knowledgeMasteryMapper;

    @GetMapping("/{id}")
    public Result<KnowledgePointDetailVO> getDetail(@PathVariable String id) {
        return Result.success(knowledgeGraphService.getKnowledgePointDetail(id));
    }

    @GetMapping("/{kpId}/prerequisites")
    public Result<List<KnowledgePointDetailVO>> getPrerequisites(@PathVariable String kpId) {
        return Result.success(knowledgeGraphService.getPrerequisites(kpId));
    }

    @PostMapping("/{kpId}/master")
    public Result<?> markMastered(@PathVariable String kpId, HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");

        LambdaQueryWrapper<KnowledgeMastery> wrapper = new LambdaQueryWrapper<KnowledgeMastery>()
                .eq(KnowledgeMastery::getUserId, userId)
                .eq(KnowledgeMastery::getKnowledgePointId, kpId);
        KnowledgeMastery existing = knowledgeMasteryMapper.selectOne(wrapper);

        if (existing != null) {
            existing.setMasteryLevel(100);
            existing.setLastAttemptTime(LocalDateTime.now());
            knowledgeMasteryMapper.updateById(existing);
        } else {
            KnowledgeMastery mastery = new KnowledgeMastery();
            mastery.setUserId(userId);
            mastery.setKnowledgePointId(kpId);
            mastery.setMasteryLevel(100);
            mastery.setLastAttemptTime(LocalDateTime.now());
            knowledgeMasteryMapper.insert(mastery);
        }

        return Result.success();
    }
}
