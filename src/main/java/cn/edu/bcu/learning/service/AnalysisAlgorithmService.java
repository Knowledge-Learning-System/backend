package cn.edu.bcu.learning.service;

import org.springframework.stereotype.Service;

@Service
public class AnalysisAlgorithmService {

    /** 知识点掌握度时间衰减算法 */
    public double calculateDecayedMastery(Long userId, Long knowledgePointId) {
        // TODO: 越早的答题记录权重越低
        return 0.0;
    }

    /** 基于知识图谱的缺陷溯源（BFS 从弱节点向上追溯先修节点） */
    public void traceWeaknessByGraph(Long userId, Long courseId) {
        // TODO: Neo4j BFS 查询
    }

    /** 学习路径推荐排序（综合掌握度 + 先修依赖 + 难度权重） */
    public void recommendLearningPath(Long userId, Long courseId) {
        // TODO
    }
}
