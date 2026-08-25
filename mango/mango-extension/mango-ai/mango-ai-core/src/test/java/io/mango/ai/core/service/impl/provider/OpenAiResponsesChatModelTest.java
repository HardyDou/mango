package io.mango.ai.core.service.impl.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.util.MimeTypeUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OpenAiResponsesChatModelTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void streamsTextAndUsageThroughSpringAiContract() throws Exception {
        AtomicReference<JsonNode> requestBody = new AtomicReference<>();
        startServer(requestBody, "OK");
        OpenAiResponsesChatModel model = new OpenAiResponsesChatModel(
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "test-secret",
                "responses-model",
                objectMapper);

        OpenAiChatOptions options = OpenAiChatOptions.builder().reasoningEffort("medium").build();
        List<ChatResponse> responses = model.stream(new Prompt(List.of(
                        new SystemMessage("Answer briefly."),
                        new UserMessage("Reply OK")), options))
                .collectList()
                .block(Duration.ofSeconds(5));

        assertNotNull(responses);
        assertEquals("OK", responses.get(0).getResult().getOutput().getText());
        assertEquals(8, responses.get(1).getMetadata().getUsage().getPromptTokens());
        assertEquals(5, responses.get(1).getMetadata().getUsage().getCompletionTokens());
        assertEquals("system", requestBody.get().path("input").get(0).path("role").asText());
        assertEquals("user", requestBody.get().path("input").get(1).path("role").asText());
        assertEquals(true, requestBody.get().path("stream").asBoolean());
        assertEquals("medium", requestBody.get().path("reasoning").path("effort").asText());
    }

    @Test
    void sendsNoneWhenThinkingIsDisabled() throws Exception {
        AtomicReference<JsonNode> requestBody = new AtomicReference<>();
        startServer(requestBody, "OK");
        OpenAiResponsesChatModel model = new OpenAiResponsesChatModel(
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "test-secret",
                "responses-model",
                objectMapper);
        OpenAiChatOptions options = OpenAiChatOptions.builder().reasoningEffort("none").build();

        model.stream(new Prompt(List.of(new UserMessage("Reply OK")), options))
                .collectList()
                .block(Duration.ofSeconds(5));

        assertEquals("none", requestBody.get().path("reasoning").path("effort").asText());
    }

    @Test
    void preservesWhitespaceOnlyStreamingDeltas() throws Exception {
        AtomicReference<JsonNode> requestBody = new AtomicReference<>();
        startServer(requestBody, "#", " ", "标题", "\n\n", "-", " ", "列表");
        OpenAiResponsesChatModel model = new OpenAiResponsesChatModel(
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "test-secret",
                "responses-model",
                objectMapper);

        ChatResponse response = model.call(new Prompt(new UserMessage("Reply with Markdown")));

        assertEquals("# 标题\n\n- 列表", response.getResult().getOutput().getText());
    }

    @Test
    void mapsImageAndPdfToResponsesContentParts() throws Exception {
        AtomicReference<JsonNode> requestBody = new AtomicReference<>();
        startServer(requestBody, "OK");
        OpenAiResponsesChatModel model = new OpenAiResponsesChatModel(
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "test-secret",
                "responses-model",
                objectMapper);
        Media image = Media.builder()
                .mimeType(MimeTypeUtils.IMAGE_PNG)
                .data(new ByteArrayResource(new byte[] {1, 2, 3}))
                .name("screen.png")
                .build();
        Media pdf = Media.builder()
                .mimeType(MimeTypeUtils.parseMimeType(MediaType.APPLICATION_PDF_VALUE))
                .data(new ByteArrayResource(new byte[] {4, 5, 6}))
                .name("contract.pdf")
                .build();

        model.stream(new Prompt(UserMessage.builder().text("分析附件").media(image, pdf).build()))
                .collectList()
                .block(Duration.ofSeconds(5));

        JsonNode content = requestBody.get().path("input").get(0).path("content");
        assertEquals("input_text", content.get(0).path("type").asText());
        assertEquals("分析附件", content.get(0).path("text").asText());
        assertEquals("input_image", content.get(1).path("type").asText());
        assertEquals("data:image/png;base64,AQID", content.get(1).path("image_url").asText());
        assertEquals("input_file", content.get(2).path("type").asText());
        assertEquals("contract.pdf", content.get(2).path("filename").asText());
        assertEquals("data:application/pdf;base64,BAUG", content.get(2).path("file_data").asText());
    }

    private void startServer(AtomicReference<JsonNode> requestBody, String... deltas) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/responses", exchange -> {
            requestBody.set(objectMapper.readTree(exchange.getRequestBody()));
            assertEquals("Bearer test-secret", exchange.getRequestHeaders().getFirst("Authorization"));
            StringBuilder stream = new StringBuilder();
            for (String delta : deltas) {
                stream.append("event: response.output_text.delta\n")
                        .append("data: ")
                        .append(objectMapper.createObjectNode()
                                .put("type", "response.output_text.delta")
                                .put("delta", delta))
                        .append("\n\n");
            }
            stream.append("event: response.completed\n")
                    .append("data: {\"type\":\"response.completed\",\"response\":{\"id\":\"resp-1\","
                            + "\"usage\":{\"input_tokens\":8,\"output_tokens\":5,\"total_tokens\":13}}}\n\n");
            byte[] response = stream.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
    }
}
