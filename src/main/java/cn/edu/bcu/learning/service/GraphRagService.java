package cn.edu.bcu.learning.service;

import cn.edu.bcu.learning.domain.dto.GraphRagRequest;
import cn.edu.bcu.learning.domain.entity.Course;
import cn.edu.bcu.learning.domain.entity.ResourceChunk;
import cn.edu.bcu.learning.domain.vo.GraphRagResponseVO;
import cn.edu.bcu.learning.domain.vo.GraphRagSourceVO;
import cn.edu.bcu.learning.repository.mysql.CourseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.stream.Collectors;

/**
 * GraphRAG 问答服务（导航栏「问答」功能专用，独立于 AiService /ai/chat 对话）。
 *
 * 完整流程：
 *   1. 课程定位：由 courseId 直接定位；否则基于问题对所有课程图谱做词法匹配自动识别
 *   2. 知识点定位：词法粗筛候选 + Embedding 语义精排，选出种子知识点
 *   3. 知识子图检索：从 Neo4j 取种子知识点的 1 跳图谱关系（前置/后续/父子/所属章节）
 *   4. 资源召回：复用 RagService 对课程资料做向量召回
 *   5. Prompt 构建 + 千问生成，返回可溯源回答
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GraphRagService {

    private final Neo4jClient neo4jClient;
    private final CourseMapper courseMapper;
    private final RagService ragService;
    private final EmbeddingService embeddingService;

    @Value("${qwen.api-key:}")
    private String apiKey;

    @Value("${qwen.model:qwen-plus}")
    private String model;

    @Value("${qwen.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    /** 词法粗筛保留候选数 */
    private static final int TOP_CANDIDATES = 12;
    /** 最终种子知识点数 */
    private static final int TOP_SEEDS = 5;
    /** 资源向量召回条数 */
    private static final int RAG_TOP_K = 5;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private WebClient webClient;

    // ============================================================
    // 对外入口
    // ============================================================

    public GraphRagResponseVO chat(GraphRagRequest req) {
        GraphRagResponseVO vo = new GraphRagResponseVO();
        String question = req.getQuestion();
        if (question == null || question.isBlank()) {
            vo.setAnswer("请输入问题后再提问。");
            vo.setThinking("问题为空");
            vo.setPromptBuilt("");
            return vo;
        }
        if (apiKey == null || apiKey.isBlank()) {
            vo.setAnswer("GraphRAG 问答服务尚未配置千问 API Key，请联系管理员在 application-dev.yml 中设置 qwen.api-key。");
            vo.setThinking("未配置 qwen.api-key");
            vo.setPromptBuilt("");
            return vo;
        }
        StringBuilder thinking = new StringBuilder();

        // ---- 1. 课程定位 ----
        String source = null;
        String courseLabel = null;
        if (req.getCourseId() != null) {
            Course c = courseMapper.selectById(req.getCourseId());
            if (c != null && c.getSource() != null) {
                source = c.getSource();
                courseLabel = "指定课程《" + c.getName() + "》";
            }
        }
        if (source == null) {
            AutoCourse ac = autoLocateCourse(question);
            if (ac != null) {
                source = ac.source;
                courseLabel = "自动识别课程《" + ac.courseName + "》";
            }
        }
        if (source == null) {
            Course first = courseMapper.selectList(
                            new LambdaQueryWrapper<Course>().eq(Course::getStatus, 1).last("LIMIT 1"))
                    .stream().findFirst().orElse(null);
            if (first != null && first.getSource() != null) {
                source = first.getSource();
                courseLabel = "默认课程《" + first.getName() + "》";
            }
        }
        if (source == null) {
            vo.setAnswer("未找到可用的课程知识图谱，请先在系统中创建课程。");
            vo.setThinking("无课程可检索");
            vo.setPromptBuilt("");
            return vo;
        }
        thinking.append("课程定位：").append(courseLabel).append("；");

        // ---- 2. 知识点定位（词法粗筛 + 语义精排） ----
        List<KpNode> nodes = loadCourseNodes(source);
        if (nodes.isEmpty()) {
            vo.setAnswer("当前课程《" + courseLabel + "》知识图谱中暂无知识点数据，无法进行图谱检索问答。");
            vo.setThinking("图谱节点为空");
            vo.setPromptBuilt("");
            return vo;
        }
        List<ScoredKp> seeds = locateSeeds(question, nodes);
        if (seeds.isEmpty()) {
            vo.setAnswer("未能从当前课程知识图谱中检索到与问题相关的知识点。可尝试更换更具体的关键词，例如知识点名称。");
            vo.setThinking("图谱检索未命中");
            vo.setPromptBuilt("");
            return vo;
        }
        thinking.append("命中知识点：")
                .append(seeds.stream().map(s -> s.node.name).collect(Collectors.joining("、")))
                .append("；");

        // ---- 3. 知识子图检索 ----
        SubGraphContext sub = buildSubGraphContext(source, seeds);

        // ---- 4. 资源向量召回 ----
        String ragContext = buildRagContext(source, question);

        // ---- 5. Prompt 构建 ----
        String prompt = buildPrompt(question, seeds, sub, ragContext, req.getHistory());

        // ---- 6. 千问生成 ----
        String answer;
        try {
            answer = callQwen(prompt, req.getHistory(), question);
        } catch (Exception e) {
            log.warn("GraphRAG 调用千问失败: {}", e.getMessage());
            vo.setAnswer("抱歉，AI 服务暂时不可用，请稍后重试。（" + e.getMessage() + "）");
            vo.setThinking(thinking.toString());
            vo.setPromptBuilt(prompt);
            return vo;
        }

        vo.setAnswer(answer);
        vo.setSources(buildSources(seeds));
        vo.setThinking(thinking.toString());
        vo.setPromptBuilt(prompt);
        return vo;
    }

    // ============================================================
    // 1. 课程定位
    // ============================================================

    private String findSourceByCourseId(Long courseId) {
        Course c = courseMapper.selectById(courseId);
        return c == null ? null : c.getSource();
    }

    /** 基于问题对所有课程图谱做轻量词法匹配，返回命中最高的一门课 */
    private AutoCourse autoLocateCourse(String question) {
        List<Course> courses = courseMapper.selectList(
                new LambdaQueryWrapper<Course>().eq(Course::getStatus, 1));
        AutoCourse best = null;
        double bestScore = 0;
        for (Course c : courses) {
            if (c.getSource() == null) continue;
            double score = courseLexicalScore(question, c.getSource());
            if (score > bestScore) {
                bestScore = score;
                best = new AutoCourse(c.getSource(), c.getName());
            }
        }
        return best;
    }

    /** 对某门课的全部知识点名称做词法命中，返回累计分数 */
    private double courseLexicalScore(String question, String source) {
        Collection<Map<String, Object>> rows = neo4jClient.query(
                        "MATCH (n:KnowledgePoint {source: $source}) RETURN n.name AS name")
                .bind(source).to("source")
                .fetch()
                .all();
        double score = 0;
        for (Map<String, Object> row : rows) {
            Object name = row.get("name");
            if (name == null) continue;
            score += lexicalScore(question, String.valueOf(name));
        }
        return score;
    }

    // ============================================================
    // 2. 知识点定位
    // ============================================================

    private List<KpNode> loadCourseNodes(String source) {
        Collection<Map<String, Object>> rows = neo4jClient.query(
                        "MATCH (n:KnowledgePoint {source: $source}) " +
                        "RETURN n.id AS id, n.name AS name, coalesce(n.description,'') AS description")
                .bind(source).to("source")
                .fetch()
                .all();
        List<KpNode> nodes = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            if (row.get("id") == null || row.get("name") == null) continue;
            nodes.add(new KpNode(
                    String.valueOf(row.get("id")),
                    String.valueOf(row.get("name")),
                    row.get("description") == null ? "" : String.valueOf(row.get("description"))));
        }
        return nodes;
    }

    /** 词法粗筛 topN → 语义精排 topK，返回种子知识点 */
    private List<ScoredKp> locateSeeds(String question, List<KpNode> nodes) {
        // 词法粗筛
        List<ScoredKp> lexical = new ArrayList<>();
        for (KpNode n : nodes) {
            double nameScore = lexicalScore(question, n.name);
            double descScore = n.description.isBlank() ? 0 : lexicalScore(question, n.description) * 0.5;
            double score = nameScore + descScore;
            if (score > 0) {
                lexical.add(new ScoredKp(n, score));
            }
        }
        lexical.sort((a, b) -> Double.compare(b.score, a.score));
        if (lexical.isEmpty()) return Collections.emptyList();
        List<ScoredKp> candidates = lexical.size() > TOP_CANDIDATES
                ? lexical.subList(0, TOP_CANDIDATES) : lexical;

        // 语义精排（embedding 不可用时退回词法排序）
        float[] qv = embeddingService.embed(question);
        if (qv == null) {
            List<ScoredKp> top = candidates.size() > TOP_SEEDS ? candidates.subList(0, TOP_SEEDS) : candidates;
            normalizeScores(top);
            return top;
        }
        List<ScoredKp> scored = new ArrayList<>();
        for (ScoredKp c : candidates) {
            float[] nv = embeddingService.embed(c.node.name + " " + c.node.description);
            double semantic = 0;
            if (nv != null) {
                semantic = cosine(qv, nv);
            }
            // 词法归一化(0~1) + 语义余弦，加权合并
            double lexicalNorm = c.score / (maxLexical(candidates) + 1e-9);
            scored.add(new ScoredKp(c.node, 0.4 * lexicalNorm + 0.6 * semantic));
        }
        scored.sort((a, b) -> Double.compare(b.score, a.score));
        List<ScoredKp> top = scored.size() > TOP_SEEDS ? scored.subList(0, TOP_SEEDS) : scored;
        normalizeScores(top);
        return top;
    }

    private double maxLexical(List<ScoredKp> list) {
        double m = 0;
        for (ScoredKp s : list) {
            if (s.score > m) m = s.score;
        }
        return m;
    }

    /** 将种子分数归一化到 0~100 便于前端展示 */
    private void normalizeScores(List<ScoredKp> list) {
        double m = maxLexical(list);
        if (m <= 0) {
            for (ScoredKp s : list) s.score = 100.0 / Math.max(list.size(), 1);
            return;
        }
        for (ScoredKp s : list) {
            s.score = Math.round(s.score / m * 1000) / 10.0;
        }
    }

    /** 词法匹配打分：全名命中 + n-gram 命中 + 英文词命中 */
    private double lexicalScore(String question, String target) {
        if (question == null || question.isBlank() || target == null || target.isBlank()) {
            return 0;
        }
        String q = question.toLowerCase(Locale.ROOT);
        String t = target.toLowerCase(Locale.ROOT);
        double score = 0;
        if (q.contains(t)) {
            score += 100;
        }
        // n-gram（2~4 字）命中比例
        int hit = 0, total = 0;
        int maxLen = Math.min(4, t.length());
        for (int len = 2; len <= maxLen; len++) {
            for (int i = 0; i + len <= t.length(); i++) {
                total++;
                if (q.contains(t.substring(i, i + len))) hit++;
            }
        }
        if (total > 0) {
            score += (double) hit / total * 60;
        }
        // 英文词匹配
        String[] words = t.split("[^a-zA-Z0-9]+");
        int wHit = 0, wTotal = 0;
        for (String w : words) {
            if (w.length() >= 2) {
                wTotal++;
                if (q.contains(w)) wHit++;
            }
        }
        if (wTotal > 0) {
            score += (double) wHit / wTotal * 60;
        }
        return score;
    }

    private double cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || a.length != b.length) return 0;
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) return 0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    // ============================================================
    // 3. 知识子图检索
    // ============================================================

    private SubGraphContext buildSubGraphContext(String source, List<ScoredKp> seeds) {
        SubGraphContext ctx = new SubGraphContext();
        for (ScoredKp s : seeds) {
            String id = s.node.id;
            // 所属章节
            String chapter = fetchChapter(id);
            // 1 跳图谱邻居（PREREQUISITE / PARENT_KP）
            List<String> prereqs = new ArrayList<>();
            List<String> followups = new ArrayList<>();
            List<String> parents = new ArrayList<>();
            List<String> children = new ArrayList<>();
            for (Map<String, Object> row : fetchNeighbors(id, source)) {
                Object name = row.get("name");
                if (name == null) continue;
                String rel = String.valueOf(row.get("rel"));
                if ("PREREQUISITE".equals(rel)) prereqs.add(String.valueOf(name));
                else if ("PARENT_KP".equals(rel)) children.add(String.valueOf(name));
                else followups.add(String.valueOf(name));
            }
            ctx.entries.add(new SubGraphEntry(s.node, chapter, prereqs, followups, parents, children));
        }
        return ctx;
    }

    private String fetchChapter(String id) {
        Map<String, Object> row = neo4jClient.query(
                        "MATCH (n:KnowledgePoint {id: $id})<-[:PART_OF]-(st:SubTopic) " +
                        "RETURN st.name AS name LIMIT 1")
                .bind(id).to("id")
                .fetch()
                .one()
                .orElse(null);
        if (row == null || row.get("name") == null) return "";
        return String.valueOf(row.get("name"));
    }

    private Collection<Map<String, Object>> fetchNeighbors(String id, String source) {
        return neo4jClient.query(
                        "MATCH (n:KnowledgePoint {id: $id, source: $source})-[r:PREREQUISITE|PARENT_KP]->(nb:KnowledgePoint {source: $source}) " +
                        "RETURN nb.name AS name, type(r) AS rel")
                .bind(id).to("id")
                .bind(source).to("source")
                .fetch()
                .all();
    }

    // ============================================================
    // 4. 资源向量召回
    // ============================================================

    private String buildRagContext(String source, String question) {
        Course c = courseMapper.selectOne(new LambdaQueryWrapper<Course>().eq(Course::getSource, source));
        if (c == null) return "（无）";
        try {
            List<ResourceChunk> chunks = ragService.recall(c.getId().intValue(), question, RAG_TOP_K);
            if (chunks == null || chunks.isEmpty()) return "（无）";
            StringBuilder sb = new StringBuilder();
            for (ResourceChunk chunk : chunks) {
                String title = chunk.getResourceTitle() == null ? "课程资料" : chunk.getResourceTitle();
                sb.append("【").append(title).append("】")
                        .append(chunk.getContent() == null ? "" : chunk.getContent())
                        .append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("GraphRAG 资源召回失败: {}", e.getMessage());
            return "（无）";
        }
    }

    // ============================================================
    // 5. Prompt 构建
    // ============================================================

    private String buildPrompt(String question, List<ScoredKp> seeds, SubGraphContext sub,
                               String ragContext, List<GraphRagRequest.HistoryMsg> history) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是高校个性化在线学习系统的课程知识问答助手，基于「知识图谱 GraphRAG 检索」模式回答问题。\n");
        sb.append("规则：\n");
        sb.append("1. 优先依据下面给出的【知识子图】和【课程资料】回答，明确引用其中的知识点名称、所属章节与知识点关系；\n");
        sb.append("2. 回答应有条理，可用 Markdown 结构化（标题、列表）；在回答末尾以「依据：」列出主要引用的知识点；\n");
        sb.append("3. 若图谱与资料不足以回答，可结合领域知识作答，但须注明「该部分基于领域知识补充，图谱中未完全覆盖」；\n");
        sb.append("4. 若问题与课程无关，温和说明并引导回学习主题；\n");
        sb.append("5. 全程使用中文。\n\n");

        sb.append("## 检索到的知识子图\n");
        for (SubGraphEntry e : sub.entries) {
            sb.append("### 知识点：").append(e.node.name).append("\n");
            if (!e.chapter.isEmpty()) {
                sb.append("- 所属章节：").append(e.chapter).append("\n");
            }
            if (!e.node.description.isEmpty()) {
                sb.append("- 概念：").append(e.node.description).append("\n");
            }
            if (!e.prereqs.isEmpty()) {
                sb.append("- 前置知识点：").append(String.join("、", e.prereqs)).append("\n");
            }
            if (!e.followups.isEmpty()) {
                sb.append("- 后续知识点：").append(String.join("、", e.followups)).append("\n");
            }
            if (!e.children.isEmpty()) {
                sb.append("- 子知识点：").append(String.join("、", e.children)).append("\n");
            }
            sb.append("\n");
        }

        sb.append("## 相关课程资料片段\n").append(ragContext).append("\n\n");

        if (history != null && !history.isEmpty()) {
            sb.append("## 对话历史\n");
            for (GraphRagRequest.HistoryMsg m : history) {
                String role = "user".equals(m.getRole()) ? "用户" : "助手";
                sb.append(role).append("：").append(m.getContent()).append("\n");
            }
            sb.append("\n");
        }

        sb.append("## 用户问题\n").append(question);
        return sb.toString();
    }

    // ============================================================
    // 6. 千问生成
    // ============================================================

    private String callQwen(String systemPrompt, List<GraphRagRequest.HistoryMsg> history, String question) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(message("system", systemPrompt));
        if (history != null) {
            for (GraphRagRequest.HistoryMsg h : history) {
                if (h.getRole() == null || h.getContent() == null) continue;
                String role = h.getRole().trim().toLowerCase(Locale.ROOT);
                if (!"user".equals(role) && !"assistant".equals(role)) continue;
                messages.add(message(role, h.getContent()));
            }
        }
        messages.add(message("user", question));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", 0.4);
        body.put("max_tokens", 2048);

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

    private Map<String, Object> message(String role, String content) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("role", role);
        m.put("content", content);
        return m;
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
    // 结果组装
    // ============================================================

    private List<GraphRagSourceVO> buildSources(List<ScoredKp> seeds) {
        List<GraphRagSourceVO> list = new ArrayList<>();
        for (ScoredKp s : seeds) {
            GraphRagSourceVO vo = new GraphRagSourceVO();
            vo.setId(s.node.id);
            vo.setName(s.node.name);
            vo.setChapter(fetchChapter(s.node.id));
            vo.setRelation("命中知识点");
            vo.setScore(s.score);
            list.add(vo);
        }
        return list;
    }

    // ============================================================
    // 内部数据结构
    // ============================================================

    @Data
    static class AutoCourse {
        final String source;
        final String courseName;
        AutoCourse(String source, String courseName) {
            this.source = source;
            this.courseName = courseName;
        }
    }

    @Data
    static class KpNode {
        final String id;
        final String name;
        final String description;
        KpNode(String id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
        }
    }

    static class ScoredKp {
        final KpNode node;
        double score;
        ScoredKp(KpNode node, double score) {
            this.node = node;
            this.score = score;
        }
    }

    static class SubGraphContext {
        final List<SubGraphEntry> entries = new ArrayList<>();
    }

    static class SubGraphEntry {
        final KpNode node;
        final String chapter;
        final List<String> prereqs;
        final List<String> followups;
        final List<String> parents;
        final List<String> children;
        SubGraphEntry(KpNode node, String chapter, List<String> prereqs, List<String> followups,
                      List<String> parents, List<String> children) {
            this.node = node;
            this.chapter = chapter;
            this.prereqs = prereqs;
            this.followups = followups;
            this.parents = parents;
            this.children = children;
        }
    }

    // ============================================================
    // 千问响应 DTO
    // ============================================================

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
