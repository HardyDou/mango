package io.mango.ai.starter;

import io.mango.ai.api.enums.AiApiProtocol;
import io.mango.ai.api.enums.AiCapability;
import io.mango.ai.api.enums.AiPromptStatus;
import io.mango.ai.api.enums.AiServiceType;
import io.mango.ai.api.enums.AiModality;
import io.mango.ai.core.entity.AiPromptEntity;
import io.mango.ai.core.entity.AiServiceEntity;
import io.mango.ai.core.mapper.AiInvocationAuditMapper;
import io.mango.ai.core.mapper.AiPromptMapper;
import io.mango.ai.core.mapper.AiServiceMapper;
import io.mango.ai.core.mapper.AiSkillMapper;
import io.mango.ai.core.service.AiModelResolution;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.kv.api.ICache;
import io.mango.infra.kv.api.ILocker;
import io.mango.infra.kv.api.IRateLimiter;
import io.mango.ai.core.service.IAiModelManagementService;
import io.mango.ai.core.service.IAiChatConversationStore;
import io.mango.infra.realtime.api.RealtimeApi;
import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AI HTTP 入口特征测试，验证标准受理响应与 Realtime 增量发布契约。
 */
@Tag("flow")
@Tag("ai")
@SpringBootTest(classes = AiHttpContractFlowTest.TestApplication.class, properties = {
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
@AutoConfigureMockMvc
class AiHttpContractFlowTest {

    private final MockMvc mockMvc;

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

    @Autowired
    AiHttpContractFlowTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @BeforeEach
    void setUpModelConfig() {
        when(modelConfigService.resolveChatModel(100L))
                .thenReturn(new AiModelResolution(
                        100L,
                        new TestChatModel(),
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
        when(auditMapper.insert(any(io.mango.ai.core.entity.AiInvocationAuditEntity.class))).thenReturn(1);
        when(conversationStore.load(any(), anyInt()))
                .thenReturn(new IAiChatConversationStore.ConversationState(List.of()));
        when(locker.tryLock(any(), anyLong())).thenReturn(true);
        when(cache.exists(any())).thenReturn(false);
    }

    @AfterEach
    void clearContext() {
        MangoContextHolder.clear();
    }

    @Test
    void chat_空消息_由BeanValidation拒绝() throws Exception {
        mockMvc.perform(post("/ai/services/chat").queryParam("serviceCode", "assistant.general")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentParts\":[{\"type\":\"TEXT\",\"text\":\"   \"}],"
                                + "\"modelId\":100,\"thinkingEnabled\":false}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chat_非法会话标识_由命令校验拒绝() throws Exception {
        mockMvc.perform(post("/ai/services/chat").queryParam("serviceCode", "assistant.general")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentParts\":[{\"type\":\"TEXT\",\"text\":\"hello\"}],"
                                + "\"sessionId\":\"../../shared\","
                                + "\"modelId\":100,\"thinkingEnabled\":false}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void removedStandaloneChatEndpointReturns404() throws Exception {
        mockMvc.perform(post("/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hello\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void removedSynchronousServiceInvokeEndpointReturns404() throws Exception {
        mockMvc.perform(post("/ai/services/invoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serviceCode\":\"contract.extract\",\"input\":{\"text\":\"hello\"}}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void chat_合法请求_返回标准受理结果并通过Realtime发布完成事件() throws Exception {
        String requestId = "1d8f5930-87ac-4b6f-b330-6294c2b252ea";
        mockMvc.perform(post("/ai/services/chat").queryParam("serviceCode", "assistant.general")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("{\"contentParts\":[{\"type\":\"TEXT\",\"text\":\"hello\"}],"
                                + "\"sessionId\":\"session-1\","
                                + "\"requestId\":\"" + requestId + "\","
                                + "\"modelId\":100,\"thinkingEnabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.requestId").value(requestId))
                .andExpect(jsonPath("$.data.sessionId").value("session-1"));

        ArgumentCaptor<RealtimeOutboundMessage> captor = ArgumentCaptor.forClass(RealtimeOutboundMessage.class);
        verify(realtimeApi, timeout(3000).atLeast(2)).publish(captor.capture());
        List<RealtimeOutboundMessage> messages = captor.getAllValues();
        assertTrue(messages.stream().allMatch(message -> requestId.equals(message.context().requestId())));
        assertTrue(messages.stream().anyMatch(message -> message.content().contains("\"type\":\"message\"")));
        assertTrue(messages.stream().anyMatch(message -> message.content().contains("\"type\":\"done\"")));
        assertTrue(messages.stream().anyMatch(message -> Boolean.TRUE.equals(message.stream().completed())));
    }

    @Test
    void chat_思考模式显式Null_由BeanValidation拒绝() throws Exception {
        mockMvc.perform(post("/ai/services/chat").queryParam("serviceCode", "assistant.general")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentParts\":[{\"type\":\"TEXT\",\"text\":\"hello\"}],"
                                + "\"modelId\":100,\"thinkingEnabled\":null}"))
                .andExpect(status().isBadRequest());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({MangoAiAutoConfiguration.class, TestContextConfiguration.class})
    static class TestApplication {

        @Bean
        IRateLimiter testRateLimiter() {
            return (key, permits) -> true;
        }

        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    static class TestContextConfiguration {

        @Bean
        org.springframework.boot.web.servlet.FilterRegistrationBean<TestContextFilter> testContextFilter() {
            org.springframework.boot.web.servlet.FilterRegistrationBean<TestContextFilter> registration =
                    new org.springframework.boot.web.servlet.FilterRegistrationBean<>();
            registration.setFilter(new TestContextFilter());
            registration.addUrlPatterns("/*");
            registration.setOrder(Integer.MIN_VALUE + 20);
            return registration;
        }
    }

    private static final class TestContextFilter extends org.springframework.web.filter.OncePerRequestFilter {

        @Override
        protected void doFilterInternal(
                jakarta.servlet.http.HttpServletRequest request,
                jakarta.servlet.http.HttpServletResponse response,
                jakarta.servlet.FilterChain filterChain) throws jakarta.servlet.ServletException, java.io.IOException {
            MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                    101L, "tenant-test", "test-user", "tenant", "MEMBER", "USER", 101L, "admin"));
            try {
                filterChain.doFilter(request, response);
            } finally {
                MangoContextHolder.clear();
            }
        }
    }

    static final class TestChatModel implements ChatModel {

        @Override
        public ChatResponse call(Prompt prompt) {
            return response();
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(response());
        }

        private ChatResponse response() {
            return new ChatResponse(java.util.List.of(new Generation(new AssistantMessage("answer"))));
        }
    }

}
