package io.mango.ai.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.mango.ai.api.command.AiServiceChatCommand;
import io.mango.ai.api.enums.AiCapability;
import io.mango.ai.api.enums.AiCode;
import io.mango.ai.api.enums.AiMessageContentType;
import io.mango.ai.api.enums.AiPromptStatus;
import io.mango.ai.api.enums.AiServiceType;
import io.mango.ai.api.vo.AiChatConversationDetailVO;
import io.mango.ai.api.vo.AiChatConversationVO;
import io.mango.ai.api.vo.AiMessageContentPartVO;
import io.mango.ai.api.vo.AiServiceRuntimeOptionsVO;
import io.mango.ai.core.entity.AiInvocationAuditEntity;
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
import io.mango.ai.core.service.IAiServiceChatService;
import io.mango.common.result.Require;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.kv.api.IRateLimiter;
import io.mango.infra.log.Loggers;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekAssistantMessage;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 所有可运行 AI 服务的唯一流式会话执行链。 */
@Service
public class AiServiceChatService implements IAiServiceChatService {

    private static final Logger LOG = LoggerFactory.getLogger(AiServiceChatService.class);
    private static final Logger AUDIT_LOG = LoggerFactory.getLogger(Loggers.OPERATION);
    private static final Pattern SERVICE_CODE_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._:-]{0,63}$");
    private static final Pattern SESSION_ID_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$");
    private static final Pattern TEMPLATE_VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_.-]+)\\s*}}");
    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";
    private static final String SUCCESS = "SUCCESS";
    private static final String FAILED = "FAILED";
    private static final String RATE_LIMITED = "RATE_LIMITED";
    private static final int MAX_SESSION_ID_LENGTH = 128;

    private final AiServiceMapper serviceMapper;
    private final AiPromptMapper promptMapper;
    private final AiSkillMapper skillMapper;
    private final AiInvocationAuditMapper auditMapper;
    private final IAiModelManagementService modelManagementService;
    private final IAiChatConversationStore conversationStore;
    private final AiMessageContentResolver contentResolver;
    private final IRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final CircuitBreaker circuitBreaker;
    private final int maxHistoryMessages;

    /** 创建服务感知的 AI 聊天执行器。 */
    public AiServiceChatService(
            AiServiceMapper serviceMapper,
            AiPromptMapper promptMapper,
            AiSkillMapper skillMapper,
            AiInvocationAuditMapper auditMapper,
            IAiModelManagementService modelManagementService,
            IAiChatConversationStore conversationStore,
            AiMessageContentResolver contentResolver,
            IRateLimiter rateLimiter,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry,
            @Value("${mango.ai.chat.max-history-messages:20}") int maxHistoryMessages,
            @Value("${mango.ai.chat.circuit-breaker.failure-rate-threshold:50}") float failureRateThreshold,
            @Value("${mango.ai.chat.circuit-breaker.minimum-calls:5}") int minimumCalls,
            @Value("${mango.ai.chat.circuit-breaker.open-state-duration:30s}") Duration openStateDuration) {
        this.serviceMapper = serviceMapper;
        this.promptMapper = promptMapper;
        this.skillMapper = skillMapper;
        this.auditMapper = auditMapper;
        this.modelManagementService = modelManagementService;
        this.conversationStore = conversationStore;
        this.contentResolver = contentResolver;
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper.copy();
        this.meterRegistry = meterRegistry;
        Require.positive(maxHistoryMessages, "maxHistoryMessages must be positive");
        this.maxHistoryMessages = maxHistoryMessages;
        this.circuitBreaker = CircuitBreaker.of("mangoAiServiceChatModel", CircuitBreakerConfig.custom()
                .failureRateThreshold(failureRateThreshold)
                .minimumNumberOfCalls(minimumCalls)
                .slidingWindowSize(minimumCalls)
                .waitDurationInOpenState(openStateDuration)
                .build());
    }

    @Override
    public List<AiChatConversationVO> conversations(String serviceCode) {
        String normalizedServiceCode = normalizeServiceCode(serviceCode);
        requireExecutableService(normalizedServiceCode);
        String tenantId = requireTenantId();
        Long userId = requireUserId();
        return conversationStore.list(tenantId, userId, normalizedServiceCode);
    }

    @Override
    public AiChatConversationDetailVO conversation(String serviceCode, String sessionId) {
        String normalizedServiceCode = normalizeServiceCode(serviceCode);
        requireExecutableService(normalizedServiceCode);
        return conversationStore.detail(
                requireTenantId(),
                requireUserId(),
                normalizedServiceCode,
                normalizeSessionId(sessionId));
    }

    @Override
    public Boolean deleteConversation(String serviceCode, String sessionId) {
        String normalizedServiceCode = normalizeServiceCode(serviceCode);
        requireExecutableService(normalizedServiceCode);
        return conversationStore.delete(
                requireTenantId(),
                requireUserId(),
                normalizedServiceCode,
                normalizeSessionId(sessionId));
    }

    @Override
    public AiServiceRuntimeOptionsVO options(String serviceCode) {
        AiServiceEntity service = requireExecutableService(normalizeServiceCode(serviceCode));
        requirePublishedPrompt(service);
        loadSkill(service.getSkillId());
        return modelManagementService.runtimeOptions();
    }

    @Override
    public Flux<String> chat(String serviceCode, AiServiceChatCommand command) {
        Require.notNull(command, AiCode.CHAT_REQUEST_REQUIRED, "对话请求不能为空");
        String normalizedServiceCode = normalizeServiceCode(serviceCode);
        Require.notNull(command.getContentParts(), AiCode.CHAT_REQUEST_INVALID, "消息内容不能为空");
        Require.notNull(command.getModelId(), AiCode.CHAT_REQUEST_INVALID, "会话模型不能为空");
        Require.notNull(command.getThinkingEnabled(), AiCode.CHAT_REQUEST_INVALID, "思考模式不能为空");
        String tenantId = requireTenantId();
        Long userId = requireUserId();

        InvocationContext context = prepareContext(
                normalizedServiceCode,
                tenantId,
                userId,
                resolveSessionId(command.getSessionId()),
                command.getContentParts(),
                command.getModelId(),
                Boolean.TRUE.equals(command.getThinkingEnabled()));
        return Flux.defer(() -> execute(context));
    }

    private InvocationContext prepareContext(
            String serviceCode,
            String tenantId,
            Long userId,
            String sessionId,
            List<io.mango.ai.api.command.AiMessageContentPartCommand> contentParts,
            Long modelId,
            boolean thinkingEnabled) {
        AiServiceEntity service = requireExecutableService(serviceCode);
        AiPromptEntity prompt = requirePublishedPrompt(service);
        AiSkillEntity skill = loadSkill(service.getSkillId());
        AiModelResolution resolution = modelManagementService.resolveChatModel(modelId);
        Require.isTrue(!thinkingEnabled || resolution.isThinkingConfigurable(), AiCode.CHAT_REQUEST_INVALID,
                "当前模型不支持可配置思考模式");
        List<AiMessageContentPartVO> normalizedParts = contentResolver.normalize(contentParts);
        validateStructuredInput(service, normalizedParts);
        return new InvocationContext(
                tenantId,
                userId,
                serviceCode,
                sessionId,
                service,
                prompt,
                skill,
                normalizedParts,
                thinkingEnabled,
                MangoContextHolder.traceId(),
                UUID.randomUUID().toString(),
                resolution,
                System.nanoTime());
    }

    private String normalizeServiceCode(String serviceCode) {
        Require.notBlank(serviceCode, AiCode.SERVICE_NOT_FOUND, "服务编码不能为空");
        String normalized = serviceCode.trim();
        Require.isTrue(SERVICE_CODE_PATTERN.matcher(normalized).matches(),
                AiCode.SERVICE_NOT_FOUND, "服务编码格式不正确");
        return normalized;
    }

    private AiServiceEntity requireExecutableService(String serviceCode) {
        AiServiceEntity service = Require.nonNull(serviceMapper.selectOne(new LambdaQueryWrapper<AiServiceEntity>()
                .eq(AiServiceEntity::getCode, serviceCode)), AiCode.SERVICE_NOT_FOUND);
        Require.isTrue(Boolean.TRUE.equals(service.getEnabled()), AiCode.SERVICE_NOT_EXECUTABLE, "AI 服务未启用");
        Require.notNull(service.getServiceType(), AiCode.SERVICE_TYPE_UNSUPPORTED, "AI 服务类型不能为空");
        Require.isTrue(service.getServiceType() == AiServiceType.CHAT
                        || service.getServiceType() == AiServiceType.EXTRACTION
                        || service.getServiceType() == AiServiceType.CLASSIFICATION,
                AiCode.SERVICE_TYPE_UNSUPPORTED, "当前 AI 服务类型不能运行");
        Require.isTrue(service.getCapability() == null || service.getCapability() == AiCapability.CHAT,
                AiCode.SERVICE_TYPE_UNSUPPORTED, "统一会话运行台当前使用 CHAT 模型能力");
        return service;
    }

    private AiPromptEntity requirePublishedPrompt(AiServiceEntity service) {
        AiPromptEntity prompt = Require.nonNull(service.getPromptId() == null ? null
                : promptMapper.selectById(service.getPromptId()), AiCode.SERVICE_NOT_EXECUTABLE,
                "AI 服务未配置 Prompt");
        Require.isTrue(prompt.getStatus() == AiPromptStatus.PUBLISHED, AiCode.SERVICE_NOT_EXECUTABLE,
                "AI 服务 Prompt 尚未发布");
        Require.isTrue(StringUtils.hasText(prompt.getTemplate()), AiCode.SERVICE_NOT_EXECUTABLE,
                "AI 服务 Prompt 为空");
        if (service.getServiceType() == AiServiceType.CHAT) {
            Require.isTrue(!TEMPLATE_VARIABLE_PATTERN.matcher(prompt.getTemplate()).find(),
                    AiCode.SERVICE_NOT_EXECUTABLE, "CHAT Prompt 只能使用固定 system 指令，不支持模板变量");
        }
        return prompt;
    }

    private AiSkillEntity loadSkill(Long skillId) {
        if (skillId == null) {
            return null;
        }
        AiSkillEntity skill = Require.nonNull(skillMapper.selectById(skillId), AiCode.SERVICE_NOT_EXECUTABLE,
                "AI 服务 Skill 不存在");
        Require.isTrue(Boolean.TRUE.equals(skill.getEnabled()), AiCode.SERVICE_NOT_EXECUTABLE,
                "AI 服务 Skill 未启用");
        Require.isTrue(readToolIds(skill.getToolIdsJson()).isEmpty(), AiCode.SERVICE_TOOLS_UNSUPPORTED);
        return skill;
    }

    private String buildSystemPrompt(InvocationContext context) {
        StringBuilder content = new StringBuilder();
        if (context.skill() != null && StringUtils.hasText(context.skill().getInstructions())) {
            content.append(context.skill().getInstructions().trim()).append("\n\n");
        }
        if (context.service().getServiceType() == AiServiceType.CHAT) {
            content.append(context.prompt().getTemplate().trim());
        } else {
            content.append("你正在执行受 Schema 约束的 ")
                    .append(context.service().getServiceType().name())
                    .append(" 服务。每轮只返回符合输出 Schema 的 JSON，不输出 Markdown 或解释文字。");
        }
        return content.toString();
    }

    private Flux<String> execute(InvocationContext context) {
        if (!rateLimiter.tryAcquire(rateLimitKey(context), 1)) {
            recordInvocation(context, RATE_LIMITED, AiCode.CHAT_RATE_LIMITED.getCode(), TokenUsage.empty(), 0L);
            return Flux.just(errorEvent(AiCode.CHAT_RATE_LIMITED.getMessage()));
        }

        ConversationAccumulator accumulator = new ConversationAccumulator();
        IAiChatConversationStore.ConversationState conversation = loadConversation(context);
        List<IAiChatConversationStore.ConversationMessage> history = conversation.messages();
        List<List<AiMessageContentPartVO>> contextParts = history.stream()
                .filter(message -> ROLE_USER.equals(message.role()))
                .map(IAiChatConversationStore.ConversationMessage::contentParts)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        contextParts.add(context.userContentParts());
        contentResolver.validateContextFileBudget(contextParts);
        List<Message> messages = new ArrayList<>(history.size() + 2);
        messages.add(new SystemMessage(buildSystemPrompt(context)));
        messages.addAll(toSpringMessages(context, history));
        messages.add(toSpringUserMessage(context, context.userContentParts()));
        return context.resolution().getChatModel().stream(modelPrompt(context, messages))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .concatMap(response -> mapResponse(response, context.thinkingEnabled(), accumulator))
                .concatWith(Mono.defer(() -> completeConversation(context, accumulator)))
                .onErrorResume(error -> modelError(context, accumulator, error));
    }

    private Prompt modelPrompt(InvocationContext context, List<Message> messages) {
        if (!context.resolution().isThinkingConfigurable()) {
            return new Prompt(messages);
        }
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(context.resolution().getModelName())
                .reasoningEffort(context.thinkingEnabled() ? "medium" : "none")
                .build();
        return new Prompt(messages, options);
    }

    private Flux<String> mapResponse(
            ChatResponse response,
            boolean thinkingEnabled,
            ConversationAccumulator accumulator) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return Flux.empty();
        }
        AssistantMessage output = response.getResult().getOutput();
        if (response.getMetadata() != null) {
            accumulator.captureUsage(response.getMetadata().getUsage());
        }
        List<String> events = new ArrayList<>(2);
        if (thinkingEnabled && output instanceof DeepSeekAssistantMessage deepSeekMessage
                && hasStreamDelta(deepSeekMessage.getReasoningContent())) {
            events.add(contentEvent("thinking", deepSeekMessage.getReasoningContent()));
        }
        if (hasStreamDelta(output.getText())) {
            accumulator.append(output.getText());
            events.add(contentEvent("message", output.getText()));
        }
        if (output.getMedia() != null && !output.getMedia().isEmpty()) {
            accumulator.appendMedia(output.getMedia());
        }
        return Flux.fromIterable(events);
    }

    private static boolean hasStreamDelta(String content) {
        return content != null && !content.isEmpty();
    }

    private Mono<String> completeConversation(
            InvocationContext context,
            ConversationAccumulator accumulator) {
        return Mono.fromCallable(() -> {
            String assistantText = accumulator.content();
            Require.isTrue(StringUtils.hasText(assistantText) || !accumulator.media().isEmpty(),
                    AiCode.CHAT_MODEL_UNAVAILABLE, "AI 模型未返回有效内容");
            List<AiMessageContentPartVO> assistantParts = assistantParts(
                    context.service(), assistantText, accumulator.media(), context.requestId());
            conversationStore.saveExchange(
                    context.tenantId(),
                    context.userId(),
                    context.serviceCode(),
                    context.sessionId(),
                    context.userContentParts(),
                    assistantParts,
                    context.thinkingEnabled(),
                    context.resolution());
            recordInvocation(
                    context,
                    SUCCESS,
                    null,
                    accumulator.tokenUsage(),
                    assistantText.getBytes(StandardCharsets.UTF_8).length);
            return doneEvent(context, assistantParts);
        });
    }

    private Flux<String> modelError(
            InvocationContext context,
            ConversationAccumulator accumulator,
            Throwable error) {
        String errorType = error instanceof CallNotPermittedException
                ? "circuit_open" : error.getClass().getSimpleName();
        LOG.error(
                "AI service chat failed tenantId={} userId={} serviceCode={} sessionId={} traceId={} errorType={}",
                context.tenantId(),
                context.userId(),
                context.serviceCode(),
                context.sessionId(),
                context.traceId(),
                errorType,
                error);
        recordInvocation(context, FAILED, AiCode.CHAT_MODEL_UNAVAILABLE.getCode(), accumulator.tokenUsage(),
                accumulator.content().getBytes(StandardCharsets.UTF_8).length);
        return Flux.just(errorEvent(AiCode.CHAT_MODEL_UNAVAILABLE.getMessage()));
    }

    private IAiChatConversationStore.ConversationState loadConversation(InvocationContext context) {
        return conversationStore.load(
                context.tenantId(), context.userId(), context.serviceCode(), context.sessionId(), maxHistoryMessages);
    }

    private List<Message> toSpringMessages(
            InvocationContext context,
            List<IAiChatConversationStore.ConversationMessage> history) {
        List<Message> messages = new ArrayList<>(history.size());
        for (IAiChatConversationStore.ConversationMessage message : history) {
            if (ROLE_USER.equals(message.role())) {
                messages.add(toSpringUserMessage(context, message.contentParts()));
            } else if (ROLE_ASSISTANT.equals(message.role())) {
                messages.add(new AssistantMessage(assistantText(message.contentParts())));
            }
        }
        return messages;
    }

    private void recordInvocation(
            InvocationContext context,
            String result,
            Integer errorCode,
            TokenUsage usage,
            long outputBytes) {
        long latencyMs = Duration.ofNanos(System.nanoTime() - context.startedNanos()).toMillis();
        meterRegistry.counter("mango.ai.service.chat.requests", "result", result).increment();
        meterRegistry.counter("mango.ai.service.chat.tokens", "type", "prompt").increment(usage.promptTokens());
        meterRegistry.counter("mango.ai.service.chat.tokens", "type", "completion").increment(usage.completionTokens());

        AiInvocationAuditEntity audit = new AiInvocationAuditEntity();
        audit.setId(IdWorker.getId());
        audit.setTenantId(context.tenantId());
        audit.setCreatedBy(context.userId());
        audit.setUpdatedBy(context.userId());
        audit.setCreatedAt(LocalDateTime.now());
        audit.setUpdatedAt(audit.getCreatedAt());
        audit.setRequestId(context.requestId());
        audit.setUserId(context.userId());
        audit.setTraceId(context.traceId());
        audit.setServiceCode(context.serviceCode());
        audit.setServiceType(context.service().getServiceType().name());
        audit.setCapability(AiCapability.CHAT.name());
        audit.setProviderCode(context.resolution().getProviderCode());
        audit.setModelName(context.resolution().getModelName());
        audit.setResultStatus(result);
        audit.setErrorCode(errorCode);
        audit.setPromptTokens(usage.promptTokens());
        audit.setCompletionTokens(usage.completionTokens());
        audit.setInputBytes(inputBytes(context.userContentParts()));
        audit.setOutputBytes(outputBytes);
        audit.setLatencyMs(latencyMs);
        audit.setCompletedAt(LocalDateTime.now());
        Require.isTrue(auditMapper.insert(audit) > 0, AiCode.SERVICE_AUDIT_FAILED);
        AUDIT_LOG.info(
                "event=ai_service_chat tenantId={} userId={} serviceCode={} sessionId={} requestId={} provider={} "
                        + "model={} result={} errorCode={} promptTokens={} completionTokens={} durationMs={}",
                context.tenantId(), context.userId(), context.serviceCode(), context.sessionId(), context.requestId(),
                context.resolution().getProviderCode(), context.resolution().getModelName(), result,
                errorCode == null ? "none" : errorCode, usage.promptTokens(), usage.completionTokens(), latencyMs);
    }

    private Set<Long> readToolIds(String value) {
        if (!StringUtils.hasText(value)) {
            return Set.of();
        }
        try {
            return objectMapper.readValue(value,
                    objectMapper.getTypeFactory().constructCollectionType(Set.class, Long.class));
        } catch (JsonProcessingException exception) {
            return Require.fail(AiCode.SERVICE_NOT_EXECUTABLE, "Skill 工具引用数据损坏", exception);
        }
    }

    private String rateLimitKey(InvocationContext context) {
        return "ai-service-chat:user:" + context.tenantId() + ':' + context.userId() + ':' + context.serviceCode();
    }

    private void validateStructuredInput(
            AiServiceEntity service,
            List<AiMessageContentPartVO> contentParts) {
        if (service.getServiceType() == AiServiceType.CHAT) {
            return;
        }
        JsonNode schema = parseObject(service.getInputSchemaJson(), AiCode.SERVICE_INPUT_INVALID,
                "服务输入 Schema 无效");
        JsonNode input = canonicalInput(contentParts, schema);
        AiJsonSchemaValidator.validate(schema, input, AiCode.SERVICE_INPUT_INVALID);
    }

    private Message toSpringUserMessage(
            InvocationContext context,
            List<AiMessageContentPartVO> contentParts) {
        if (context.service().getServiceType() == AiServiceType.CHAT) {
            return contentResolver.toUserMessage(contentParts, context.resolution());
        }
        return contentResolver.toUserMessage(
                contentParts,
                context.resolution(),
                structuredUserPrompt(context, contentParts));
    }

    private String structuredUserPrompt(
            InvocationContext context,
            List<AiMessageContentPartVO> contentParts) {
        JsonNode schema = parseObject(context.service().getInputSchemaJson(), AiCode.SERVICE_INPUT_INVALID,
                "服务输入 Schema 无效");
        JsonNode input = canonicalInput(contentParts, schema);
        String template = context.prompt().getTemplate();
        Matcher matcher = TEMPLATE_VARIABLE_PATTERN.matcher(template);
        StringBuilder rendered = new StringBuilder();
        int end = 0;
        boolean hasVariable = false;
        while (matcher.find()) {
            hasVariable = true;
            rendered.append(template, end, matcher.start());
            JsonNode value = resolveInput(input, matcher.group(1));
            Require.isTrue(value != null && !value.isMissingNode(), AiCode.SERVICE_INPUT_INVALID,
                    "Prompt 变量不存在: " + matcher.group(1));
            rendered.append(value.isTextual() ? value.textValue() : value.toString());
            end = matcher.end();
        }
        rendered.append(template, end, template.length());
        if (!hasVariable) {
            rendered.append("\n\n输入数据：\n").append(input);
        }
        rendered.append("\n\n只返回符合以下 JSON Schema 的 JSON：\n")
                .append(context.service().getOutputSchemaJson());
        return rendered.toString();
    }

    private JsonNode resolveInput(JsonNode input, String path) {
        JsonNode current = input;
        for (String part : path.split("\\.")) {
            current = current == null ? null : current.get(part);
        }
        return current;
    }

    private ObjectNode canonicalInput(
            List<AiMessageContentPartVO> contentParts,
            JsonNode schema) {
        ObjectNode input = objectMapper.createObjectNode();
        contentParts.stream()
                .filter(part -> part.getType() == AiMessageContentType.TEXT)
                .map(AiMessageContentPartVO::getText)
                .findFirst()
                .ifPresent(text -> input.put("text", text));
        ArrayNode files = objectMapper.createArrayNode();
        contentParts.stream()
                .filter(part -> part.getFileId() != null)
                .forEach(part -> {
                    ObjectNode file = files.addObject();
                    file.put("fileId", String.valueOf(part.getFileId()));
                    file.put("type", part.getType().name());
                    file.put("fileName", part.getFileName());
                    file.put("contentType", part.getContentType());
                    file.put("fileSize", part.getFileSize());
                });
        if (!files.isEmpty()) {
            input.set("files", files);
            JsonNode properties = schema.path("properties");
            if (!input.has("text") && properties.path("text").path("type").asText().equals("string")) {
                input.put("text", "请处理我上传的附件。");
            }
        }
        return input;
    }

    private List<AiMessageContentPartVO> assistantParts(
            AiServiceEntity service,
            String modelText,
            List<Media> media,
            String requestId) {
        if (service.getServiceType() == AiServiceType.CHAT) {
            List<AiMessageContentPartVO> parts = new ArrayList<>(media.size() + 1);
            if (StringUtils.hasText(modelText)) {
                AiMessageContentPartVO text = new AiMessageContentPartVO();
                text.setType(AiMessageContentType.RICH_TEXT);
                text.setText(modelText);
                parts.add(text);
            }
            for (int index = 0; index < media.size(); index++) {
                parts.add(contentResolver.saveAssistantMedia(media.get(index), requestId, index + 1));
            }
            Require.isTrue(!parts.isEmpty(), AiCode.CHAT_MODEL_UNAVAILABLE, "AI 模型未返回可展示内容");
            return List.copyOf(parts);
        }
        Require.isTrue(media.isEmpty(), AiCode.SERVICE_OUTPUT_INVALID, "结构化服务不允许返回媒体内容");
        AiMessageContentPartVO part = new AiMessageContentPartVO();
        String resultJson = normalizeJsonResult(modelText);
        JsonNode result = parseObject(resultJson, AiCode.SERVICE_OUTPUT_INVALID, "AI 输出不是合法 JSON");
        JsonNode schema = parseObject(service.getOutputSchemaJson(), AiCode.SERVICE_OUTPUT_INVALID,
                "服务输出 Schema 无效");
        AiJsonSchemaValidator.validate(schema, result, AiCode.SERVICE_OUTPUT_INVALID);
        part.setType(AiMessageContentType.STRUCTURED_DATA);
        part.setDataJson(result.toString());
        return List.of(part);
    }

    private String assistantText(List<AiMessageContentPartVO> parts) {
        StringBuilder text = new StringBuilder();
        for (AiMessageContentPartVO part : parts) {
            String value = part.getType() == AiMessageContentType.STRUCTURED_DATA
                    ? part.getDataJson() : part.getText();
            if (StringUtils.hasText(value)) {
                if (!text.isEmpty()) {
                    text.append("\n\n");
                }
                text.append(value);
            }
        }
        return text.toString();
    }

    private JsonNode parseObject(String json, AiCode code, String message) {
        try {
            JsonNode value = objectMapper.readTree(json);
            Require.isTrue(value != null && value.isObject(), code, message);
            return value;
        } catch (JsonProcessingException exception) {
            return Require.fail(code, message, exception);
        }
    }

    private String normalizeJsonResult(String value) {
        String normalized = value.trim();
        if (normalized.startsWith("```") && normalized.endsWith("```")) {
            int firstLine = normalized.indexOf('\n');
            normalized = firstLine < 0
                    ? normalized.substring(3, normalized.length() - 3).trim()
                    : normalized.substring(firstLine + 1, normalized.length() - 3).trim();
        }
        return normalized;
    }

    private long inputBytes(List<AiMessageContentPartVO> parts) {
        long bytes = 0L;
        for (AiMessageContentPartVO part : parts) {
            if (part.getText() != null) {
                bytes += part.getText().getBytes(StandardCharsets.UTF_8).length;
            }
            if (part.getFileSize() != null) {
                bytes += part.getFileSize();
            }
        }
        return bytes;
    }

    private String resolveSessionId(String sessionId) {
        if (StringUtils.hasText(sessionId)) {
            return normalizeSessionId(sessionId);
        }
        return UUID.randomUUID().toString();
    }

    private String normalizeSessionId(String sessionId) {
        Require.notBlank(sessionId, AiCode.CHAT_REQUEST_INVALID, "会话标识不能为空");
        String normalized = sessionId.trim();
        Require.isTrue(normalized.length() <= MAX_SESSION_ID_LENGTH
                        && SESSION_ID_PATTERN.matcher(normalized).matches(),
                AiCode.CHAT_REQUEST_INVALID, "会话标识格式不正确");
        return normalized;
    }

    private String requireTenantId() {
        String tenantId = MangoContextHolder.tenantId();
        Require.notBlank(tenantId, AiCode.TENANT_REQUIRED, "租户标识不能为空");
        return tenantId;
    }

    private Long requireUserId() {
        return Require.nonNull(MangoContextHolder.userId(), AiCode.USER_REQUIRED, "用户标识不能为空");
    }

    private String contentEvent(String type, String content) {
        return writeJson(Map.of("type", type, "content", content));
    }

    private String errorEvent(String message) {
        return writeJson(Map.of("type", "error", "message", message));
    }

    private String doneEvent(InvocationContext context, List<AiMessageContentPartVO> contentParts) {
        return writeJson(Map.of(
                "type", "done",
                "sessionId", context.sessionId(),
                "requestId", context.requestId(),
                "modelId", context.resolution().getModelId(),
                "modelName", context.resolution().getModelName(),
                "providerCode", context.resolution().getProviderCode(),
                "thinkingEnabled", context.thinkingEnabled(),
                "contentParts", contentParts));
    }

    private String writeJson(Map<String, ?> event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            return Require.rethrow(new IllegalStateException("AI 流式事件序列化失败", exception));
        }
    }

    private record InvocationContext(
            String tenantId,
            Long userId,
            String serviceCode,
            String sessionId,
            AiServiceEntity service,
            AiPromptEntity prompt,
            AiSkillEntity skill,
            List<AiMessageContentPartVO> userContentParts,
            boolean thinkingEnabled,
            String traceId,
            String requestId,
            AiModelResolution resolution,
            long startedNanos) {
    }

    private record TokenUsage(int promptTokens, int completionTokens) {
        private static TokenUsage empty() {
            return new TokenUsage(0, 0);
        }

        private static TokenUsage from(Usage usage) {
            if (usage == null) {
                return empty();
            }
            return new TokenUsage(valueOrZero(usage.getPromptTokens()), valueOrZero(usage.getCompletionTokens()));
        }

        private static int valueOrZero(Integer value) {
            return value == null ? 0 : value;
        }
    }

    private static final class ConversationAccumulator {
        private final StringBuilder content = new StringBuilder();
        private final List<Media> media = new ArrayList<>();
        private TokenUsage tokenUsage = TokenUsage.empty();

        private void append(String delta) {
            content.append(delta);
        }

        private void captureUsage(Usage usage) {
            TokenUsage current = TokenUsage.from(usage);
            if (current.promptTokens() > 0 || current.completionTokens() > 0) {
                tokenUsage = current;
            }
        }

        private void appendMedia(List<Media> values) {
            media.addAll(values);
        }

        private String content() {
            return content.toString();
        }

        private TokenUsage tokenUsage() {
            return tokenUsage;
        }

        private List<Media> media() {
            return List.copyOf(media);
        }
    }
}
