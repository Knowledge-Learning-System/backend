package cn.edu.bcu.learning.service;

import cn.edu.bcu.learning.domain.dto.CreateHomeworkRequest;
import cn.edu.bcu.learning.domain.entity.Homework;
import cn.edu.bcu.learning.domain.entity.HomeworkSubmission;
import cn.edu.bcu.learning.domain.vo.HomeworkSubmissionVO;
import cn.edu.bcu.learning.domain.vo.HomeworkVO;
import cn.edu.bcu.learning.repository.mysql.HomeworkMapper;
import cn.edu.bcu.learning.repository.mysql.HomeworkSubmissionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HomeworkService {

    @Autowired
    private HomeworkMapper homeworkMapper;

    @Autowired
    private HomeworkSubmissionMapper homeworkSubmissionMapper;

    public Homework createHomework(CreateHomeworkRequest request) {
        // TODO
        return null;
    }

    public void publishHomework(Long homeworkId) {
        // TODO: status draft → published
    }

    public void closeHomework(Long homeworkId) {
        // TODO: status → closed
    }

    public List<HomeworkVO> listHomeworkByCourse(Long courseId) {
        // TODO
        return null;
    }

    public HomeworkVO getHomeworkDetail(Long homeworkId) {
        // TODO
        return null;
    }

    public void submitHomework(Long homeworkId, Long userId, HomeworkSubmission submission) {
        // TODO
    }

    public List<HomeworkSubmissionVO> getSubmissionsByHomework(Long homeworkId) {
        // TODO
        return null;
    }

    public void gradeSubmission(Long submissionId, Integer score, String feedback) {
        // TODO
    }
}
