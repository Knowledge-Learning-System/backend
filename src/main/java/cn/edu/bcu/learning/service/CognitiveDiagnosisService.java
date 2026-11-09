package cn.edu.bcu.learning.service;

import cn.edu.bcu.learning.domain.entity.UserAnswerRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
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

    /** 猜测率 g 的经验先验（样本不足时的兜底值） */
    private static final double PRIOR_GUESS_RATE = 0.20;

    /** 失误率 s 的经验先验（样本不足时的兜底值） */
    private static final double PRIOR_SLIP_RATE = 0.10;

    /** 参与题目参数估计所需的最小知识点数 */
    private static final int MIN_KP_FOR_ESTIMATION = 4;

    /** 参与题目参数估计所需的最小答题记录数 */
    private static final int MIN_RECORDS_FOR_ESTIMATION = 20;

    /** 猜测率 g 的合理取值区间 */
    private static final double MIN_GUESS = 0.05;
    private static final double MAX_GUESS = 0.40;

    /** 失误率 s 的合理取值区间 */
    private static final double MIN_SLIP = 0.02;
    private static final double MAX_SLIP = 0.30;

    /** EM 最大迭代次数 */
    private static final int MAX_ITER = 200;

    /** RSM 等级评定阈值：θ < 0.40 未掌握；[0.40, 0.60) 初步掌握；[0.60, 0.85) 熟练；≥ 0.85 精通 */
    private static final double LEVEL_LOW = 0.40;
    private static final double LEVEL_MID = 0.60;
    private static final double LEVEL_HIGH = 0.85;

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

        // 题目参数（猜测率 g、失误率 s）由作答数据估计，不再固定为常量
        double[] params = estimateItemParameters(records);
        double guess = params[0];
        double slip = params[1];

        Map<String, Double> result = new HashMap<>();
        for (String kpId : totalMap.keySet()) {
            result.put(kpId, estimateOne(correctMap.getOrDefault(kpId, 0), totalMap.get(kpId), guess, slip));
        }
        return result;
    }

    /**
     * 题目参数估计（分位矩估计，经验贝叶斯思路）：
     * 将所有知识点的作答视为同一参数族——已掌握知识点的答对率趋近 1-s，
     * 未掌握知识点的答对率趋近 g。取答对率分布的低四分位均值估计 g、
     * 高四分位均值估计 1-s，并夹逼到合理区间，避免个别极端知识点污染估计。
     * 当知识点数或记录数不足时，回落到经验先验值。
     *
     * @return 长度 2 的数组：[猜测率 g, 失误率 s]
     */
    public double[] estimateItemParameters(List<UserAnswerRecord> records) {
        Map<String, int[]> stat = new HashMap<>();
        int totalRecords = 0;
        for (UserAnswerRecord r : records) {
            if (r.getKnowledgePointId() == null) continue;
            int[] cnt = stat.computeIfAbsent(r.getKnowledgePointId(), k -> new int[2]);
            cnt[1]++;
            totalRecords++;
            if (Boolean.TRUE.equals(r.getIsCorrect())) {
                cnt[0]++;
            }
        }
        if (stat.size() < MIN_KP_FOR_ESTIMATION || totalRecords < MIN_RECORDS_FOR_ESTIMATION) {
            return new double[]{PRIOR_GUESS_RATE, PRIOR_SLIP_RATE};
        }

        List<Double> rates = new ArrayList<>();
        for (int[] cnt : stat.values()) {
            rates.add((double) cnt[0] / cnt[1]);
        }
        Collections.sort(rates);

        int q = Math.max(1, rates.size() / 4);
        double lowMean = 0.0;
        for (int i = 0; i < q; i++) {
            lowMean += rates.get(i);
        }
        lowMean /= q;
        double highMean = 0.0;
        for (int i = rates.size() - q; i < rates.size(); i++) {
            highMean += rates.get(i);
        }
        highMean /= q;

        double guess = clamp(lowMean, MIN_GUESS, MAX_GUESS);
        double slip = clamp(1.0 - highMean, MIN_SLIP, MAX_SLIP);
        return new double[]{guess, slip};
    }

    /**
     * RSM 式等级评定：将连续掌握概率映射为有序等级。
     * 借鉴 RSM（评定量表模型）将潜在特质离散化的思想，用四级有序类别表达掌握状态。
     *
     * @param theta 掌握概率 0~1
     * @return 未掌握 / 初步掌握 / 熟练 / 精通
     */
    public static String mapToLevel(double theta) {
        if (theta < LEVEL_LOW) return "未掌握";
        if (theta < LEVEL_MID) return "初步掌握";
        if (theta < LEVEL_HIGH) return "熟练";
        return "精通";
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 单知识点掌握概率的 EM 估计。
     *
     * @param correct 答对次数
     * @param total   总答题次数
     * @param guess   猜测率 g
     * @param slip    失误率 s
     * @return 掌握概率 0~1
     */
    private double estimateOne(int correct, int total, double guess, double slip) {
        if (total <= 0) return 0.0;
        int wrong = total - correct;
        // 初始化：以答对率为起点
        double pi = (double) correct / total;
        for (int it = 0; it < MAX_ITER; it++) {
            // E 步：答对样本的后验掌握概率 P(α=1|X=1)
            double denomC = pi * (1 - slip) + (1 - pi) * guess;
            double gammaCorrect = denomC <= 1e-9 ? pi : (pi * (1 - slip)) / denomC;
            // E 步：答错样本的后验掌握概率 P(α=1|X=0)
            double denomW = pi * slip + (1 - pi) * (1 - guess);
            double gammaWrong = denomW <= 1e-9 ? 0.0 : (pi * slip) / denomW;
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
