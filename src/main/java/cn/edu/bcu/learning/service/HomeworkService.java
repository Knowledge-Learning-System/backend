package cn.edu.bcu.learning.service;

import cn.edu.bcu.learning.domain.dto.CreateHomeworkRequest;
import cn.edu.bcu.learning.domain.entity.Homework;
import cn.edu.bcu.learning.domain.entity.HomeworkSubmission;
import cn.edu.bcu.learning.domain.entity.User;
import cn.edu.bcu.learning.domain.vo.HomeworkSubmissionVO;
import cn.edu.bcu.learning.domain.vo.HomeworkVO;
import cn.edu.bcu.learning.repository.mysql.HomeworkMapper;
import cn.edu.bcu.learning.repository.mysql.HomeworkSubmissionMapper;
import cn.edu.bcu.learning.repository.mysql.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class HomeworkService {

    @Autowired
    private HomeworkMapper homeworkMapper;

    @Autowired
    private HomeworkSubmissionMapper homeworkSubmissionMapper;

    @Autowired
    private UserMapper userMapper;

    public Homework createHomework(CreateHomeworkRequest request) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new RuntimeException("作业标题不能为空");
        }
        Homework homework = new Homework();
        homework.setCourseId(request.getCourseId());
        homework.setKnowledgePointId(request.getKnowledgePointId());
        homework.setTitle(request.getTitle());
        homework.setDescription(request.getDescription());
        homework.setDeadline(request.getDeadline());
        homework.setStatus("draft");
        homeworkMapper.insert(homework);
        return homework;
    }

    public void publishHomework(Long homeworkId) {
        Homework homework = homeworkMapper.selectById(homeworkId);
        if (homework == null) {
            throw new RuntimeException("作业不存在");
        }
        homework.setStatus("published");
        homeworkMapper.updateById(homework);
    }

    public void closeHomework(Long homeworkId) {
        Homework homework = homeworkMapper.selectById(homeworkId);
        if (homework == null) {
            throw new RuntimeException("作业不存在");
        }
        homework.setStatus("closed");
        homeworkMapper.updateById(homework);
    }

    public List<HomeworkVO> listHomeworkByCourse(Long courseId, Long currentUserId) {
        List<Homework> homeworks = homeworkMapper.selectList(new LambdaQueryWrapper<Homework>()
                .eq(Homework::getCourseId, courseId)
                .orderByDesc(Homework::getCreateTime));
        List<HomeworkVO> vos = new ArrayList<>();
        for (Homework h : homeworks) {
            HomeworkVO vo = new HomeworkVO();
            BeanUtils.copyProperties(h, vo);
            Long count = homeworkSubmissionMapper.selectCount(new LambdaQueryWrapper<HomeworkSubmission>()
                    .eq(HomeworkSubmission::getHomeworkId, h.getId()));
            vo.setSubmissionCount(count == null ? 0 : count.intValue());
            if (currentUserId != null) {
                HomeworkSubmission mine = homeworkSubmissionMapper.selectOne(
                        new LambdaQueryWrapper<HomeworkSubmission>()
                                .eq(HomeworkSubmission::getHomeworkId, h.getId())
                                .eq(HomeworkSubmission::getUserId, currentUserId));
                if (mine != null) {
                    vo.setMyScore(mine.getScore());
                    vo.setMyFeedback(mine.getFeedback());
                }
            }
            vos.add(vo);
        }
        return vos;
    }

    public HomeworkVO getHomeworkDetail(Long homeworkId) {
        // TODO
        return null;
    }

    public void submitHomework(Long homeworkId, Long userId, HomeworkSubmission submission) {
        if (homeworkId == null || userId == null) {
            throw new RuntimeException("提交参数缺失");
        }
        Homework homework = homeworkMapper.selectById(homeworkId);
        if (homework == null) {
            throw new RuntimeException("作业不存在");
        }
        if (!"published".equals(homework.getStatus())) {
            throw new RuntimeException("作业未发布，无法提交");
        }
        if (homework.getDeadline() != null && homework.getDeadline().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("作业已过截止时间，无法提交");
        }
        // 同一用户同一作业重复提交时覆盖更新
        HomeworkSubmission existing = homeworkSubmissionMapper.selectOne(
                new LambdaQueryWrapper<HomeworkSubmission>()
                        .eq(HomeworkSubmission::getHomeworkId, homeworkId)
                        .eq(HomeworkSubmission::getUserId, userId));
        if (existing != null) {
            existing.setContent(submission.getContent());
            existing.setAttachments(submission.getAttachments());
            existing.setSubmitTime(LocalDateTime.now());
            homeworkSubmissionMapper.updateById(existing);
        } else {
            HomeworkSubmission sub = new HomeworkSubmission();
            sub.setHomeworkId(homeworkId);
            sub.setUserId(userId);
            sub.setContent(submission.getContent());
            sub.setAttachments(submission.getAttachments());
            sub.setSubmitTime(LocalDateTime.now());
            homeworkSubmissionMapper.insert(sub);
        }
    }

    public List<HomeworkSubmissionVO> getSubmissionsByHomework(Long homeworkId) {
        List<HomeworkSubmission> submissions = homeworkSubmissionMapper.selectList(
                new LambdaQueryWrapper<HomeworkSubmission>()
                        .eq(HomeworkSubmission::getHomeworkId, homeworkId)
                        .orderByDesc(HomeworkSubmission::getSubmitTime));
        List<HomeworkSubmissionVO> vos = new ArrayList<>();
        for (HomeworkSubmission s : submissions) {
            HomeworkSubmissionVO vo = new HomeworkSubmissionVO();
            BeanUtils.copyProperties(s, vo);
            User user = userMapper.selectById(s.getUserId());
            vo.setUsername(user != null ? user.getUsername() : String.valueOf(s.getUserId()));
            vos.add(vo);
        }
        return vos;
    }

    public void gradeSubmission(Long submissionId, Integer score, String feedback) {
        if (submissionId == null) {
            throw new RuntimeException("评分提交参数缺失");
        }
        if (score == null) {
            throw new RuntimeException("分数不能为空");
        }
        HomeworkSubmission submission = homeworkSubmissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new RuntimeException("提交记录不存在");
        }
        submission.setScore(score);
        submission.setFeedback(feedback);
        submission.setGradeTime(LocalDateTime.now());
        homeworkSubmissionMapper.updateById(submission);
    }
}
