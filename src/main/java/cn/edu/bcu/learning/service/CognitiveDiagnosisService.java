package cn.edu.bcu.learning.service;

import cn.edu.bcu.learning.domain.entity.UserAnswerRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DINA 认知诊断（简化版）—— 基于答题记录，用 EM 算法估计每个知识点的掌握概率。
 *
 * 模型设定（单知识点题目的 DINA 特例）：
 *   - 每个知识点 k 存在隐变量 α_k ∈ {0,1} 表示是否掌握；
 *   - 掌握时答对概率 = 1 - s（s 为失误率）；
 *   - 未掌握时答对概率 = g（g 为猜测率）；
 *   - 观测似然：P(答对) = π_k(1-s) + (1-π_k)g
 *
 * 估计方法：EM 迭代
 *   - E 步：由当前 π_k 计算每次作答为"掌握"的后验概率
 *   - M 步：用后验期望更新 π_k
 * 相比简单答对率，该估计引入了猜测/失误修正（答对率中的一部分可能来自猜测），
 * 输出 0~1 的掌握概率，可换算为 0~100 掌握度。
 */
@Slf4j
@Service
public class CognitiveDiagnosisService {

    /** 猜测率 g：未掌握知识点时蒙对概率（经验先验，小样本下的固定参数） */
    private static final double GUESS_RATE = 0.20;

    /** 失误率 s：掌握知识点时粗心答错的概率（经验先验） */
    private static final double SLIP_RATE = 0.10;

    /** EM 最大迭代次数 */
    private static final int MAX_ITER = 200;

    /**
     * 对某用户在某课程（source）下的全部答题记录做认知诊断，
     * 返回 知识点id -> 掌握概率(0~1) 的映射。
     */
    public Map<String, Double> estimate(List<UserAnswerRecord> records) {
        Map<String, Integer> correctMap = new HashMap<>();
        Map<String, Integer> totalMap = new HashMap<>();
        for (UserAnswerRecord r : records) {
            if (r.getKnowledgePointId() == null) continue;
            totalMap.merge(r.getKnowledgePointId(), 1, Integer::sum);
            if (Boolean.TRUE.equals(r.getIsCorrect())) {
                correctMap.merge(r.getKnowledgePointId(), 1, Integer::sum);
            }
        }
        Map<String, Double> result = new HashMap<>();
        for (String kpId : totalMap.keySet()) {
            result.put(kpId, estimateOne(correctMap.getOrDefault(kpId, 0), totalMap.get(kpId)));
        }
        return result;
    }

    /**
     * 单知识点掌握概率的 EM 估计。
     *
     * @param correct 答对次数
     * @param total   总答题次数
     * @return 掌握概率 0~1
     */
    private double estimateOne(int correct, int total) {
        if (total <= 0) return 0.0;
        int wrong = total - correct;
        // 初始化：以答对率为起点
        double pi = (double) correct / total;
        for (int it = 0; it < MAX_ITER; it++) {
            // E 步：答对样本的后验掌握概率 P(α=1|X=1)
            double denomC = pi * (1 - SLIP_RATE) + (1 - pi) * GUESS_RATE;
            double gammaCorrect = denomC <= 1e-9 ? pi : (pi * (1 - SLIP_RATE)) / denomC;
            // E 步：答错样本的后验掌握概率 P(α=1|X=0)
            double denomW = pi * SLIP_RATE + (1 - pi) * (1 - GUESS_RATE);
            double gammaWrong = denomW <= 1e-9 ? 0.0 : (pi * SLIP_RATE) / denomW;
            // M 步：更新 π_k
            double newPi = (correct * gammaCorrect + wrong * gammaWrong) / total;
            newPi = Math.max(0.0, Math.min(1.0, newPi));
            if (Math.abs(newPi - pi) < 1e-7) {
                pi = newPi;
                break;
            }
            pi = newPi;
        }
        return pi;
    }
}
