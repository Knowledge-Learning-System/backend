package cn.edu.bcu.learning.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * 文本向量化服务
 * 复用 qwen.api-key / qwen.base-url，调用阿里云百炼 text-embedding-v3。
 */
@Service
public class EmbeddingService {

    @Value("${qwen.api-key:}")
    private String apiKey;

    @Value("${qwen.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    private static final String EMBEDDING_MODEL = "text-embedding-v3";

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

    /**
     * 将文本转为向量；未配置 key 或调用失败时返回 null。
     */
    public float[] embed(String text) {
        if (apiKey == null || apiKey.isBlank() || text == null || text.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> body = Map.of(
                    "model", EMBEDDING_MODEL,
                    "input", text
            );
            String raw = getWebClient().post()
                    .uri("/embeddings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            EmbeddingResponse response = objectMapper.readValue(raw, EmbeddingResponse.class);
            if (response == null || response.getData() == null || response.getData().isEmpty()) {
                return null;
            }
            List<Double> list = response.getData().get(0).getEmbedding();
            if (list == null || list.isEmpty()) {
                return null;
            }
            float[] arr = new float[list.size()];
            for (int i = 0; i < list.size(); i++) {
                arr[i] = list.get(i).floatValue();
            }
            return arr;
        } catch (Exception e) {
            return null;
        }
    }

    // ---- 内部 DTO ----

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class EmbeddingResponse {
        private List<EmbeddingData> data;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class EmbeddingData {
        private List<Double> embedding;
    }
}
