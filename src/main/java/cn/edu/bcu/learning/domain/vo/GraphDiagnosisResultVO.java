package cn.edu.bcu.learning.domain.vo;

import lombok.Data;
import java.util.List;

@Data
public class GraphDiagnosisResultVO {

    private Long courseId;

    private List<RiskNodeVO> riskNodes;

    private List<String> impactPaths;
}
