package io.mango.ai.core.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.ai.api.command.AiServiceChatCommand;
import io.mango.ai.api.command.AiMessageContentPartCommand;
import io.mango.ai.api.enums.AiApiProtocol;
import io.mango.ai.api.enums.AiCapability;
import io.mango.ai.api.enums.AiMessageContentType;
import io.mango.ai.api.enums.AiModality;
import io.mango.ai.api.enums.AiPromptStatus;
import io.mango.ai.api.enums.AiServiceType;
import io.mango.ai.core.entity.AiInvocationAuditEntity;
import io.mango.ai.core.entity.AiChatConversationEntity;
import io.mango.ai.core.entity.AiPromptEntity;
import io.mango.ai.core.entity.AiServiceEntity;
import io.mango.ai.core.entity.AiSkillEntity;
import io.mango.ai.core.mapper.AiInvocationAuditMapper;
import io.mango.ai.core.mapper.AiPromptMapper;
import io.mango.ai.core.mapper.AiServiceMapper;
import io.mango.ai.core.mapper.AiSkillMapper;
import io.mango.ai.core.service.AiModelResolution;
import io.mango.ai.core.service.IAiChatConversationStore;
import io.mango.ai.core.service.IAiModelManagementService;
import io.mango.ai.api.vo.AiMessageContentPartVO;
import io.mango.common.exception.BizException;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.kv.api.IRateLimiter;
import io.mango.file.api.IFileContentProvider;
import io.mango.file.api.vo.FileDownloadVO;
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

