package cn.edu.bcu.learning.controller;

import cn.edu.bcu.learning.domain.vo.GraphDiagnosisResultVO;
import cn.edu.bcu.learning.service.DiagnosisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/diagnosis/graph")
public class GraphDiagnosisController {

    @Autowired
    private DiagnosisService diagnosisService;

    /** 图传播诊断 — GET /diagnosis/graph/{courseId}?userId= */
    @GetMapping("/{courseId}")
    public GraphDiagnosisResultVO diagnose(@PathVariable Long courseId, @RequestParam Long userId) {
        // TODO: 调用 diagnosisService.diagnoseByGraph(userId, courseId)
        return null;
    }
}
