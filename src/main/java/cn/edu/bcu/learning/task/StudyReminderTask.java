package cn.edu.bcu.learning.task;

import cn.edu.bcu.learning.domain.entity.Notification;
import cn.edu.bcu.learning.domain.entity.StudyPlan;
import cn.edu.bcu.learning.repository.mysql.NotificationMapper;
import cn.edu.bcu.learning.repository.mysql.StudyPlanMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StudyReminderTask {

    private final StudyPlanMapper studyPlanMapper;
    private final NotificationMapper notificationMapper;

    /** 每分钟扫描一次，到提醒时间且当天未提醒则生成通知 */
    @Scheduled(cron = "0 * * * * *")
    public void sendReminders() {
        String now = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        LocalDate today = LocalDate.now();
        List<StudyPlan> plans = studyPlanMapper.selectList(
                new LambdaQueryWrapper<StudyPlan>()
                        .eq(StudyPlan::getRemindTime, now)
                        .le(StudyPlan::getStartDate, today)
                        .ge(StudyPlan::getEndDate, today));
        for (StudyPlan plan : plans) {
            Long count = notificationMapper.selectCount(
                    new LambdaQueryWrapper<Notification>()
                            .eq(Notification::getUserId, plan.getUserId())
                            .eq(Notification::getType, "remind")
                            .ge(Notification::getCreateTime, today.atStartOfDay()));
            if (count != null && count > 0) continue;
            Notification n = new Notification();
            n.setUserId(plan.getUserId());
            n.setContent("到学习时间啦，记得完成今日学习计划");
            n.setType("remind");
            n.setIsRead(0);
            notificationMapper.insert(n);
        }
    }
}
