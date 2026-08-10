package cn.edu.bcu.learning.domain.vo;

import lombok.Data;

@Data
public class RiskNodeVO {

    private Long kpId;

    private String kpName;

    /** high / medium / low */
    private String riskLevel;

    private String reason;
}
