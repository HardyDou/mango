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
import io.mango.infra.kv.api.ICache;
import io.mango.infra.kv.api.ILocker;
import io.mango.infra.realtime.api.RealtimeApi;
import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;
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
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

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
    @MockitoBean
    private ICache cache;
    @MockitoBean
    private ILocker locker;
    @MockitoBean
    private RealtimeApi realtimeApi;

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
        when(conversationStore.load(any(), anyInt()))
                .thenReturn(new IAiChatConversationStore.ConversationState(List.of()));
        when(locker.tryLock(any(), anyLong())).thenReturn(true);
        when(cache.exists(any())).thenReturn(false);
    }

    @Test
    void chat_真实HTTP入口_返回标准受理结果并发布Realtime事件() {
        WebClient client = WebClient.builder().baseUrl("http://127.0.0.1:" + port).build();
        String requestId = "ab5f3f7d-4f62-4e31-a318-c9518a454c2c";

        String body = client.post()
                .uri(uriBuilder -> uriBuilder.path("/ai/services/chat")
                        .queryParam("serviceCode", "assistant.general")
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue("{\"contentParts\":[{\"type\":\"TEXT\",\"text\":\"hello\"}],"
                        + "\"sessionId\":\"runtime-session\","
                        + "\"requestId\":\"" + requestId + "\","
                        + "\"modelId\":100,\"thinkingEnabled\":false}")
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(5));

        assertTrue(body.contains("\"success\":true"), body);
        assertTrue(body.contains(requestId), body);
        assertTrue(body.contains("runtime-session"), body);
        ArgumentCaptor<RealtimeOutboundMessage> captor = ArgumentCaptor.forClass(RealtimeOutboundMessage.class);
        verify(realtimeApi, timeout(3000).atLeast(2)).publish(captor.capture());
        assertTrue(captor.getAllValues().stream()
                .anyMatch(message -> message.content().contains("\"type\":\"done\"")));
    }

}
