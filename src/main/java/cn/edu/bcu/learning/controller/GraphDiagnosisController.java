package cn.edu.bcu.learning.controller;

import cn.edu.bcu.learning.domain.vo.GraphDiagnosisResultVO;
import cn.edu.bcu.learning.domain.vo.KnowledgePointDetailVO;
import cn.edu.bcu.learning.domain.vo.WeaknessChainVO;
import cn.edu.bcu.learning.domain.vo.WeaknessTraceVO;
import cn.edu.bcu.learning.repository.neo4j.KnowledgeGraphRepository;
import cn.edu.bcu.learning.service.AnalysisAlgorithmService;
import cn.edu.bcu.learning.service.DiagnosisService;
import cn.edu.bcu.learning.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/diagnosis/graph")
public class GraphDiagnosisController {

    @Autowired
    private DiagnosisService diagnosisService;

    @Autowired
    private AnalysisAlgorithmService analysisAlgorithmService;

    @Autowired
    private KnowledgeGraphRepository knowledgeGraphRepository;

    /** 图传播诊断 — GET /diagnosis/graph/{courseId}?userId= */
    @GetMapping("/{courseId}")
    public GraphDiagnosisResultVO diagnose(@PathVariable Long courseId, @RequestAttribute Long userId) {
        // TODO: 调用 diagnosisService.diagnoseByGraph(userId, courseId)
        return null;
    }

    /**
     * 薄弱点原因分析（功能2-接通图谱缺陷链溯源）
     * 复用 AnalysisAlgorithmService.traceWeaknessByGraph 已有实现，将缺陷链 id 转为名称序列返回；
     * 仅接通薄弱点溯源链路，不改动其他 TODO 接口。
     * GET /diagnosis/graph/weakness/trace?userId=xxx&courseId=xxx
     */
    @GetMapping("/weakness/trace")
    public Result<List<WeaknessChainVO>> traceWeakness(@RequestAttribute Long userId, @RequestParam Long courseId) {
        List<WeaknessTraceVO> traces = analysisAlgorithmService.traceWeaknessByGraph(userId, courseId);
        List<WeaknessChainVO> result = new ArrayList<>();
        for (WeaknessTraceVO t : traces) {
            List<String> chainNames = new ArrayList<>();
            for (String id : t.getChain()) {
                chainNames.add(knowledgeGraphRepository.findDetailById(id)
                        .map(KnowledgePointDetailVO::getName).orElse(id));
            }
            result.add(new WeaknessChainVO(t.getId(), t.getName(), t.getMastery(), chainNames));
        }
        return Result.success(result);
    }
}
