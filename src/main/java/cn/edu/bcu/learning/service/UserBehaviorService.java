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
}
