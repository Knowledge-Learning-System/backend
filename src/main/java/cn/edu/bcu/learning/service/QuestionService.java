package cn.edu.bcu.learning.service;

import cn.edu.bcu.learning.domain.dto.SubmitAnswerRequest;
import cn.edu.bcu.learning.domain.entity.Course;
import cn.edu.bcu.learning.domain.entity.Question;
import cn.edu.bcu.learning.domain.entity.UserAnswerRecord;
import cn.edu.bcu.learning.domain.vo.AnswerDetailVO;
import cn.edu.bcu.learning.domain.vo.SubmitAnswerResultVO;
import cn.edu.bcu.learning.repository.mysql.CourseMapper;
import cn.edu.bcu.learning.repository.mysql.QuestionMapper;
import cn.edu.bcu.learning.repository.mysql.UserAnswerRecordMapper;
import cn.edu.bcu.learning.repository.neo4j.KnowledgeGraphRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionMapper questionMapper;
    private final UserAnswerRecordMapper userAnswerRecordMapper;
    private final KnowledgeGraphRepository knowledgeGraphRepository;
    private final CourseMapper courseMapper;

    /**
     * 获取某课程的所有题目（做题时不返回答案）
     * 若指定知识点下无题，自动递归查询所有子孙知识点
     */
    public List<AnswerDetailVO> getQuestions(Integer courseId, String knowledgePointId) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>()
                .eq(Question::getCourseId, courseId);
        if (knowledgePointId != null && !knowledgePointId.isEmpty()) {
            wrapper.eq(Question::getKnowledgePointId, knowledgePointId);
        }
        List<AnswerDetailVO> result = questionMapper.selectList(wrapper).stream().map(q -> {
            AnswerDetailVO vo = new AnswerDetailVO();
            vo.setId(q.getId());
            vo.setType(q.getType());
            vo.setContent(q.getContent());
            vo.setOptions(q.getOptions());
            vo.setKnowledgePointId(q.getKnowledgePointId());
            return vo;
        }).collect(Collectors.toList());

        // 当前知识点无题且指定了具体知识点 → 递归向下查子孙节点
        if (result.isEmpty() && knowledgePointId != null && !knowledgePointId.isEmpty()) {
            Course course = courseMapper.selectById(courseId);
            if (course != null && course.getSource() != null) {
                List<String> descendantIds = knowledgeGraphRepository.findDescendantKpIds(
                        course.getSource(), knowledgePointId);
                // descendantIds 包含自身，size > 1 才说明有子节点可递归
                if (descendantIds != null && descendantIds.size() > 1) {
                    return getQuestionsByKpIds(courseId, descendantIds);
                }
            }
        }

        return result;
    }

    /**
     * 批量按知识点ID查询题目（IN 查询）
     */
    public List<AnswerDetailVO> getQuestionsByKpIds(Integer courseId, List<String> kpIds) {
        if (CollectionUtils.isEmpty(kpIds)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>()
                .eq(Question::getCourseId, courseId)
                .in(Question::getKnowledgePointId, kpIds);
        return questionMapper.selectList(wrapper).stream().map(q -> {
            AnswerDetailVO vo = new AnswerDetailVO();
            vo.setId(q.getId());
            vo.setType(q.getType());
            vo.setContent(q.getContent());
            vo.setOptions(q.getOptions());
            vo.setKnowledgePointId(q.getKnowledgePointId());
            return vo;
        }).collect(Collectors.toList());
    }

    @Transactional
    public SubmitAnswerResultVO submit(SubmitAnswerRequest request) {
        List<Integer> questionIds = request.getAnswers().stream()
                .map(SubmitAnswerRequest.AnswerItem::getQuestionId).toList();
        List<Question> questions = questionMapper.selectBatchIds(questionIds);

        int correctCount = 0;
        List<SubmitAnswerResultVO.ItemResult> itemResults = new ArrayList<>();

        for (SubmitAnswerRequest.AnswerItem item : request.getAnswers()) {
            Question q = questions.stream()
                    .filter(qu -> qu.getId().equals(item.getQuestionId()))
                    .findFirst().orElse(null);
            if (q == null) continue;

            boolean isCorrect = q.getAnswer().equalsIgnoreCase(item.getAnswer().trim());
            if (isCorrect) correctCount++;

            itemResults.add(buildItemResult(item.getQuestionId(), isCorrect, q.getAnswer(), q.getAnalysis()));

            // 记录答题
            UserAnswerRecord record = new UserAnswerRecord();
            record.setUserId(request.getUserId());
            record.setQuestionId(item.getQuestionId());
            record.setKnowledgePointId(item.getKnowledgePointId());
            record.setUserAnswer(item.getAnswer());
            record.setIsCorrect(isCorrect);
            userAnswerRecordMapper.insert(record);
        }

        SubmitAnswerResultVO result = new SubmitAnswerResultVO();
        result.setTotalQuestions(request.getAnswers().size());
        result.setCorrectCount(correctCount);
        result.setItems(itemResults);
        return result;
    }

    private SubmitAnswerResultVO.ItemResult buildItemResult(Integer qid, boolean isCorrect, String answer, String analysis) {
        SubmitAnswerResultVO.ItemResult ir = new SubmitAnswerResultVO.ItemResult();
        ir.setQuestionId(qid);
        ir.setIsCorrect(isCorrect);
        ir.setCorrectAnswer(answer);
        ir.setAnalysis(analysis);
        return ir;
    }
}
