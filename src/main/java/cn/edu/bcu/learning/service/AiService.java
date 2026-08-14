package cn.edu.bcu.learning.service;

import cn.edu.bcu.learning.domain.dto.AiChatRequest;
import cn.edu.bcu.learning.domain.entity.AiMessage;
import cn.edu.bcu.learning.domain.entity.Course;
import cn.edu.bcu.learning.domain.entity.ResourceChunk;
import cn.edu.bcu.learning.domain.vo.AiChatResponseVO;
import cn.edu.bcu.learning.domain.vo.FunctionEntryVO;
import cn.edu.bcu.learning.repository.mysql.AiMessageMapper;
import cn.edu.bcu.learning.repository.mysql.CourseMapper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiService {

    @Autowired
    private AiMessageMapper aiMessageMapper;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private RagService ragService;

    @Value("${qwen.api-key:}")
    private String apiKey;

    @Value("${qwen.model:qwen-plus}")
    private String model;

    @Value("${qwen.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    private static final String SYSTEM_PROMPT =
        "你是高校个性化在线学习系统的AI学习助手「AI伴学」。\n" +
        "你的职责：帮助学生理解课程知识、解答学习疑问、提供学习建议。\n" +
        "规则：\n" +
        "1. 回答应简洁准确，围绕课程内容展开\n" +
        "2. 若学生提问与课程无关，温和引导回学习主题\n" +
        "3. 遇到不确定的内容，坦诚说明而非编造\n" +
        "4. 用中文回答，语气亲切但专业";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private WebClient webClient;

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

    /** 发送消息并获取 AI 回复 */
    public AiChatResponseVO chat(Long userId, AiChatRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            AiChatResponseVO fallback = new AiChatResponseVO();
            fallback.setReply("AI 助手尚未配置 API Key，请联系管理员在 application-dev.yml 中设置 qwen.api-key。");
            fallback.setSources(Collections.emptyList());
            return fallback;
        }

        // 1. 读取最近 10 条历史上下文
        List<AiMessage> history = aiMessageMapper.selectRecentByUser(userId, 10);
        Collections.reverse(history); // 按时间正序

        // 2. 构建课程上下文
        String courseContext = buildCourseContext(request.getCourseId());

        // 2.5 召回当前课程下相关资料片段
        String ragContext = buildRagContext(request.getCourseId(), request.getMessage());

        // 3. 构建消息列表
        List<QwenMessage> messages = new ArrayList<>();
        messages.add(QwenMessage.system(SYSTEM_PROMPT + "\n\n当前课程信息：" + courseContext + "\n\n可参考的课程资料片段：\n" + ragContext));

        for (AiMessage msg : history) {
            messages.add(new QwenMessage(msg.getRole(), msg.getContent()));
        }
        messages.add(new QwenMessage("user", request.getMessage()));

        // 4. 调用千问 API
        String reply;
        try {
            reply = callQwenApi(messages);
        } catch (Exception e) {
            AiChatResponseVO errorVo = new AiChatResponseVO();
            errorVo.setReply("抱歉，AI 服务暂时不可用，请稍后重试。（" + e.getMessage() + "）");
            errorVo.setSources(Collections.emptyList());
            return errorVo;
        }

        // 5. 保存问答记录
        AiMessage userMsg = new AiMessage();
        userMsg.setUserId(userId);
        userMsg.setCourseId(request.getCourseId());
        userMsg.setRole("user");
        userMsg.setContent(request.getMessage());
        aiMessageMapper.insert(userMsg);

        AiMessage assistantMsg = new AiMessage();
        assistantMsg.setUserId(userId);
        assistantMsg.setCourseId(request.getCourseId());
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(reply);
        aiMessageMapper.insert(assistantMsg);

        // 6. 返回
        AiChatResponseVO vo = new AiChatResponseVO();
        vo.setReply(reply);
        vo.setSources(Collections.emptyList());
        vo.setFunctions(matchFunctions(request.getMessage()));
        return vo;
    }

    /** 获取当前课程的问答历史 */
    public List<AiMessage> getHistory(Long userId, Long courseId) {
        return aiMessageMapper.selectByUserAndCourse(userId, courseId);
    }

    // ---- 内部方法 ----

    private String buildCourseContext(Long courseId) {
        if (courseId == null) return "未选择课程";
        Course course = courseMapper.selectById(courseId);
        if (course == null) return "课程ID:" + courseId;
        return String.format("《%s》- %s", course.getName(),
                course.getDescription() != null ? course.getDescription() : "暂无简介");
    }

    private String buildRagContext(Long courseId, String query) {
        if (courseId == null) return "（无）";
        List<ResourceChunk> chunks = ragService.recall(courseId.intValue(), query, 5);
        if (chunks == null || chunks.isEmpty()) return "（无）";
        StringBuilder sb = new StringBuilder();
        for (ResourceChunk c : chunks) {
            String title = c.getResourceTitle() == null ? "课程资料" : c.getResourceTitle();
            sb.append("【").append(title).append("】")
              .append(c.getContent() == null ? "" : c.getContent())
              .append("\n");
        }
        return sb.toString();
    }

    private List<FunctionEntryVO> matchFunctions(String message) {
        if (message == null || message.isBlank()) {
            return Collections.emptyList();
        }
        String lower = message.toLowerCase();
        List<FunctionEntryVO> matched = new ArrayList<>();
        for (FunctionNav nav : FUNCTION_NAVS) {
            for (String kw : nav.keywords) {
                if (lower.contains(kw)) {
                    matched.add(new FunctionEntryVO(nav.name, nav.path, nav.description));
                    break;
                }
            }
        }
        return matched;
    }

    private static final List<FunctionNav> FUNCTION_NAVS = List.of(
            new FunctionNav("我的课程", "/my-courses", "查看已选课程与学习进度", "我的课程", "选课", "已选课程", "课程列表"),
            new FunctionNav("课程学习", "/course/{id}", "进入课程学习视频与课件", "上课", "看视频", "课件", "开始学习", "课程学习"),
            new FunctionNav("知识图谱", "/course/{id}", "查看课程知识图谱", "知识图谱", "图谱", "知识结构", "知识关系"),
            new FunctionNav("作业", "/course/{id}", "查看与提交作业", "作业", "提交", "交作业", "homework"),
            new FunctionNav("测评测验", "/assessment", "能力测评与学习诊断", "诊断", "测评", "测验", "评估", "测试", "考试", "做题"),
            new FunctionNav("能力雷达图", "/radar-chart", "能力维度可视化", "雷达图", "能力分析"),
            new FunctionNav("薄弱知识点", "/weak-points", "查看薄弱知识点", "薄弱", "弱项", "短板"),
            new FunctionNav("个性化推荐", "/recommendations", "个性化资源推荐", "推荐", "资源推荐", "学什么"),
            new FunctionNav("学习计划", "/learning-plan", "制定与查看学习计划", "学习计划", "学习安排", "每天学习", "计划"),
            new FunctionNav("笔记管理", "/notes", "管理学习笔记", "笔记", "记笔记"),
            new FunctionNav("问答社区", "/qa", "进入问答社区提问交流", "问答", "提问", "答疑", "讨论", "交流"),
            new FunctionNav("搜索", "/search", "搜索课程与资料", "搜索", "查找", "检索", "找资料"),
            new FunctionNav("个人中心", "/personal-center", "查看与修改个人信息", "个人中心", "账号", "个人信息", "改密码"),
            new FunctionNav("设置", "/settings", "系统偏好设置", "设置", "偏好")
    );

    private static class FunctionNav {
        final String name;
        final String path;
        final String description;
        final String[] keywords;

        FunctionNav(String name, String path, String description, String... keywords) {
            this.name = name;
            this.path = path;
            this.description = description;
            this.keywords = keywords;
        }
    }

    private String callQwenApi(List<QwenMessage> messages) {
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", messages,
                "temperature", 0.7,
                "max_tokens", 2048
        );

        QwenChatResponse response;
        try {
            String raw = getWebClient().post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            response = objectMapper.readValue(raw, QwenChatResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("调用千问 API 失败: " + e.getMessage(), e);
        }

        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            throw new RuntimeException("千问 API 返回空结果");
        }

        return response.getChoices().get(0).getMessage().getContent();
    }

    // ---- 内部 DTO ----

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class QwenMessage {
        private String role;
        private String content;

        public QwenMessage() {}
        public QwenMessage(String role, String content) { this.role = role; this.content = content; }
        public static QwenMessage system(String content) { return new QwenMessage("system", content); }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class QwenChatResponse {
        private List<Choice> choices;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class Choice {
            private int index;
            private QwenMessage message;
            @JsonProperty("finish_reason")
            private String finishReason;
        }
    }
}