import java.time.Duration;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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

        fixture.service.chat("assistant.general", command("first", "session-1")).collectList().block();
        fixture.service.chat("assistant.general", command("second", "session-1")).collectList().block();
        fixture.service.chat("assistant.other", command("other", "session-1")).collectList().block();

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

        fixture.service.chat("assistant.general", command("first", "session-1", 100L))
                .collectList().block();

        fixture.service.chat("assistant.general", command("switch", "session-1", 200L))
                .collectList().block();

        fixture.service.chat("assistant.general", command("new", "session-2", 200L))
                .collectList().block();
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

        List<String> events = fixture.service.chat("contract.extract", command("hello", "session-1"))
                .collectList().block();

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

        fixture.service.chat("contract.extract", fileCommand("session-1", 88L)).collectList().block();

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

        List<String> events = fixture.service.chat("assistant.general", command("hello", "session-1"))
                .collectList().block();

        assertTrue(events.stream().anyMatch(event -> event.contains("\"type\":\"done\"")));
        assertTrue(events.stream().anyMatch(event -> event.contains("\"modelId\":100")));
        assertTrue(events.stream().anyMatch(event -> event.contains("\"modelName\":\"test-model\"")));
        assertTrue(events.stream().anyMatch(event -> event.contains("\"providerCode\":\"test-provider\"")));
        assertTrue(events.stream().anyMatch(event -> event.contains("\"thinkingEnabled\":false")));
        ArgumentCaptor<AiInvocationAuditEntity> captor = ArgumentCaptor.forClass(AiInvocationAuditEntity.class);
        verify(fixture.auditMapper).insert(captor.capture());
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

        fixture.service.chat("assistant.general", command("hello", "session-1"))
                .collectList().block();

        IAiChatConversationStore.ConversationState state = store.states.values().iterator().next();
        assertEquals("## 标题\n\n- 列表", state.messages().getLast().contentParts().getFirst().getText());
    }

    @Test
    void chat_限流拒绝_不调用模型并记录审计() {
        RecordingChatModel model = new RecordingChatModel(List.of("unused"));
        Fixture fixture = fixture(model, new RecordingConversationStore(), (key, permits) -> false,
                AiServiceType.CHAT, AiPromptStatus.PUBLISHED, true);
        setContext("tenant-a", 101L);

        List<String> events = fixture.service.chat("assistant.general", command("hello", "session-1"))
                .collectList().block();

        assertTrue(events.getFirst().contains("AI 对话请求过于频繁"));
        assertTrue(model.invocations.isEmpty());
        ArgumentCaptor<AiInvocationAuditEntity> captor = ArgumentCaptor.forClass(AiInvocationAuditEntity.class);
        verify(fixture.auditMapper).insert(captor.capture());
        assertEquals("RATE_LIMITED", captor.getValue().getResultStatus());
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
                conversationStore, contentResolver, rateLimiter, OBJECT_MAPPER, new SimpleMeterRegistry(),
                20, 50.0F, 2, Duration.ofSeconds(30));
        return new Fixture(service, prompt, skill, auditMapper, modelManagementService);
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
        return command;
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
            IAiModelManagementService modelManagementService) {
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
        private final Map<String, ConversationState> states = new HashMap<>();

        @Override
        public List<io.mango.ai.api.vo.AiChatConversationVO> list(
                String tenantId, Long userId, String serviceCode) {
            return List.of();
        }

        @Override
        public io.mango.ai.api.vo.AiChatConversationDetailVO detail(
                String tenantId, Long userId, String serviceCode, String sessionId) {
            return new io.mango.ai.api.vo.AiChatConversationDetailVO();
        }

        @Override
        public ConversationState load(
                String tenantId,
                Long userId,
                String serviceCode,
                String sessionId,
                int maxHistoryMessages) {
            ConversationState state = states.get(key(tenantId, userId, serviceCode, sessionId));
            if (state == null) {
                return new ConversationState(null, List.of());
            }
            List<ConversationMessage> messages = state.messages();
            int fromIndex = Math.max(0, messages.size() - maxHistoryMessages);
            return new ConversationState(state.conversation(), messages.subList(fromIndex, messages.size()));
        }

        @Override
        public void saveExchange(
                String tenantId,
                Long userId,
                String serviceCode,
                String sessionId,
                List<AiMessageContentPartVO> userContentParts,
                List<AiMessageContentPartVO> assistantContentParts,
                boolean thinkingEnabled,
                AiModelResolution resolution) {
            String key = key(tenantId, userId, serviceCode, sessionId);
            ConversationState previous = states.get(key);
            AiChatConversationEntity conversation = previous == null
                    ? conversation(tenantId, userId, serviceCode, sessionId, thinkingEnabled, resolution)
                    : previous.conversation();
            List<ConversationMessage> messages = new ArrayList<>(previous == null
                    ? List.of() : previous.messages());
            messages.add(new ConversationMessage("user", userContentParts));
            messages.add(new ConversationMessage("assistant", assistantContentParts));
            conversation.setLastModelId(resolution.getModelId());
            conversation.setLastModelName(resolution.getModelName());
            conversation.setLastProviderCode(resolution.getProviderCode());
            conversation.setLastThinkingEnabled(thinkingEnabled);
            conversation.setMessageCount(messages.size());
            states.put(key, new ConversationState(conversation, messages));
        }

        @Override
        public boolean delete(String tenantId, Long userId, String serviceCode, String sessionId) {
            return states.remove(key(tenantId, userId, serviceCode, sessionId)) != null;
        }

        private AiChatConversationEntity conversation(
                String tenantId,
                Long userId,
                String serviceCode,
                String sessionId,
                boolean thinkingEnabled,
                AiModelResolution resolution) {
            AiChatConversationEntity conversation = new AiChatConversationEntity();
            conversation.setTenantId(tenantId);
            conversation.setUserId(userId);
            conversation.setServiceCode(serviceCode);
            conversation.setSessionId(sessionId);
            conversation.setLastModelId(resolution.getModelId());
            conversation.setLastModelName(resolution.getModelName());
            conversation.setLastProviderCode(resolution.getProviderCode());
            conversation.setLastThinkingEnabled(thinkingEnabled);
            conversation.setMessageCount(0);
            return conversation;
        }

        private String key(String tenantId, Long userId, String serviceCode, String sessionId) {
            return tenantId + ':' + userId + ':' + serviceCode + ':' + sessionId;
        }
    }
}
