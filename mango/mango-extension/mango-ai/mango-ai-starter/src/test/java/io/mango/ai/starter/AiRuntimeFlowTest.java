package io.mango.ai.starter;

import io.mango.ai.api.enums.AiApiProtocol;
import io.mango.ai.api.enums.AiCapability;
import io.mango.ai.api.enums.AiPromptStatus;
import io.mango.ai.api.enums.AiServiceType;
import io.mango.ai.api.enums.AiModality;
import io.mango.ai.core.entity.AiInvocationAuditEntity;
import io.mango.ai.core.entity.AiPromptEntity;
import io.mango.ai.core.entity.AiServiceEntity;
import io.mango.ai.core.mapper.AiInvocationAuditMapper;
import io.mango.ai.core.mapper.AiPromptMapper;
import io.mango.ai.core.mapper.AiServiceMapper;
import io.mango.ai.core.mapper.AiSkillMapper;
import io.mango.ai.core.service.AiModelResolution;
import io.mango.ai.core.service.IAiModelManagementService;
import io.mango.ai.core.service.IAiChatConversationStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

/**
 * 通过真实嵌入式 HTTP 服务验证 AI starter 入口。
 */
@Tag("flow")
@Tag("ai")
@SpringBootTest(
        classes = AiHttpContractFlowTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "mango.kv.store.type=redis",
                "spring.autoconfigure.exclude=io.mango.infra.kv.starter.KvStoreAutoConfiguration,"
                        + "io.mango.infra.kv.starter.redis.KvRedisAutoConfiguration,"
                        + "io.mango.infra.kv.starter.KvCapabilityAutoConfiguration,"
                        + "io.mango.infra.persistence.starter.PersistenceDataSourceAutoConfiguration,"
                        + "io.mango.infra.persistence.starter.PersistenceAutoConfiguration,"
                        + "io.mango.infra.persistence.starter.PersistenceFlywayAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
                        + "io.mango.infra.crypto.starter.CryptoAutoConfiguration,"
                        + "io.mango.infra.realtime.starter.MangoRealtimeAutoConfiguration"
        })
class AiRuntimeFlowTest {

    @LocalServerPort
    private int port;

    @MockitoBean
    private IAiModelManagementService modelConfigService;
    @MockitoBean
    private AiServiceMapper serviceMapper;
    @MockitoBean
    private AiPromptMapper promptMapper;
    @MockitoBean
    private AiSkillMapper skillMapper;
    @MockitoBean
    private AiInvocationAuditMapper auditMapper;
    @MockitoBean
    private IAiChatConversationStore conversationStore;

    @BeforeEach
    void setUpModelConfig() {
        when(modelConfigService.resolveChatModel(100L))
                .thenReturn(new AiModelResolution(
                        100L,
                        new AiHttpContractFlowTest.TestChatModel(),
                        "test-provider",
                        "test-model",
                        AiApiProtocol.CHAT_COMPLETIONS,
                        false,
                        Set.of(AiModality.TEXT),
                        Set.of(AiModality.TEXT)));
        AiServiceEntity service = new AiServiceEntity();
        service.setCode("assistant.general");
        service.setServiceType(AiServiceType.CHAT);
        service.setCapability(AiCapability.CHAT);
        service.setEnabled(true);
        service.setPromptId(10L);
        AiPromptEntity prompt = new AiPromptEntity();
        prompt.setStatus(AiPromptStatus.PUBLISHED);
        prompt.setTemplate("你是测试助手");
        when(serviceMapper.selectOne(any())).thenReturn(service);
        when(promptMapper.selectById(10L)).thenReturn(prompt);
        when(auditMapper.insert(any(AiInvocationAuditEntity.class))).thenReturn(1);
        when(conversationStore.load(any(), any(), any(), any(), anyInt()))
                .thenReturn(new IAiChatConversationStore.ConversationState(null, List.of()));
    }

    @Test
    void chat_真实HTTP入口_返回标准Sse事件() {
        WebClient client = WebClient.builder().baseUrl("http://127.0.0.1:" + port).build();

        String body = client.post()
                .uri(uriBuilder -> uriBuilder.path("/ai/services/chat")
                        .queryParam("serviceCode", "assistant.general")
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue("{\"contentParts\":[{\"type\":\"TEXT\",\"text\":\"hello\"}],"
                        + "\"sessionId\":\"runtime-session\","
                        + "\"modelId\":100,\"thinkingEnabled\":false}")
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(5));

        assertTrue(body.contains("data:"), body);
        assertTrue(body.contains("\"type\":\"message\""), body);
        assertTrue(body.contains("\"type\":\"done\""), body);
        assertTrue(body.contains("runtime-session"), body);
    }

}
