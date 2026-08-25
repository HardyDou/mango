package io.mango.ai.core.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.ai.api.command.AiMessageContentPartCommand;
import io.mango.ai.api.command.AiServiceChatCommand;
import io.mango.ai.api.enums.AiApiProtocol;
import io.mango.ai.api.enums.AiCapability;
import io.mango.ai.api.enums.AiMessageContentType;
import io.mango.ai.api.enums.AiModality;
import io.mango.ai.api.enums.AiPromptStatus;
import io.mango.ai.api.enums.AiServiceType;
import io.mango.ai.api.vo.AiMessageContentPartVO;
import io.mango.ai.api.vo.AiServiceChatStartVO;
import io.mango.ai.core.entity.AiChatConversationEntity;
import io.mango.ai.core.entity.AiInvocationAuditEntity;
import io.mango.ai.core.entity.AiPromptEntity;
import io.mango.ai.core.entity.AiServiceEntity;
import io.mango.ai.core.entity.AiSkillEntity;
import io.mango.ai.core.mapper.AiInvocationAuditMapper;
import io.mango.ai.core.mapper.AiPromptMapper;
import io.mango.ai.core.mapper.AiServiceMapper;
import io.mango.ai.core.mapper.AiSkillMapper;
import io.mango.ai.core.service.AiConversationExchange;
import io.mango.ai.core.service.AiConversationScope;
import io.mango.ai.core.service.AiModelResolution;
import io.mango.ai.core.service.IAiChatConversationStore;
import io.mango.ai.core.service.IAiModelManagementService;
import io.mango.common.exception.BizException;
import io.mango.file.api.IFileContentProvider;
import io.mango.file.api.vo.FileDownloadVO;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.kv.api.ICache;
import io.mango.infra.kv.api.ILocker;
import io.mango.infra.kv.api.IRateLimiter;
import io.mango.infra.realtime.api.RealtimeApi;
import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import reactor.core.publisher.Flux;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 服务感知聊天业务规则测试。 */
class AiServiceChatServiceTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @AfterEach
    void clearContext() {
        MangoContextHolder.clear();
    }

    @Test
    void chat_加载服务PromptSkill并传递隔离的多轮历史() {
        RecordingChatModel model = new RecordingChatModel(List.of("first-answer", "second-answer", "other-answer"));
        RecordingConversationStore store = new RecordingConversationStore();
        Fixture fixture = fixture(model, store, (key, permits) -> true,
                AiServiceType.CHAT, AiPromptStatus.PUBLISHED, true);
        setContext("tenant-a", 101L);

        invoke(fixture, "assistant.general", command("first", "session-1"));
        invoke(fixture, "assistant.general", command("second", "session-1"));
        invoke(fixture, "assistant.other", command("other", "session-1"));

        assertEquals(List.of(
                "SYSTEM:遵守企业回答规范\n\n你是企业助手",
                "USER:first",
                "ASSISTANT:first-answer",
                "USER:second"), model.invocations.get(1));
        assertEquals(List.of("SYSTEM:遵守企业回答规范\n\n你是企业助手", "USER:other"),
                model.invocations.get(2));
        assertEquals(2, store.states.size());
        assertTrue(store.states.keySet().stream().anyMatch(key -> key.contains("assistant.general")));
        assertTrue(store.states.keySet().stream().anyMatch(key -> key.contains("assistant.other")));
    }

    @Test
    void chat_同一会话按轮切换模型并继承历史() {
        RecordingChatModel firstModel = new RecordingChatModel(List.of("first-answer"));
        RecordingChatModel secondModel = new RecordingChatModel(List.of("second-answer", "new-answer"));
        RecordingConversationStore store = new RecordingConversationStore();
        Fixture fixture = fixture(firstModel, store, (key, permits) -> true,
                AiServiceType.CHAT, AiPromptStatus.PUBLISHED, true);
        when(fixture.modelManagementService.resolveChatModel(200L))
                .thenReturn(resolution(200L, secondModel));
        setContext("tenant-a", 101L);

        invoke(fixture, "assistant.general", command("first", "session-1", 100L));
        invoke(fixture, "assistant.general", command("switch", "session-1", 200L));
        invoke(fixture, "assistant.general", command("new", "session-2", 200L));
        assertEquals(2, secondModel.invocations.size());
        assertEquals(List.of(
                "SYSTEM:遵守企业回答规范\n\n你是企业助手",
                "USER:first",
                "ASSISTANT:first-answer",
                "USER:switch"), secondModel.invocations.getFirst());
        assertEquals(List.of("SYSTEM:遵守企业回答规范\n\n你是企业助手", "USER:new"),
                secondModel.invocations.get(1));
    }

    @Test
    void chat_结构化服务_使用同一会话链并校验输出Schema() {
        Fixture fixture = fixture(new RecordingChatModel(List.of("{\"value\":\"ok\"}")), new RecordingConversationStore(),
                (key, permits) -> true, AiServiceType.EXTRACTION, AiPromptStatus.PUBLISHED, true);
        setContext("tenant-a", 101L);

        List<String> events = invoke(fixture, "contract.extract", command("hello", "session-1"));

        assertTrue(events.stream().anyMatch(event -> event.contains("STRUCTURED_DATA")));
    }

    @Test
    void chat_结构化服务仅上传文本文件_映射标准text输入并传递文件内容() {
        RecordingChatModel model = new RecordingChatModel(List.of("{\"value\":\"ok\"}"));
        IFileContentProvider provider = mock(IFileContentProvider.class);
        when(provider.downloadForService(88L)).thenAnswer(invocation -> new FileDownloadVO(
                new ByteArrayInputStream("合同正文".getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                "contract.txt", "text/plain", 12L));
        Fixture fixture = fixture(model, new RecordingConversationStore(), (key, permits) -> true,
                AiServiceType.EXTRACTION, AiPromptStatus.PUBLISHED, true, contentResolver(provider));
        setContext("tenant-a", 101L);

        invoke(fixture, "contract.extract", fileCommand("session-1", 88L));

        String userMessage = model.invocations.getFirst().getLast();
        assertTrue(userMessage.contains("请处理我上传的附件"));
        assertTrue(userMessage.contains("合同正文"));
    }

    @Test
    void chat_Prompt未发布或Skill停用_拒绝执行() {
        setContext("tenant-a", 101L);
        Fixture draftPrompt = fixture(new RecordingChatModel(List.of("unused")), new RecordingConversationStore(),
                (key, permits) -> true, AiServiceType.CHAT, AiPromptStatus.DRAFT, true);
        assertThrows(BizException.class,
                () -> draftPrompt.service.chat("assistant.general", command("hello", "session-1")));

        Fixture disabledSkill = fixture(new RecordingChatModel(List.of("unused")), new RecordingConversationStore(),
                (key, permits) -> true, AiServiceType.CHAT, AiPromptStatus.PUBLISHED, false);
        assertThrows(BizException.class,
                () -> disabledSkill.service.chat("assistant.general", command("hello", "session-1")));
    }

    @Test
    void chat_模板变量或Skill工具_拒绝未定义的聊天语义() {
        setContext("tenant-a", 101L);
        Fixture fixture = fixture(new RecordingChatModel(List.of("unused")), new RecordingConversationStore(),
                (key, permits) -> true, AiServiceType.CHAT, AiPromptStatus.PUBLISHED, true);
        fixture.prompt.setTemplate("回答 {{message}}");
        assertThrows(BizException.class,
                () -> fixture.service.chat("assistant.general", command("hello", "session-1")));

        fixture.prompt.setTemplate("你是企业助手");
        fixture.skill.setToolIdsJson("[1001]");
        assertThrows(BizException.class,
                () -> fixture.service.chat("assistant.general", command("hello", "session-1")));
    }

    @Test
    void chat_成功完成_记录服务模型Token和用量审计() {
        Fixture fixture = fixture(new RecordingChatModel(List.of("answer")), new RecordingConversationStore(),
                (key, permits) -> true, AiServiceType.CHAT, AiPromptStatus.PUBLISHED, true);
        setContext("tenant-a", 101L);

        List<String> events = invoke(fixture, "assistant.general", command("hello", "session-1"));

        assertTrue(events.stream().anyMatch(event -> event.contains("\"type\":\"done\"")));
        assertTrue(events.stream().anyMatch(event -> event.contains("\"modelId\":100")));
        assertTrue(events.stream().anyMatch(event -> event.contains("\"modelName\":\"test-model\"")));
        assertTrue(events.stream().anyMatch(event -> event.contains("\"providerCode\":\"test-provider\"")));
        assertTrue(events.stream().anyMatch(event -> event.contains("\"thinkingEnabled\":false")));
        ArgumentCaptor<AiInvocationAuditEntity> captor = ArgumentCaptor.forClass(AiInvocationAuditEntity.class);
        verify(fixture.auditMapper, timeout(2_000L)).insert(captor.capture());
        AiInvocationAuditEntity audit = captor.getValue();
        assertEquals("assistant.general", audit.getServiceCode());
        assertEquals("test-provider", audit.getProviderCode());
        assertEquals("test-model", audit.getModelName());
        assertEquals(7, audit.getPromptTokens());
        assertEquals(3, audit.getCompletionTokens());
        assertEquals("SUCCESS", audit.getResultStatus());
    }

    @Test
    void chat_流式空白分片_完整保留Markdown结构() {
        ChatModel model = mock(ChatModel.class);
        when(model.stream(any(Prompt.class))).thenReturn(Flux.just(
                response("##"),
                response(" "),
                response("标题"),
                response("\n\n"),
                response("-"),
                response(" "),
                response("列表")));
        RecordingConversationStore store = new RecordingConversationStore();
        Fixture fixture = fixture(model, store, (key, permits) -> true,
                AiServiceType.CHAT, AiPromptStatus.PUBLISHED, true);
        setContext("tenant-a", 101L);

        invoke(fixture, "assistant.general", command("hello", "session-1"));

        IAiChatConversationStore.ConversationState state = store.states.values().iterator().next();
        assertEquals("## 标题\n\n- 列表", state.messages().getLast().contentParts().getFirst().getText());
    }

    @Test
    void chat_限流拒绝_不调用模型并记录审计() {
        RecordingChatModel model = new RecordingChatModel(List.of("unused"));
        Fixture fixture = fixture(model, new RecordingConversationStore(), (key, permits) -> false,
                AiServiceType.CHAT, AiPromptStatus.PUBLISHED, true);
        setContext("tenant-a", 101L);

        List<String> events = invoke(fixture, "assistant.general", command("hello", "session-1"));

        assertTrue(events.getFirst().contains("AI 对话请求过于频繁"));
        assertTrue(model.invocations.isEmpty());
        ArgumentCaptor<AiInvocationAuditEntity> captor = ArgumentCaptor.forClass(AiInvocationAuditEntity.class);
        verify(fixture.auditMapper, timeout(2_000L)).insert(captor.capture());
        assertEquals("RATE_LIMITED", captor.getValue().getResultStatus());
    }

    @Test
    void chat_相同RequestId并发提交_只允许一个执行者() {
        ChatModel pendingModel = mock(ChatModel.class);
        when(pendingModel.stream(any(Prompt.class))).thenReturn(Flux.never());
        Fixture fixture = fixture(pendingModel, new RecordingConversationStore(), (key, permits) -> true,
                AiServiceType.CHAT, AiPromptStatus.PUBLISHED, true);
        setContext("tenant-a", 101L);
        AiServiceChatCommand command = command("hello", "session-1");

        fixture.service.chat("assistant.general", command);

        assertThrows(BizException.class, () -> fixture.service.chat("assistant.general", command));
        assertTrue(fixture.service.cancel(command.getRequestId()));
    }

    @Test
    void cancel_非请求所有者_拒绝取消且保留原请求() {
        ChatModel pendingModel = mock(ChatModel.class);
        when(pendingModel.stream(any(Prompt.class))).thenReturn(Flux.never());
        Fixture fixture = fixture(pendingModel, new RecordingConversationStore(), (key, permits) -> true,
                AiServiceType.CHAT, AiPromptStatus.PUBLISHED, true);
        setContext("tenant-a", 101L);
        AiServiceChatCommand command = command("hello", "session-1");
        fixture.service.chat("assistant.general", command);

        setContext("tenant-a", 202L);
        assertThrows(BizException.class, () -> fixture.service.cancel(command.getRequestId()));
        assertTrue(fixture.requestCoordinator.hasOwner(command.getRequestId()));

        setContext("tenant-a", 101L);
        assertTrue(fixture.service.cancel(command.getRequestId()));
    }

    @Test
    void cancel_跨实例请求_共享取消标记并释放执行锁() {
        ChatModel pendingModel = mock(ChatModel.class);
        when(pendingModel.stream(any(Prompt.class))).thenReturn(Flux.never());
        RequestCoordinator requestCoordinator = new RequestCoordinator();
        Fixture executor = fixture(pendingModel, new RecordingConversationStore(), (key, permits) -> true,
                AiServiceType.CHAT, AiPromptStatus.PUBLISHED, true, contentResolver(), requestCoordinator,
                new RecordingRealtimeApi());
        Fixture canceller = fixture(new RecordingChatModel(List.of("unused")), new RecordingConversationStore(),
                (key, permits) -> true, AiServiceType.CHAT, AiPromptStatus.PUBLISHED, true, contentResolver(),
                requestCoordinator, new RecordingRealtimeApi());
        setContext("tenant-a", 101L);
        AiServiceChatCommand command = command("hello", "session-1");
        executor.service.chat("assistant.general", command);

        assertTrue(canceller.service.cancel(command.getRequestId()));

        ArgumentCaptor<AiInvocationAuditEntity> audit = ArgumentCaptor.forClass(AiInvocationAuditEntity.class);
        verify(executor.auditMapper, timeout(2_000L)).insert(audit.capture());
        assertEquals("CANCELLED", audit.getValue().getResultStatus());
        awaitRequestReleased(requestCoordinator, command.getRequestId());
    }

    @Test
    void chat_Realtime发布异常_释放请求并记录投递失败() {
        RequestCoordinator requestCoordinator = new RequestCoordinator();
        RecordingRealtimeApi realtimeApi = new RecordingRealtimeApi();
        realtimeApi.failOnPublish(2, new IllegalStateException("outbox unavailable"));
        Fixture fixture = fixture(new RecordingChatModel(List.of("answer")), new RecordingConversationStore(),
                (key, permits) -> true, AiServiceType.CHAT, AiPromptStatus.PUBLISHED, true, contentResolver(),
                requestCoordinator, realtimeApi);
        setContext("tenant-a", 101L);
        AiServiceChatCommand command = command("hello", "session-1");

        fixture.service.chat("assistant.general", command);

        ArgumentCaptor<AiInvocationAuditEntity> audit = ArgumentCaptor.forClass(AiInvocationAuditEntity.class);
        verify(fixture.auditMapper, timeout(2_000L)).insert(audit.capture());
        assertEquals("DELIVERY_FAILED", audit.getValue().getResultStatus());
        awaitRequestReleased(requestCoordinator, command.getRequestId());
        verify(fixture.auditMapper, times(1)).insert(any(AiInvocationAuditEntity.class));
    }

    @Test
    void chat_终态投递成功但审计写入失败_不产生取消或重复审计() {
        RequestCoordinator requestCoordinator = new RequestCoordinator();
        Fixture fixture = fixture(new RecordingChatModel(List.of("answer")), new RecordingConversationStore(),
                (key, permits) -> true, AiServiceType.CHAT, AiPromptStatus.PUBLISHED, true, contentResolver(),
                requestCoordinator, new RecordingRealtimeApi());
        when(fixture.auditMapper.insert(any(AiInvocationAuditEntity.class)))
                .thenThrow(new IllegalStateException("audit unavailable"));
        setContext("tenant-a", 101L);
        AiServiceChatCommand command = command("hello", "session-1");

        List<String> events = invoke(fixture, "assistant.general", command);

        assertTrue(events.getLast().contains("\"type\":\"done\""));
        awaitRequestReleased(requestCoordinator, command.getRequestId());
        verify(fixture.auditMapper, times(1)).insert(any(AiInvocationAuditEntity.class));
    }

    @Test
    void chat_Realtime事件_按租户用户和请求精确投递() {
        Fixture fixture = fixture(new RecordingChatModel(List.of("answer")), new RecordingConversationStore(),
                (key, permits) -> true, AiServiceType.CHAT, AiPromptStatus.PUBLISHED, true);
        setContext("tenant-a", 101L);
        AiServiceChatCommand command = command("hello", "session-1");

        invoke(fixture, "assistant.general", command);

        RealtimeOutboundMessage message = fixture.realtimeApi.history().getFirst();
        assertEquals("tenant-a", message.context().tenantId());
        assertEquals(101L, message.context().userId());
        assertEquals(command.getRequestId(), message.context().requestId());
        assertEquals("101", message.target().id());
    }

    private Fixture fixture(
            ChatModel model,
            IAiChatConversationStore conversationStore,
            IRateLimiter rateLimiter,
            AiServiceType serviceType,
            AiPromptStatus promptStatus,
            boolean skillEnabled) {
        return fixture(model, conversationStore, rateLimiter, serviceType, promptStatus, skillEnabled,
                contentResolver());
    }

    private Fixture fixture(
            ChatModel model,
            IAiChatConversationStore conversationStore,
            IRateLimiter rateLimiter,
            AiServiceType serviceType,
            AiPromptStatus promptStatus,
            boolean skillEnabled,
            AiMessageContentResolver contentResolver) {
        return fixture(model, conversationStore, rateLimiter, serviceType, promptStatus, skillEnabled,
                contentResolver, new RequestCoordinator(), new RecordingRealtimeApi());
    }

    private Fixture fixture(
            ChatModel model,
            IAiChatConversationStore conversationStore,
            IRateLimiter rateLimiter,
            AiServiceType serviceType,
            AiPromptStatus promptStatus,
            boolean skillEnabled,
            AiMessageContentResolver contentResolver,
            RequestCoordinator requestCoordinator,
            RecordingRealtimeApi realtimeApi) {
        AiServiceMapper serviceMapper = mock(AiServiceMapper.class);
        AiPromptMapper promptMapper = mock(AiPromptMapper.class);
        AiSkillMapper skillMapper = mock(AiSkillMapper.class);
        AiInvocationAuditMapper auditMapper = mock(AiInvocationAuditMapper.class);
        IAiModelManagementService modelManagementService = mock(IAiModelManagementService.class);
        AiServiceEntity serviceEntity = new AiServiceEntity();
        serviceEntity.setCode("assistant.general");
        serviceEntity.setServiceType(serviceType);
        serviceEntity.setCapability(AiCapability.CHAT);
        serviceEntity.setEnabled(true);
        serviceEntity.setPromptId(10L);
        serviceEntity.setSkillId(20L);
        serviceEntity.setInputSchemaJson("{\"type\":\"object\",\"required\":[\"text\"],"
                + "\"properties\":{\"text\":{\"type\":\"string\"}}}");
        serviceEntity.setOutputSchemaJson("{\"type\":\"object\",\"required\":[\"value\"],"
                + "\"properties\":{\"value\":{\"type\":\"string\"}}}");
        AiPromptEntity prompt = new AiPromptEntity();
        prompt.setTemplate("你是企业助手");
        prompt.setStatus(promptStatus);
        AiSkillEntity skill = new AiSkillEntity();
        skill.setEnabled(skillEnabled);
        skill.setInstructions("遵守企业回答规范");
        skill.setToolIdsJson("[]");

        when(serviceMapper.selectOne(any())).thenReturn(serviceEntity);
        when(promptMapper.selectById(10L)).thenReturn(prompt);
        when(skillMapper.selectById(20L)).thenReturn(skill);
        when(auditMapper.insert(any(AiInvocationAuditEntity.class))).thenReturn(1);
        when(modelManagementService.resolveChatModel(100L)).thenReturn(resolution(100L, model));

        AiServiceChatService service = new AiServiceChatService(
                serviceMapper, promptMapper, skillMapper, auditMapper, modelManagementService,
                conversationStore, contentResolver, rateLimiter, requestCoordinator, requestCoordinator, realtimeApi,
                OBJECT_MAPPER, new SimpleMeterRegistry(),
                20, 50.0F, 2, Duration.ofSeconds(30));
        return new Fixture(service, prompt, skill, auditMapper, modelManagementService, realtimeApi,
                requestCoordinator);
    }

    private AiServiceChatCommand command(String message, String sessionId) {
        return command(message, sessionId, 100L);
    }

    private AiServiceChatCommand command(String message, String sessionId, Long modelId) {
        AiServiceChatCommand command = new AiServiceChatCommand();
        AiMessageContentPartCommand text = new AiMessageContentPartCommand();
        text.setType(AiMessageContentType.TEXT);
        text.setText(message);
        command.setContentParts(List.of(text));
        command.setSessionId(sessionId);
        command.setModelId(modelId);
        command.setThinkingEnabled(false);
        command.setRequestId(UUID.randomUUID().toString());
        return command;
    }

    private AiServiceChatCommand fileCommand(String sessionId, Long fileId) {
        AiServiceChatCommand command = new AiServiceChatCommand();
        AiMessageContentPartCommand file = new AiMessageContentPartCommand();
        file.setType(AiMessageContentType.FILE);
        file.setFileId(fileId);
        command.setContentParts(List.of(file));
        command.setSessionId(sessionId);
        command.setModelId(100L);
        command.setThinkingEnabled(false);
        command.setRequestId(UUID.randomUUID().toString());
        return command;
    }

    private List<String> invoke(Fixture fixture, String serviceCode, AiServiceChatCommand command) {
        AiServiceChatStartVO started = fixture.service.chat(serviceCode, command);
        assertEquals(command.getRequestId(), started.getRequestId());
        assertEquals(command.getSessionId(), started.getSessionId());
        return fixture.realtimeApi.awaitTerminal(command.getRequestId());
    }

    private AiMessageContentResolver contentResolver(IFileContentProvider... providers) {
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        for (int index = 0; index < providers.length; index++) {
            beans.addBean("fileContentProvider" + index, providers[index]);
        }
        return new AiMessageContentResolver(beans.getBeanProvider(IFileContentProvider.class));
    }

    private AiModelResolution resolution(Long modelId, ChatModel model) {
        return new AiModelResolution(
                modelId,
                model,
                "test-provider",
                "test-model",
                AiApiProtocol.CHAT_COMPLETIONS,
                false,
                Set.of(AiModality.TEXT),
                Set.of(AiModality.TEXT));
    }

    private void setContext(String tenantId, Long userId) {
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                userId, tenantId, "user-" + userId, "tenant", "MEMBER", "USER", userId, "admin"));
    }

    private void awaitRequestReleased(RequestCoordinator coordinator, String requestId) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2L);
        while (System.nanoTime() < deadline) {
            if (!coordinator.hasOwner(requestId) && !coordinator.isLocked(requestId)) {
                return;
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10L));
        }
        throw new AssertionError("AI 请求未按时释放 requestId=" + requestId);
    }

    private static ChatResponse response(String content) {
        ChatResponseMetadata metadata = ChatResponseMetadata.builder()
                .model("test-model")
                .usage(new DefaultUsage(7, 3, 10))
                .build();
        return new ChatResponse(List.of(new Generation(new org.springframework.ai.chat.messages.AssistantMessage(content))),
                metadata);
    }

    private record Fixture(
            AiServiceChatService service,
            AiPromptEntity prompt,
            AiSkillEntity skill,
            AiInvocationAuditMapper auditMapper,
            IAiModelManagementService modelManagementService,
            RecordingRealtimeApi realtimeApi,
            RequestCoordinator requestCoordinator) {
    }

    private static final class RequestCoordinator implements ICache, ILocker {
        private final Map<String, String> values = new ConcurrentHashMap<>();
        private final Set<String> locks = ConcurrentHashMap.newKeySet();

        @Override
        public void set(String key, String value, long ttlSeconds) {
            values.put(key, value);
        }

        @Override
        public String get(String key) {
            return values.get(key);
        }

        @Override
        public boolean exists(String key) {
            return values.containsKey(key);
        }

        @Override
        public void delete(String key) {
            values.remove(key);
        }

        @Override
        public boolean tryLock(String key, long ttlSeconds) {
            return locks.add(key);
        }

        @Override
        public void unlock(String key) {
            locks.remove(key);
        }

        private boolean hasOwner(String requestId) {
            return values.containsKey("ai-service-chat:request-owner:" + requestId);
        }

        private boolean isLocked(String requestId) {
            return locks.contains("ai-service-chat:request-lock:" + requestId);
        }
    }

    private static final class RecordingRealtimeApi implements RealtimeApi {
        private static final long EVENT_TIMEOUT_SECONDS = 3L;
        private final BlockingQueue<RealtimeOutboundMessage> messages = new LinkedBlockingQueue<>();
        private final List<RealtimeOutboundMessage> history = new CopyOnWriteArrayList<>();
        private final AtomicReference<RuntimeException> nextFailure = new AtomicReference<>();
        private final AtomicInteger publishCount = new AtomicInteger();
        private final AtomicInteger failurePublishNumber = new AtomicInteger(-1);

        @Override
        public void publish(RealtimeOutboundMessage message) {
            int currentPublish = publishCount.incrementAndGet();
            RuntimeException failure = failurePublishNumber.compareAndSet(currentPublish, -1)
                    ? nextFailure.getAndSet(null) : null;
            if (failure != null) {
                throw failure;
            }
            history.add(message);
            messages.add(message);
        }

        private void failOnPublish(int publishNumber, RuntimeException failure) {
            nextFailure.set(failure);
            failurePublishNumber.set(publishNumber);
        }

        private List<RealtimeOutboundMessage> history() {
            return List.copyOf(history);
        }

        private List<String> awaitTerminal(String requestId) {
            List<String> events = new ArrayList<>();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(EVENT_TIMEOUT_SECONDS);
            try {
                while (System.nanoTime() < deadline) {
                    long remaining = Math.max(1L, deadline - System.nanoTime());
                    RealtimeOutboundMessage message = messages.poll(remaining, TimeUnit.NANOSECONDS);
                    if (message == null || !requestId.equals(message.context().requestId())) {
                        continue;
                    }
                    events.add(message.payload().textValue());
                    if (Boolean.TRUE.equals(message.stream().completed())) {
                        return events;
                    }
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AssertionError("等待 AI Realtime 事件时被中断", exception);
            }
            throw new AssertionError("等待 AI Realtime 完成事件超时 requestId=" + requestId);
        }
    }

    private static final class RecordingChatModel implements ChatModel {
        private final List<String> responses;
        private final List<List<String>> invocations = new ArrayList<>();
        private final AtomicInteger index = new AtomicInteger();

        private RecordingChatModel(List<String> responses) {
            this.responses = responses;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            return response(responses.get(index.getAndIncrement()));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            invocations.add(prompt.getInstructions().stream().map(this::description).toList());
            return Flux.just(response(responses.get(index.getAndIncrement())));
        }

        private String description(Message message) {
            return message.getMessageType().name() + ':' + message.getText();
        }
    }

    private static final class RecordingConversationStore implements IAiChatConversationStore {
        private final Map<String, ConversationState> states = new ConcurrentHashMap<>();
        private final Map<String, AiChatConversationEntity> conversations = new ConcurrentHashMap<>();

        @Override
        public List<io.mango.ai.api.vo.AiChatConversationVO> list(AiConversationScope scope) {
            return List.of();
        }

        @Override
        public io.mango.ai.api.vo.AiChatConversationDetailVO detail(AiConversationScope scope) {
            return new io.mango.ai.api.vo.AiChatConversationDetailVO();
        }

        @Override
        public ConversationState load(AiConversationScope scope, int maxHistoryMessages) {
            ConversationState state = states.get(key(scope));
            if (state == null) {
                return new ConversationState(List.of());
            }
            List<ConversationMessage> messages = state.messages();
            int fromIndex = Math.max(0, messages.size() - maxHistoryMessages);
            return new ConversationState(messages.subList(fromIndex, messages.size()));
        }

        @Override
        public void saveExchange(AiConversationExchange exchange) {
            AiConversationScope scope = exchange.scope();
            String key = key(scope);
            ConversationState previous = states.get(key);
            AiChatConversationEntity conversation = conversations.computeIfAbsent(key,
                    ignored -> conversation(scope, exchange.thinkingEnabled(), exchange.resolution()));
            List<ConversationMessage> messages = new ArrayList<>(previous == null
                    ? List.of() : previous.messages());
            messages.add(new ConversationMessage("user", exchange.userContentParts()));
            messages.add(new ConversationMessage("assistant", exchange.assistantContentParts()));
            conversation.setLastModelId(exchange.resolution().getModelId());
            conversation.setLastModelName(exchange.resolution().getModelName());
            conversation.setLastProviderCode(exchange.resolution().getProviderCode());
            conversation.setLastThinkingEnabled(exchange.thinkingEnabled());
            conversation.setMessageCount(messages.size());
            states.put(key, new ConversationState(messages));
        }

        @Override
        public boolean delete(AiConversationScope scope) {
            String key = key(scope);
            conversations.remove(key);
            return states.remove(key) != null;
        }

        private AiChatConversationEntity conversation(
                AiConversationScope scope,
                boolean thinkingEnabled,
                AiModelResolution resolution) {
            AiChatConversationEntity conversation = new AiChatConversationEntity();
            conversation.setTenantId(scope.tenantId());
            conversation.setUserId(scope.userId());
            conversation.setServiceCode(scope.serviceCode());
            conversation.setSessionId(scope.sessionId());
            conversation.setLastModelId(resolution.getModelId());
            conversation.setLastModelName(resolution.getModelName());
            conversation.setLastProviderCode(resolution.getProviderCode());
            conversation.setLastThinkingEnabled(thinkingEnabled);
            conversation.setMessageCount(0);
            return conversation;
        }

        private String key(AiConversationScope scope) {
            return scope.tenantId() + ':' + scope.userId() + ':' + scope.serviceCode() + ':' + scope.sessionId();
        }
    }
}
