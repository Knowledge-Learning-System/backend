package cn.edu.bcu.learning.service;

import cn.edu.bcu.learning.domain.entity.CoursewareResource;
import cn.edu.bcu.learning.domain.entity.ResourceChunk;
import cn.edu.bcu.learning.repository.mysql.ResourceChunkMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * RAG 服务：资料内容索引 + 语义召回。
 */
@Service
@RequiredArgsConstructor
public class RagService {

    private final ResourceChunkMapper resourceChunkMapper;
    private final EmbeddingService embeddingService;
    private final DocumentParserService documentParserService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 召回当前课程下与 query 最相关的 topK 个切块。 */
    public List<ResourceChunk> recall(Integer courseId, String query, int topK) {
        if (courseId == null) {
            return Collections.emptyList();
        }
        float[] queryVec = embeddingService.embed(query);
        if (queryVec == null) {
            return Collections.emptyList();
        }
        List<ResourceChunk> chunks = resourceChunkMapper.selectByCourseId(courseId);
        if (chunks == null || chunks.isEmpty()) {
            return Collections.emptyList();
        }

        List<ScoredChunk> scored = new ArrayList<>();
        for (ResourceChunk chunk : chunks) {
            float[] vec = parseEmbedding(chunk.getEmbedding());
            if (vec == null) {
                continue;
            }
            double sim = cosineSimilarity(queryVec, vec);
            scored.add(new ScoredChunk(chunk, sim));
        }
        scored.sort(Comparator.comparingDouble(ScoredChunk::getSim).reversed());

        List<ResourceChunk> result = new ArrayList<>();
        int limit = Math.min(topK, scored.size());
        for (int i = 0; i < limit; i++) {
            result.add(scored.get(i).getChunk());
        }
        return result;
    }

    /** 上传资料后解析正文、切块、向量化并写入 resource_chunk（失败不影响上传主流程）。 */
    public void indexResource(CoursewareResource resource, File file) {
        try {
            String text = documentParserService.parse(file, resource.getFileType());
            if (text == null || text.isBlank()) {
                return;
            }
            List<String> chunkTexts = documentParserService.splitChunks(text);
            int index = 0;
            for (String chunkText : chunkTexts) {
                float[] vec = embeddingService.embed(chunkText);
                if (vec == null) {
                    continue;
                }
                ResourceChunk chunk = new ResourceChunk();
                chunk.setCourseId(resource.getCourseId());
                chunk.setResourceId(resource.getId());
                chunk.setResourceTitle(resource.getTitle());
                chunk.setKnowledgePointId(resource.getKnowledgePointId());
                chunk.setChunkIndex(index++);
                chunk.setContent(chunkText);
                chunk.setEmbedding(toJson(vec));
                resourceChunkMapper.insert(chunk);
            }
        } catch (Exception ignored) {
            // 索引失败不影响上传
        }
    }

    private float[] parseEmbedding(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, float[].class);
        } catch (Exception e) {
            return null;
        }
    }

    private String toJson(float[] vec) {
        try {
            return objectMapper.writeValueAsString(vec);
        } catch (Exception e) {
            return null;
        }
    }

    private double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length || a.length == 0) {
            return 0d;
        }
        double dot = 0d;
        double na = 0d;
        double nb = 0d;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            na += (double) a[i] * a[i];
            nb += (double) b[i] * b[i];
        }
        if (na == 0d || nb == 0d) {
            return 0d;
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    private static class ScoredChunk {
        private final ResourceChunk chunk;
        private final double sim;

        ScoredChunk(ResourceChunk chunk, double sim) {
            this.chunk = chunk;
            this.sim = sim;
        }

        ResourceChunk getChunk() {
            return chunk;
        }

        double getSim() {
            return sim;
        }
    }
}
