package cn.edu.bcu.learning.service;

import cn.edu.bcu.learning.domain.entity.UserBehavior;
import cn.edu.bcu.learning.repository.mysql.UserBehaviorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserBehaviorService {

    private final UserBehaviorMapper userBehaviorMapper;

    /**
     * 记录用户行为。在 controller / service 中埋点调用。
     * @param userId 用户id
     * @param knowledgePointId 知识点id（可选）
     * @param action 行为类型：view_kp / click_kp / complete_quiz / start_learning
     */
    public void record(Integer userId, String knowledgePointId, String action) {
        UserBehavior ub = new UserBehavior();
        ub.setUserId(userId);
        ub.setKnowledgePointId(knowledgePointId);
        ub.setAction(action);
        userBehaviorMapper.insert(ub);
    }

    public int getActiveDays(Integer userId) {
        return userBehaviorMapper.countActiveDays(userId);
    }

    /**
     * 记录学习停留时长（action=study_time，durationSeconds 为累计秒）。
     * 功能3：学习时长采集链路（前端学习/答题结束上报，统计接口按用户聚合）。
     */
    public void recordStudyTime(Integer userId, String knowledgePointId, Integer durationSeconds) {
        UserBehavior ub = new UserBehavior();
        ub.setUserId(userId);
        ub.setKnowledgePointId(knowledgePointId);
        ub.setAction("study_time");
        ub.setDurationSeconds(durationSeconds == null ? 0 : durationSeconds);
        userBehaviorMapper.insert(ub);
    }

    /** 累计学习时长（秒），基于 action=study_time 的 duration_seconds 求和 */
    public int getTotalStudySeconds(Integer userId) {
        return userBehaviorMapper.sumStudySeconds(userId);
    }
}
