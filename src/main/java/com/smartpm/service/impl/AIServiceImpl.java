package com.smartpm.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartpm.common.config.AIConfigProperties;
import com.smartpm.service.AIService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {

    private final AIConfigProperties aiConfig;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private WebClient webClient;

    @PostConstruct
    public void init() {
        this.webClient = webClientBuilder
                .baseUrl(aiConfig.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + aiConfig.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Host", "api.deepseek.com")
                .build();
        log.info("[AI] 初始化完成: baseUrl={}, model={}", aiConfig.getBaseUrl(), aiConfig.getModel());
    }

    @Override
    public Flux<String> streamChat(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return Flux.error(new IllegalArgumentException("prompt 不能为空"));
        }

        Map<String, Object> body = Map.of(
                "model", aiConfig.getModel(),
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "stream", true
        );

        log.info("[AI] 发起流式调用: model={}, promptLength={}", aiConfig.getModel(), prompt.length());

        // Spring WebFlux 的 text/event-stream 解码器已自动剥离 SSE 协议的
        // "data:" 前缀和换行符，每个 chunk 是纯 JSON (如 {"choices":[...]})。
        // 无需任何缓冲区或行分割处理。
        return webClient.post()
                .uri("/v1/chat/completions")
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnSubscribe(s -> log.info("[AI] SSE 订阅已建立"))
                .<String>handle((jsonChunk, sink) -> {
                    String content = extractContentFromJson(jsonChunk);
                    if (content != null && !content.isEmpty()) {
                        sink.next(content);
                    }
                })
                .doOnNext(content -> log.info("[AI] 发射 token ({} chars): {}",
                        content.length(),
                        content.length() > 60 ? content.substring(0, 60) + "..." : content))
                .doOnComplete(() -> log.info("[AI] 流式调用完成"))
                .doOnError(err -> log.error("[AI] 流式调用异常: {}", err.getMessage(), err));
    }

    /**
     * 从 SSE 解码后的纯 JSON chunk 中提取文本。
     * Spring WebFlux 已将 "data:" 前缀和 "\n" 剥离，chunk 直接是 JSON。
     * 兼容 content 和 reasoning_content 两种字段（DeepSeek 推理模型用后者）。
     */
    private String extractContentFromJson(String jsonChunk) {
        try {
            if (jsonChunk == null || jsonChunk.isBlank() || jsonChunk.contains("[DONE]")) {
                return null;
            }
            JsonNode root = objectMapper.readTree(jsonChunk);
            JsonNode choices = root.path("choices");
            if (choices.isEmpty()) return null;
            JsonNode delta = choices.get(0).path("delta");
            if (delta.isEmpty() || delta.isMissingNode()) return null;

            // 优先取 content，为空则取 reasoning_content (DeepSeek 推理模型)
            JsonNode contentNode = delta.path("content");
            String text = (contentNode != null && !contentNode.isNull()) ? contentNode.asText() : null;
            if (text == null || text.isEmpty()) {
                JsonNode reasoningNode = delta.path("reasoning_content");
                text = (reasoningNode != null && !reasoningNode.isNull()) ? reasoningNode.asText() : null;
            }
            return (text != null && !text.isEmpty()) ? text : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String chat(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("prompt 不能为空");
        }

        Map<String, Object> body = Map.of(
                "model", aiConfig.getModel(),
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "stream", false
        );

        log.info("[AI] 发起非流式调用: model={}, promptLength={}", aiConfig.getModel(), prompt.length());

        try {
            String response = webClient.post()
                    .uri("/v1/chat/completions")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode choices = root.path("choices");
            if (!choices.isEmpty()) {
                JsonNode content = choices.get(0).path("message").path("content");
                if (!content.isNull() && !content.isMissingNode()) {
                    String text = content.asText();
                    log.info("[AI] 非流式响应: {} chars", text.length());
                    return text;
                }
            }
            log.warn("[AI] 非流式响应的 choices 为空");
            return "";
        } catch (Exception e) {
            log.error("[AI] 非流式调用失败: {}", e.getMessage(), e);
            throw new RuntimeException("AI 调用失败: " + e.getMessage(), e);
        }
    }
}
