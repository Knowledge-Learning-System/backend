package cn.edu.bcu.learning.controller;

import cn.edu.bcu.learning.domain.entity.Question;
import cn.edu.bcu.learning.domain.vo.QaSearchResultVO;
import cn.edu.bcu.learning.repository.mysql.QuestionMapper;
import cn.edu.bcu.learning.utils.Result;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/qa")
@RequiredArgsConstructor
public class QaController {

    private final QuestionMapper questionMapper;

    @PostMapping("/search")
    public Result<List<QaSearchResultVO>> search(@RequestBody Map<String, String> body) {
        String query = body.get("query");
        if (query == null || query.isBlank()) {
            return Result.success(Collections.emptyList());
        }

        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>()
                .like(Question::getContent, query);
        List<Question> questions = questionMapper.selectList(wrapper);

        List<QaSearchResultVO> results = questions.stream().map(q -> {
            QaSearchResultVO vo = new QaSearchResultVO();
            vo.setQuestionId(q.getId());
            vo.setContent(q.getContent());
            vo.setAnswer(q.getAnswer());
            vo.setAnalysis(q.getAnalysis());
            return vo;
        }).collect(Collectors.toList());

        return Result.success(results);
    }
}