package io.mango.ai.core.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek 流式对话默认实现。
 */
public class DeepSeekProvider implements IAiProvider {

    private static final Logger LOG = LoggerFactory.getLogger(DeepSeekProvider.class);
    private static final int THINKING_MAX_TOKENS = 2000;
    private static final int SSE_PREFIX_LENGTH = 5;

    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final String model;
    private final Duration readTimeout;

    /**
     * 创建 DeepSeek provider。
     *
     * @param objectMapper JSON 序列化器
     * @param webClient 已配置地址与认证头的客户端
     * @param model 模型名称
     * @param readTimeout 流式读取超时
     */
    public DeepSeekProvider(
            ObjectMapper objectMapper, WebClient webClient, String model, Duration readTimeout) {
        this.objectMapper = objectMapper.copy();
        this.webClient = webClient.mutate().build();
        this.model = model;
        this.readTimeout = readTimeout;
    }

    @Override
    public Flux<String> chat(List<Map<String, String>> messages, boolean enableThinking) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("stream", true);
        if (enableThinking) {
            requestBody.put("max_tokens", THINKING_MAX_TOKENS);
        }
        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(readTimeout)
                .<String>handle((data, sink) -> {
                    String event = parseEvent(data, enableThinking);
                    if (event != null) {
                        sink.next(event);
                    }
                })
                .doOnError(error -> LOG.error("DeepSeek API call failed", error));
    }

    private String parseEvent(String source, boolean enableThinking) {
        String data = source;
        if (source.startsWith("data:")) {
            data = source.substring(SSE_PREFIX_LENGTH).trim();
        }
        if ("[DONE]".equals(data)) {
            return null;
        }
        try {
            JsonNode delta = objectMapper.readTree(data).path("choices").path(0).path("delta");
            String thinking = delta.path("thinking").asText();
            if (enableThinking && !thinking.isEmpty()) {
                return jsonEvent("thinking", thinking);
            }
            String content = delta.path("content").asText();
            if (content.isEmpty()) {
                return null;
            }
            return jsonEvent("message", content);
        } catch (JsonProcessingException exception) {
            LOG.warn("Failed to parse DeepSeek stream event", exception);
            return jsonEvent("error", "Failed to parse response");
        }
    }

    private String jsonEvent(String type, String content) {
        try {
            return objectMapper.writeValueAsString(Map.of("type", type, "content", content));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize AI stream event", exception);
        }
    }
}
