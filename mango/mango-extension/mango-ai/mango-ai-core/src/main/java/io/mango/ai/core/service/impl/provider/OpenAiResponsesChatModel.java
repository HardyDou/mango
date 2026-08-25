package io.mango.ai.core.service.impl.provider;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/** OpenAI Responses API 对 Spring AI {@link ChatModel} 的流式适配。 */
public final class OpenAiResponsesChatModel implements ChatModel {

    private static final ParameterizedTypeReference<ServerSentEvent<String>> SSE_TYPE =
            new ParameterizedTypeReference<>() { };

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String responsesUrl;
    private final String modelName;
    private final ChatOptions defaultOptions;

    public OpenAiResponsesChatModel(String baseUrl, String apiKey, String modelName, ObjectMapper objectMapper) {
        Assert.hasText(baseUrl, "baseUrl must not be blank");
        Assert.hasText(apiKey, "apiKey must not be blank");
        Assert.hasText(modelName, "modelName must not be blank");
        Assert.notNull(objectMapper, "objectMapper must not be null");
        this.responsesUrl = OpenAiCompatibleEndpoint.responsesUrl(baseUrl);
        this.modelName = modelName;
        this.objectMapper = objectMapper.copy();
        this.defaultOptions = ChatOptions.builder().model(modelName).build();
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public ChatOptions getDefaultOptions() {
        return defaultOptions.copy();
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        List<ChatResponse> chunks = stream(prompt).collectList().block();
        if (chunks == null || chunks.isEmpty()) {
            throw new IllegalStateException("Responses API 未返回有效响应");
        }
        StringBuilder content = new StringBuilder();
        ChatResponseMetadata metadata = null;
        for (ChatResponse chunk : chunks) {
            if (chunk.getResult() != null && chunk.getResult().getOutput() != null
                    && hasStreamDelta(chunk.getResult().getOutput().getText())) {
                content.append(chunk.getResult().getOutput().getText());
            }
            if (chunk.getMetadata() != null && chunk.getMetadata().getUsage() != null) {
                metadata = chunk.getMetadata();
            }
        }
        ChatResponseMetadata resolvedMetadata = metadata == null
                ? ChatResponseMetadata.builder().model(modelName).build() : metadata;
        return new ChatResponse(
                List.of(new Generation(new AssistantMessage(content.toString()))),
                resolvedMetadata);
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        Assert.notNull(prompt, "prompt must not be null");
        ResponsesPayload payload = new ResponsesPayload(
                modelName,
                toInput(prompt.getInstructions()),
                true,
                new ResponsesReasoning(reasoningEffort(prompt)));
        return webClient.post()
                .uri(responsesUrl)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(payload)
                .retrieve()
                .bodyToFlux(SSE_TYPE)
                .concatMap(event -> mapEvent(event.data()));
    }

    private List<ResponsesInputMessage> toInput(List<Message> messages) {
        List<ResponsesInputMessage> input = new ArrayList<>(messages.size());
        for (Message message : messages) {
            input.add(new ResponsesInputMessage(role(message.getMessageType()), content(message)));
        }
        return input;
    }

    private Object content(Message message) {
        if (!(message instanceof UserMessage userMessage) || userMessage.getMedia().isEmpty()) {
            return message.getText();
        }
        List<ResponsesInputContent> content = new ArrayList<>(userMessage.getMedia().size() + 1);
        if (StringUtils.hasText(userMessage.getText())) {
            content.add(new ResponsesInputText("input_text", userMessage.getText()));
        }
        for (Media media : userMessage.getMedia()) {
            content.add(mediaContent(media));
        }
        return content;
    }

    private ResponsesInputContent mediaContent(Media media) {
        String contentType = media.getMimeType().toString();
        String dataUrl = "data:" + contentType + ";base64,"
                + Base64.getEncoder().encodeToString(media.getDataAsByteArray());
        if (contentType.startsWith("image/")) {
            return new ResponsesInputImage("input_image", dataUrl, "auto");
        }
        if (MediaType.APPLICATION_PDF_VALUE.equals(contentType)) {
            String fileName = StringUtils.hasText(media.getName()) ? media.getName() : "document.pdf";
            return new ResponsesInputFile("input_file", fileName, dataUrl);
        }
        throw new IllegalArgumentException("Responses API 不支持该附件类型：" + contentType);
    }

    private String role(MessageType messageType) {
        return switch (messageType) {
            case SYSTEM -> "system";
            case USER -> "user";
            case ASSISTANT -> "assistant";
            case TOOL -> throw new IllegalArgumentException("Responses ChatModel 暂不支持工具消息");
        };
    }

    private String reasoningEffort(Prompt prompt) {
        if (prompt.getOptions() instanceof OpenAiChatOptions options
                && StringUtils.hasText(options.getReasoningEffort())) {
            return options.getReasoningEffort();
        }
        return "medium";
    }

    private Flux<ChatResponse> mapEvent(String data) {
        if (!StringUtils.hasText(data)) {
            return Flux.empty();
        }
        JsonNode event = readEvent(data);
        String type = event.path("type").asText();
        if ("response.output_text.delta".equals(type)) {
            String delta = event.path("delta").asText();
            return hasStreamDelta(delta)
                    ? Flux.just(response(delta, ChatResponseMetadata.builder().model(modelName).build()))
                    : Flux.empty();
        }
        if ("response.completed".equals(type)) {
            JsonNode value = event.path("response");
            return Flux.just(response("", completedMetadata(value)));
        }
        if ("error".equals(type) || "response.failed".equals(type)
                || "response.incomplete".equals(type)) {
            return Flux.error(new IllegalStateException(errorMessage(event)));
        }
        return Flux.empty();
    }

    private static boolean hasStreamDelta(String content) {
        return content != null && !content.isEmpty();
    }

    private ChatResponseMetadata completedMetadata(JsonNode response) {
        ChatResponseMetadata.Builder builder = ChatResponseMetadata.builder().model(modelName);
        String id = response.path("id").asText();
        if (StringUtils.hasText(id)) {
            builder.id(id);
        }
        JsonNode usage = response.path("usage");
        if (usage.isObject()) {
            builder.usage(new DefaultUsage(
                    usage.path("input_tokens").asInt(0),
                    usage.path("output_tokens").asInt(0),
                    usage.path("total_tokens").asInt(0),
                    usage));
        }
        return builder.build();
    }

    private ChatResponse response(String content, ChatResponseMetadata metadata) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(content))), metadata);
    }

    private JsonNode readEvent(String data) {
        try {
            return objectMapper.readTree(data);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Responses API 返回了无法解析的事件", exception);
        }
    }

    private String errorMessage(JsonNode event) {
        String message = event.path("error").path("message").asText();
        if (!StringUtils.hasText(message)) {
            message = event.path("response").path("error").path("message").asText();
        }
        return StringUtils.hasText(message) ? "Responses API 调用失败：" + message : "Responses API 调用失败";
    }

    @Getter
    @RequiredArgsConstructor
    private static final class ResponsesPayload {
        private final String model;
        private final List<ResponsesInputMessage> input;
        private final boolean stream;
        private final ResponsesReasoning reasoning;
    }

    @Getter
    @RequiredArgsConstructor
    private static final class ResponsesInputMessage {
        private final String role;
        private final Object content;
    }

    private sealed interface ResponsesInputContent
            permits ResponsesInputText, ResponsesInputImage, ResponsesInputFile { }

    @Getter
    @RequiredArgsConstructor
    private static final class ResponsesInputText implements ResponsesInputContent {
        private final String type;
        private final String text;
    }

    @Getter
    @RequiredArgsConstructor
    private static final class ResponsesInputImage implements ResponsesInputContent {
        private final String type;
        @JsonProperty("image_url")
        private final String imageUrl;
        private final String detail;
    }

    @Getter
    @RequiredArgsConstructor
    private static final class ResponsesInputFile implements ResponsesInputContent {
        private final String type;
        private final String filename;
        @JsonProperty("file_data")
        private final String fileData;
    }

    @Getter
    @RequiredArgsConstructor
    private static final class ResponsesReasoning {
        private final String effort;
    }
}
