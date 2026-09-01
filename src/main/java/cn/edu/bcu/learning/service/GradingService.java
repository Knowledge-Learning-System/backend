package cn.edu.bcu.learning.service;

import cn.edu.bcu.learning.domain.dto.GradingDiagnoseRequest;
import cn.edu.bcu.learning.domain.dto.GradingRequest;
import cn.edu.bcu.learning.domain.entity.AiGradingRecord;
import cn.edu.bcu.learning.domain.entity.Homework;
import cn.edu.bcu.learning.domain.entity.HomeworkSubmission;
import cn.edu.bcu.learning.domain.entity.Question;
import cn.edu.bcu.learning.domain.vo.GradingRecordVO;
import cn.edu.bcu.learning.domain.vo.GradingResultVO;
import cn.edu.bcu.learning.repository.mysql.AiGradingRecordMapper;
import cn.edu.bcu.learning.repository.mysql.HomeworkMapper;
import cn.edu.bcu.learning.repository.mysql.HomeworkSubmissionMapper;
import cn.edu.bcu.learning.repository.mysql.QuestionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 主观题判分服务（千问 json_object）+ 大模型诊断推荐链路。
 *
 * 流程：
 *   1. 解析待判分的提交（按 homework 或 submission 维度）
 *   2. 解析目标主观题（显式 questionId 或按作业课程自动定位）
 *   3. 调用千问（response_format=json_object）一次返回：auto_score、维度判分详情、诊断结论与学习推荐
 *   4. 写入 ai_grading_record（grading_detail 落库完整 JSON）
 *   5. 回写 homework_submission.auto_score / final_score / grade_status
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GradingService {

    private final AiGradingRecordMapper aiGradingRecordMapper;
    private final HomeworkMapper homeworkMapper;
    private final HomeworkSubmissionMapper homeworkSubmissionMapper;
    private final QuestionMapper questionMapper;

    @Value("${qwen.api-key:}")
    private String apiKey;

    @Value("${qwen.model:qwen-plus}")
    private String model;

    @Value("${qwen.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private WebClient webClient;

    // ============================================================
    // 1. 判分触发（按 homework / submission 维度）
    // ============================================================

    public List<GradingResultVO> grade(GradingRequest req) {
        if (req == null || (req.getHomeworkId() == null && req.getSubmissionId() == null)) {
            throw new RuntimeException("请提供 homeworkId（按作业批量判分）或 submissionId（按提交判分）");
        }
        List<HomeworkSubmission> submissions = resolveSubmissions(req);
        List<GradingResultVO> results = new ArrayList<>();
        for (HomeworkSubmission sub : submissions) {
            results.add(gradeOne(sub, req.getQuestionId()));
        }
        return results;
    }

    private List<HomeworkSubmission> resolveSubmissions(GradingRequest req) {
        if (req.getSubmissionId() != null) {
            HomeworkSubmission s = homeworkSubmissionMapper.selectById(req.getSubmissionId());
            if (s == null) {
                throw new RuntimeException("提交记录不存在: submissionId=" + req.getSubmissionId());
            }
            return List.of(s);
        }
        List<HomeworkSubmission> list = homeworkSubmissionMapper.selectList(
                new LambdaQueryWrapper<HomeworkSubmission>()
                        .eq(HomeworkSubmission::getHomeworkId, req.getHomeworkId()));
        if (list == null || list.isEmpty()) {
            throw new RuntimeException("该作业暂无提交记录，无法判分: homeworkId=" + req.getHomeworkId());
        }
        return list;
    }

    private GradingResultVO gradeOne(HomeworkSubmission sub, Integer questionId) {
        Homework homework = homeworkMapper.selectById(sub.getHomeworkId());
        if (homework == null) {
            throw new RuntimeException("作业不存在: homeworkId=" + sub.getHomeworkId());
        }
        Question question = resolveQuestion(homework, questionId);
        String userAnswer = sub.getContent();
        String standardAnswer = question.getAnswerText();

        // 千问判分 + 诊断推荐（一次调用返回结构化 JSON）
        String prompt = buildGradingPrompt(question, userAnswer);
        String rawJson = callQwenJson(prompt);
        QwenGradingResult parsed = parseGradingJson(rawJson);

        Integer autoScore = clampScore(parsed.getAutoScore());
        String gradingDetail = rawJson;

        // 写 ai_grading_record
        AiGradingRecord record = new AiGradingRecord();
        record.setSubmissionId(sub.getId().intValue());
        record.setUserId(sub.getUserId().intValue());
        record.setQuestionId(question.getId());
        record.setQuestionType("subjective");
        record.setUserAnswer(userAnswer);
        record.setStandardAnswer(standardAnswer);
        record.setAutoScore(autoScore);
        record.setFinalScore(autoScore);
        record.setGradeStatus("graded");
        record.setGradingDetail(gradingDetail);
        record.setModelName(model);
        record.setCreateTime(LocalDateTime.now());
        aiGradingRecordMapper.insert(record);

        // 回写 homework_submission
        sub.setAutoScore(autoScore);
        sub.setFinalScore(autoScore);
        sub.setGradeStatus("graded");
        sub.setGradeTime(LocalDateTime.now());
        homeworkSubmissionMapper.updateById(sub);

        // 组装返回
        GradingResultVO vo = new GradingResultVO();
        vo.setRecordId(record.getId());
        vo.setSubmissionId(sub.getId().intValue());
        vo.setUserId(sub.getUserId().intValue());
        vo.setQuestionId(question.getId());
        vo.setQuestionType("subjective");
        vo.setUserAnswer(userAnswer);
        vo.setStandardAnswer(standardAnswer);
        vo.setAutoScore(autoScore);
        vo.setFinalScore(autoScore);
        vo.setGradeStatus("graded");
        vo.setHasProblem(parsed.getHasProblem());
        vo.setProblems(parsed.getProblems());
        vo.setDiagnosis(parsed.getDiagnosis());
        vo.setRecommendations(parsed.getRecommendations());
        vo.setDimensions(parsed.getDimensions());
        vo.setModelName(model);
        vo.setCreateTime(record.getCreateTime());
        return vo;
    }

    /**
     * 解析目标主观题：
     *   1. 显式 questionId
     *   2. 按作业课程下的主观题自动定位（优先匹配作业知识点，否则取课程下第一道主观题）
     */
    private Question resolveQuestion(Homework homework, Integer questionId) {
        if (questionId != null) {
            Question q = questionMapper.selectById(questionId);
            if (q == null) {
                throw new RuntimeException("题目不存在: questionId=" + questionId);
            }
            return q;
        }
        Integer courseId = homework.getCourseId() == null ? null : homework.getCourseId().intValue();
        String kpId = homework.getKnowledgePointId() == null ? null : String.valueOf(homework.getKnowledgePointId());
        if (courseId != null) {
            // 优先匹配知识点
            if (kpId != null) {
                Question q = questionMapper.selectOne(new LambdaQueryWrapper<Question>()
                        .eq(Question::getCourseId, courseId)
                        .eq(Question::getKnowledgePointId, kpId)
                        .eq(Question::getQuestionType, "subjective")
                        .last("LIMIT 1"));
                if (q != null) {
                    return q;
                }
            }
            // 退化为课程下第一道主观题
            Question q = questionMapper.selectOne(new LambdaQueryWrapper<Question>()
                    .eq(Question::getCourseId, courseId)
                    .eq(Question::getQuestionType, "subjective")
                    .last("LIMIT 1"));
            if (q != null) {
                return q;
            }
        }
        throw new RuntimeException("未找到可判分的主观题，请在题目中配置 question_type=subjective，或显式指定 questionId");
    }

    // ============================================================
    // 2. 判分记录查询
    // ============================================================

    public List<GradingRecordVO> listRecords(Long submissionId, Long userId) {
        if (submissionId == null && userId == null) {
            throw new RuntimeException("请提供 submissionId 或 userId");
        }
        LambdaQueryWrapper<AiGradingRecord> wrapper = new LambdaQueryWrapper<AiGradingRecord>()
                .orderByDesc(AiGradingRecord::getCreateTime);
        if (submissionId != null) {
            wrapper.eq(AiGradingRecord::getSubmissionId, submissionId);
        }
        if (userId != null) {
            wrapper.eq(AiGradingRecord::getUserId, userId);
        }
        List<AiGradingRecord> records = aiGradingRecordMapper.selectList(wrapper);
        List<GradingRecordVO> vos = new ArrayList<>();
        for (AiGradingRecord r : records) {
            GradingRecordVO vo = new GradingRecordVO();
            vo.setId(r.getId());
            vo.setSubmissionId(r.getSubmissionId());
            vo.setUserId(r.getUserId());
            vo.setQuestionId(r.getQuestionId());
            vo.setQuestionType(r.getQuestionType());
            vo.setUserAnswer(r.getUserAnswer());
            vo.setStandardAnswer(r.getStandardAnswer());
            vo.setAutoScore(r.getAutoScore());
            vo.setFinalScore(r.getFinalScore());
            vo.setGradeStatus(r.getGradeStatus());
            vo.setModelName(r.getModelName());
            vo.setCreateTime(r.getCreateTime());
            // 解析 grading_detail 中的诊断推荐
            QwenGradingResult parsed = parseGradingJsonQuietly(r.getGradingDetail());
            if (parsed != null) {
                vo.setHasProblem(parsed.getHasProblem());
                vo.setProblems(parsed.getProblems());
                vo.setDiagnosis(parsed.getDiagnosis());
                vo.setRecommendations(parsed.getRecommendations());
                vo.setDimensions(parsed.getDimensions());
            }
            vos.add(vo);
        }
        return vos;
    }

    // ============================================================
    // 3. 大模型诊断推荐（独立接口）
    // ============================================================

    public GradingResultVO diagnose(GradingDiagnoseRequest req) {
        if (req == null) {
            throw new RuntimeException("诊断请求参数缺失");
        }
        String userAnswer = req.getUserAnswer();
        String standardAnswer = req.getStandardAnswer();
        Integer questionId = req.getQuestionId();

        // 基于已有提交诊断
        if (req.getSubmissionId() != null) {
            HomeworkSubmission sub = homeworkSubmissionMapper.selectById(req.getSubmissionId());
            if (sub == null) {
                throw new RuntimeException("提交记录不存在: submissionId=" + req.getSubmissionId());
            }
            Homework homework = homeworkMapper.selectById(sub.getHomeworkId());
            if (homework == null) {
                throw new RuntimeException("作业不存在");
            }
            Question q = questionId != null ? questionMapper.selectById(questionId)
                    : resolveQuestion(homework, null);
            if (q != null) {
                questionId = q.getId();
                if (standardAnswer == null) {
                    standardAnswer = q.getAnswerText();
                }
            }
            if (userAnswer == null) {
                userAnswer = sub.getContent();
            }
        }
        if (userAnswer == null || userAnswer.isBlank()) {
            throw new RuntimeException("缺少用户作答内容");
        }

        String prompt = buildDiagnosePrompt(userAnswer, standardAnswer);
        String rawJson = callQwenJson(prompt);
        QwenGradingResult parsed = parseGradingJson(rawJson);

        GradingResultVO vo = new GradingResultVO();
        vo.setQuestionId(questionId);
        vo.setUserAnswer(userAnswer);
        vo.setStandardAnswer(standardAnswer);
        vo.setHasProblem(parsed.getHasProblem());
        vo.setProblems(parsed.getProblems());
        vo.setDiagnosis(parsed.getDiagnosis());
        vo.setRecommendations(parsed.getRecommendations());
        vo.setDimensions(parsed.getDimensions());
        vo.setModelName(model);
        vo.setCreateTime(LocalDateTime.now());
        return vo;
    }

    // ============================================================
    // 4. Prompt 构建
    // ============================================================

    private String buildGradingPrompt(Question question, String userAnswer) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是高校在线学习系统的主观题判分专家。请对学生的作答进行评分，并给出诊断结论与学习推荐。\n");
        sb.append("要求：仅输出一个 JSON 对象（不要输出其他文字），结构如下：\n");
        sb.append("{\n");
        sb.append("  \"auto_score\": 0到100之间的整数,\n");
        sb.append("  \"dimensions\": [{\"name\": \"维度名\", \"score\": 0-100整数, \"comment\": \"该维度简评\"}],\n");
        sb.append("  \"has_problem\": true或false（作答是否存在问题）,\n");
        sb.append("  \"problems\": [\"具体问题点1\", \"具体问题点2\"],\n");
        sb.append("  \"diagnosis\": \"总体诊断结论（中文）\",\n");
        sb.append("  \"recommendations\": [\"学习建议1\", \"学习建议2\"]\n");
        sb.append("}\n\n");
        sb.append("## 题目\n").append(question.getContent() == null ? "" : question.getContent()).append("\n\n");
        sb.append("## 参考答案\n").append(question.getAnswerText() == null ? "（未提供）" : question.getAnswerText()).append("\n\n");
        sb.append("## 学生作答\n").append(userAnswer == null ? "（未作答）" : userAnswer).append("\n");
        return sb.toString();
    }

    private String buildDiagnosePrompt(String userAnswer, String standardAnswer) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是高校在线学习系统的智能学习诊断助手。请根据学生的作答与参考答案，判断学生答得是否有问题，指出具体问题点，并给出学习推荐。\n");
        sb.append("要求：仅输出一个 JSON 对象（不要输出其他文字），结构如下：\n");
        sb.append("{\n");
        sb.append("  \"has_problem\": true或false,\n");
        sb.append("  \"problems\": [\"具体问题点1\", \"具体问题点2\"],\n");
        sb.append("  \"diagnosis\": \"总体诊断结论（中文）\",\n");
        sb.append("  \"recommendations\": [\"学习建议1\", \"学习建议2\"]\n");
        sb.append("}\n\n");
        sb.append("## 参考答案\n").append(standardAnswer == null ? "（未提供）" : standardAnswer).append("\n\n");
        sb.append("## 学生作答\n").append(userAnswer == null ? "（未作答）" : userAnswer).append("\n");
        return sb.toString();
    }

    // ============================================================
    // 5. 千问调用（json_object 模式）
    // ============================================================

    private String callQwenJson(String userPrompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("未配置 qwen.api-key，无法调用千问判分服务");
        }
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> system = new LinkedHashMap<>();
        system.put("role", "system");
        system.put("content", "你只输出合法的 JSON 对象，不要包含 Markdown 代码块标记。");
        messages.add(system);
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("role", "user");
        user.put("content", userPrompt);
        messages.add(user);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", 0.2);
        body.put("max_tokens", 2048);
        body.put("response_format", Map.of("type", "json_object"));

        String raw;
        try {
            raw = getWebClient().post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            throw new RuntimeException("调用千问 API 失败: " + e.getMessage(), e);
        }
        QwenChatResponse response;
        try {
            response = objectMapper.readValue(raw, QwenChatResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("解析千问响应失败: " + e.getMessage(), e);
        }
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()
                || response.getChoices().get(0).getMessage() == null) {
            throw new RuntimeException("千问 API 返回空结果");
        }
        return response.getChoices().get(0).getMessage().getContent();
    }

    private WebClient getWebClient() {
        if (webClient == null) {
            webClient = WebClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .defaultHeader("Content-Type", "application/json")
                    .build();
        }
        return webClient;
    }

    // ============================================================
    // 6. 解析与工具方法
    // ============================================================

    private QwenGradingResult parseGradingJson(String content) {
        if (content == null || content.isBlank()) {
            throw new RuntimeException("千问未返回判分内容");
        }
        String json = stripJsonFence(content);
        try {
            return objectMapper.readValue(json, QwenGradingResult.class);
        } catch (Exception e) {
            log.warn("解析判分 JSON 失败，原文: {}", content, e);
            throw new RuntimeException("解析千问判分结果失败: " + e.getMessage(), e);
        }
    }

    private QwenGradingResult parseGradingJsonQuietly(String content) {
        try {
            return parseGradingJson(content);
        } catch (Exception e) {
            return null;
        }
    }

    /** 去掉可能的 ```json ... ``` 围栏 */
    private String stripJsonFence(String content) {
        String s = content.trim();
        if (s.startsWith("```")) {
            int firstNl = s.indexOf('\n');
            if (firstNl >= 0) {
                s = s.substring(firstNl + 1);
            }
            if (s.endsWith("```")) {
                s = s.substring(0, s.length() - 3);
            }
            s = s.trim();
        }
        return s;
    }

    private Integer clampScore(Integer score) {
        if (score == null) {
            return 0;
        }
        return Math.max(0, Math.min(100, score));
    }

    // ============================================================
    // 内部 DTO
    // ============================================================

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class QwenGradingResult {
        /** 兼容千问输出的 snake_case（auto_score）与 camelCase（autoScore）两种键名 */
        @JsonAlias({"auto_score", "autoScore"})
        private Integer autoScore;
        @JsonAlias({"dimensions"})
        private List<GradingResultVO.DimensionScore> dimensions;
        @JsonAlias({"has_problem", "hasProblem"})
        private Boolean hasProblem;
        @JsonAlias({"problems"})
        private List<String> problems;
        @JsonAlias({"diagnosis"})
        private String diagnosis;
        @JsonAlias({"recommendations"})
        private List<String> recommendations;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class QwenChatResponse {
        private List<Choice> choices;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class Choice {
            private QwenMessage message;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class QwenMessage {
        private String role;
        private String content;
    }
}
