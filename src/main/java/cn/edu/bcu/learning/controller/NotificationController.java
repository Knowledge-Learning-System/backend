package cn.edu.bcu.learning.controller;

import cn.edu.bcu.learning.domain.entity.Notification;
import cn.edu.bcu.learning.repository.mysql.NotificationMapper;
import cn.edu.bcu.learning.utils.Result;
import cn.edu.bcu.learning.utils.ThreadLocalUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationMapper notificationMapper;

    /** 查询当前用户通知（最近20条） */
    @GetMapping
    public Result<List<Notification>> getNotifications() {
        Integer userId = ThreadLocalUtil.getUserId();
        return Result.success(notificationMapper.selectList(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .orderByDesc(Notification::getCreateTime)
                        .last("LIMIT 20")));
    }

    /** 全部标记已读 */
    @PutMapping("/read")
    public Result<?> markRead() {
        Integer userId = ThreadLocalUtil.getUserId();
        List<Notification> list = notificationMapper.selectList(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, 0));
        for (Notification n : list) {
            n.setIsRead(1);
            notificationMapper.updateById(n);
        }
        return Result.success();
    }
}
