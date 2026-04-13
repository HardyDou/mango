package io.mango.ai.core.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek AI provider for chat completions
 * <p>
 * Handles SSE streaming responses from DeepSeek API.
 *
 * @author Mango
 */
@Slf4j
@Component
public class DeepSeekProvider {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${mango.ai.deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${mango.ai.deepseek.api-key:}")
    private String apiKey;

    @Value("${mango.ai.deepseek.model:deepseek-chat}")
    private String model;

    @Value("${mango.ai.deepseek.connect-timeout:10000}")
    private int connectTimeout;

    @Value("${mango.ai.deepseek.read-timeout:60000}")
    private int readTimeout;

    @Autowired
    public DeepSeekProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
        this.webClient = WebClient.builder()
                .exchangeStrategies(strategies)
                .build();
    }

    /**
     * Chat completion with SSE streaming
     *
     * @param messages        conversation messages
     * @param sessionId      session identifier
     * @param enableThinking  enable thinking mode
     * @return SSE event flux
     */
    public Flux<String> chat(List<Map<String, String>> messages, String sessionId, boolean enableThinking) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("stream", true);
        if (enableThinking) {
            requestBody.put("max_tokens", 2000);
        }

        final boolean enableThinkingFinal = enableThinking;
        return webClient.post()
                .uri(baseUrl + "/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofMillis(readTimeout))
                .map(data -> parseSseEvent(data, enableThinkingFinal))
                .doOnError(e -> log.error("DeepSeek API call failed", e));
    }

    /**
     * Parse SSE event from DeepSeek response
     */
    private String parseSseEvent(String data, boolean enableThinking) {
        try {
            // DeepSeek SSE format: data: {"choices":[{"delta":{"content":"..."}}]}
            if (data.startsWith("data:")) {
                data = data.substring(5).trim();
            }

            if ("[DONE]".equals(data)) {
                return "data: {\"type\":\"done\",\"sessionId\":\"" + "" + "\"}";
            }

            JsonNode node = objectMapper.readTree(data);
            JsonNode choices = node.path("choices");

            if (choices.isArray() && !choices.isEmpty()) {
                JsonNode delta = choices.get(0).path("delta");

                // Check for thinking content
                JsonNode thinkingContent = delta.path("thinking");
                if (!thinkingContent.isMissingNode() && enableThinking) {
                    String thinking = thinkingContent.asText();
                    if (!thinking.isEmpty()) {
                        return "data: {\"type\":\"thinking\",\"content\":\"" + escapeJson(thinking) + "\"}";
                    }
                }

                // Check for regular content
                JsonNode content = delta.path("content");
                if (!content.isMissingNode()) {
                    String text = content.asText();
                    if (!text.isEmpty()) {
                        return "data: {\"type\":\"message\",\"content\":\"" + escapeJson(text) + "\"}";
                    }
                }
            }

            return null;
        } catch (Exception e) {
            log.warn("Failed to parse SSE event: {}", data, e);
            return "data: {\"type\":\"error\",\"message\":\"Failed to parse response\"}";
        }
    }

    /**
     * Escape JSON string for embedding in SSE JSON payload
     * Order: escape quotes first, then backslashes
     */
    private String escapeJson(String text) {
        return text.replace("\"", "\\\"")
                .replace("\\", "\\\\");
    }

    /**
     * Validate message for prompt injection patterns
     */
    public boolean containsPromptInjection(String message) {
        // Basic prompt injection detection
        String lower = message.toLowerCase();
        return lower.contains("ignore previous instructions") ||
                lower.contains("ignore all previous") ||
                lower.contains("disregard your instructions") ||
                lower.contains("you are now") ||
                lower.contains("pretend you are") ||
                lower.contains("as an ai");
    }
}
