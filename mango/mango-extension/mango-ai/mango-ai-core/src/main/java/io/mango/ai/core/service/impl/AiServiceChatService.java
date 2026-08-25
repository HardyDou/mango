package io.mango.ai.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import io.mango.ai.api.vo.AiServiceChatStartVO;
import io.mango.ai.api.vo.AiServiceRuntimeOptionsVO;
import io.mango.ai.core.entity.AiInvocationAuditEntity;
import io.mango.ai.core.entity.AiPromptEntity;
import io.mango.ai.core.entity.AiServiceEntity;
import io.mango.ai.core.entity.AiSkillEntity;
import io.mango.ai.core.mapper.AiInvocationAuditMapper;
import io.mango.ai.core.mapper.AiPromptMapper;
import io.mango.ai.core.mapper.AiServiceMapper;
import io.mango.ai.core.mapper.AiSkillMapper;
import io.mango.ai.core.service.AiAssistantMediaInput;
import io.mango.ai.core.service.AiConversationExchange;
import io.mango.ai.core.service.AiConversationScope;
import io.mango.ai.core.service.AiModelResolution;
import io.mango.ai.core.service.AiUserMessageInput;
import io.mango.ai.core.service.IAiChatConversationStore;
import io.mango.ai.core.service.IAiModelManagementService;
import io.mango.ai.core.service.IAiServiceChatService;
import io.mango.common.result.Require;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.kv.api.ICache;
import io.mango.infra.kv.api.ILocker;
import io.mango.infra.kv.api.IRateLimiter;
import io.mango.infra.log.Loggers;
import io.mango.infra.realtime.api.RealtimeApi;
import io.mango.infra.realtime.api.dto.RealtimeContext;
import io.mango.infra.realtime.api.dto.RealtimeEvent;
import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;
import io.mango.infra.realtime.api.dto.RealtimePayload;
import io.mango.infra.realtime.api.dto.RealtimeSource;
import io.mango.infra.realtime.api.dto.RealtimeStatus;
import io.mango.infra.realtime.api.dto.RealtimeStream;
import io.mango.infra.realtime.api.dto.RealtimeTarget;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.deepseek.DeepSeekAssistantMessage;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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
    private static final String CANCELLED = "CANCELLED";
    private static final String DELIVERY_FAILED = "DELIVERY_FAILED";
    private static final String REALTIME_EVENT_NAME = "service.chat";
    private static final long ACTIVE_REQUEST_TTL_SECONDS = 360L;
    private static final Duration REMOTE_CANCELLATION_POLL_INTERVAL = Duration.ofMillis(100L);
    private static final int MAX_SESSION_ID_LENGTH = 128;
    private static final int MARKDOWN_FENCE_LENGTH = 3;

    private final AiServiceMapper serviceMapper;
    private final AiPromptMapper promptMapper;
    private final AiSkillMapper skillMapper;
    private final AiInvocationAuditMapper auditMapper;
    private final IAiModelManagementService modelManagementService;
    private final IAiChatConversationStore conversationStore;
    private final AiMessageContentResolver contentResolver;
    private final IRateLimiter rateLimiter;
    private final ICache cache;
    private final ILocker locker;
    private final RealtimeApi realtimeApi;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final CircuitBreaker circuitBreaker;
    private final int maxHistoryMessages;
    private final ConcurrentMap<String, ActiveInvocation> activeInvocations = new ConcurrentHashMap<>();

    /** 创建服务感知的 AI 聊天执行器。 */
    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring injects shared service collaborators; copying container-managed services is not valid")
    public AiServiceChatService(
            AiServiceMapper serviceMapper,
            AiPromptMapper promptMapper,
            AiSkillMapper skillMapper,
            AiInvocationAuditMapper auditMapper,
            IAiModelManagementService modelManagementService,
            IAiChatConversationStore conversationStore,
            AiMessageContentResolver contentResolver,
            IRateLimiter rateLimiter,
            ICache cache,
            ILocker locker,
            RealtimeApi realtimeApi,
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
        this.cache = cache;
        this.locker = locker;
        this.realtimeApi = realtimeApi;
        this.objectMapper = objectMapper.copy();
        this.meterRegistry = meterRegistry;
        Require.positive(maxHistoryMessages, AiCode.CONFIG_INVALID);
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
        return conversationStore.list(new AiConversationScope(tenantId, userId, normalizedServiceCode, null));
    }

    @Override
    public AiChatConversationDetailVO conversation(String serviceCode, String sessionId) {
        String normalizedServiceCode = normalizeServiceCode(serviceCode);
        requireExecutableService(normalizedServiceCode);
        return conversationStore.detail(conversationScope(normalizedServiceCode, normalizeSessionId(sessionId)));
    }

    @Override
    public Boolean deleteConversation(String serviceCode, String sessionId) {
        Require.notBlank(serviceCode, AiCode.SERVICE_NOT_FOUND, "服务编码不能为空");
        String normalizedServiceCode = normalizeServiceCode(serviceCode);
        requireExecutableService(normalizedServiceCode);
        return conversationStore.delete(conversationScope(normalizedServiceCode, normalizeSessionId(sessionId)));
    }

    @Override
    public AiServiceRuntimeOptionsVO options(String serviceCode) {
        AiServiceEntity service = requireExecutableService(normalizeServiceCode(serviceCode));
        requirePublishedPrompt(service);
        loadSkill(service.getSkillId());
        return modelManagementService.runtimeOptions();
    }

    @Override
    public AiServiceChatStartVO chat(String serviceCode, AiServiceChatCommand command) {
        Require.notNull(command, AiCode.CHAT_REQUEST_REQUIRED, "对话请求不能为空");
        String normalizedServiceCode = normalizeServiceCode(serviceCode);
        Require.notNull(command.getContentParts(), AiCode.CHAT_REQUEST_INVALID, "消息内容不能为空");
        Require.notNull(command.getModelId(), AiCode.CHAT_REQUEST_INVALID, "会话模型不能为空");
        Require.notNull(command.getThinkingEnabled(), AiCode.CHAT_REQUEST_INVALID, "思考模式不能为空");
        Require.notBlank(command.getRequestId(), AiCode.CHAT_REQUEST_INVALID, "请求标识不能为空");
        String tenantId = requireTenantId();
        Long userId = requireUserId();

        InvocationContext context = prepareContext(
                normalizedServiceCode,
                tenantId,
                userId,
                resolveSessionId(command.getSessionId()),
                command.getContentParts(),
                command.getModelId(),
                Boolean.TRUE.equals(command.getThinkingEnabled()),
                command.getRequestId().trim());
        return startInvocation(context);
    }

    @Override
    public Boolean cancel(String requestId) {
        Require.notBlank(requestId, AiCode.CHAT_REQUEST_INVALID, "请求标识不能为空");
        String owner = cache.get(requestOwnerKey(requestId));
        if (owner == null) {
            return false;
        }
        Require.isTrue(owner.equals(requestOwner(requireTenantId(), requireUserId())),
                AiCode.CHAT_REQUEST_INVALID, "只能取消当前用户发起的 AI 请求");
        cache.set(requestCancelledKey(requestId), Boolean.TRUE.toString(), ACTIVE_REQUEST_TTL_SECONDS);
        ActiveInvocation invocation = activeInvocations.get(requestId);
        if (invocation != null) {
            invocation.cancel();
        }
        return true;
    }

    private InvocationContext prepareContext(
            String serviceCode,
            String tenantId,
            Long userId,
            String sessionId,
            List<io.mango.ai.api.command.AiMessageContentPartCommand> contentParts,
            Long modelId,
            boolean thinkingEnabled,
            String requestId) {
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
                requestId,
                resolution,
                System.nanoTime());
    }

    private AiServiceChatStartVO startInvocation(InvocationContext context) {
        String lockKey = requestLockKey(context.requestId());
        Require.isTrue(locker.tryLock(lockKey, ACTIVE_REQUEST_TTL_SECONDS),
                AiCode.CHAT_REQUEST_INVALID, "请求标识已被使用，请重新发送");
        ActiveInvocation invocation = new ActiveInvocation();
        try {
            cache.set(
                    requestOwnerKey(context.requestId()),
                    requestOwner(context.tenantId(), context.userId()),
                    ACTIVE_REQUEST_TTL_SECONDS);
            activeInvocations.put(context.requestId(), invocation);
            AtomicInteger chunk = new AtomicInteger();
            Disposable subscription = Flux.defer(() -> execute(context, invocation))
                    .publishOn(Schedulers.boundedElastic())
                    .concatMap(event -> Mono.fromRunnable(() -> publishEvent(context, event, chunk.incrementAndGet(),
                            invocation)))
                    .doFinally(signal -> finishInvocation(context.requestId(), invocation))
                    .subscribe(
                            ignored -> { },
                            error -> invocationFailure(context, invocation, error));
            invocation.attach(subscription);
            AiServiceChatStartVO result = new AiServiceChatStartVO();
            result.setRequestId(context.requestId());
            result.setSessionId(context.sessionId());
            return result;
        } catch (RuntimeException exception) {
            finishInvocation(context.requestId(), invocation);
            return Require.rethrow(exception);
        }
    }

    private void finishInvocation(String requestId, ActiveInvocation invocation) {
        if (!invocation.terminate()) {
            return;
        }
        activeInvocations.remove(requestId, invocation);
        cache.delete(requestOwnerKey(requestId));
        cache.delete(requestCancelledKey(requestId));
        locker.unlock(requestLockKey(requestId));
    }

    private void publishEvent(
            InvocationContext context,
            String eventJson,
            int chunk,
            ActiveInvocation invocation) {
        JsonNode event = parseEvent(eventJson);
        String type = event.path("type").asText();
        boolean completed = "done".equals(type) || "error".equals(type);
        RealtimeStatus status = "error".equals(type)
                ? RealtimeStatus.error() : "done".equals(type) ? RealtimeStatus.success() : null;
        try {
            realtimeApi.publish(new RealtimeOutboundMessage(
                    null,
                    "1.0",
                    RealtimeEvent.of("ai", REALTIME_EVENT_NAME),
                    RealtimeSource.server(),
                    new RealtimeContext(context.tenantId(), context.userId(), context.traceId(), context.requestId()),
                    RealtimeTarget.user(context.userId()),
                    Map.of("serviceCode", context.serviceCode(), "sessionId", context.sessionId()),
                    RealtimePayload.text(eventJson),
                    null,
                    (long) chunk,
                    status,
                    null,
                    new RealtimeStream(context.requestId(), chunk, completed)));
        } catch (RuntimeException exception) {
            invocation.markDeliveryFailed();
            Require.rethrow(exception);
        }
        if (completed) {
            invocation.markTerminalPublished();
            TerminalAudit terminalAudit = Require.nonNull(invocation.takeTerminalAudit(),
                    AiCode.SERVICE_AUDIT_FAILED, "AI 终态审计上下文不存在");
            recordInvocationOnce(invocation, context, terminalAudit.result(), terminalAudit.errorCode(),
                    terminalAudit.usage(), terminalAudit.outputBytes());
        }
    }

    private JsonNode parseEvent(String eventJson) {
        try {
            return objectMapper.readTree(eventJson);
        } catch (JsonProcessingException exception) {
            return Require.fail(AiCode.SERVICE_INVOCATION_FAILED, "AI 实时事件序列化结果无效", exception);
        }
    }

    private void invocationFailure(InvocationContext context, ActiveInvocation invocation, Throwable error) {
        if (invocation.isDeliveryFailed()) {
            recordInvocationOnce(invocation, context, DELIVERY_FAILED, AiCode.SERVICE_INVOCATION_FAILED.getCode(),
                    TokenUsage.empty(), 0L);
        }
        LOG.error(
                "AI asynchronous invocation failed tenantId={} userId={} serviceCode={} requestId={} traceId={} "
                        + "deliveryFailed={}",
                context.tenantId(),
                context.userId(),
                context.serviceCode(),
                context.requestId(),
                context.traceId(),
                invocation.isDeliveryFailed(),
                error);
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

    private Flux<String> execute(InvocationContext context, ActiveInvocation invocation) {
        if (!rateLimiter.tryAcquire(rateLimitKey(context), 1)) {
            invocation.prepareTerminalAudit(new TerminalAudit(
                    RATE_LIMITED, AiCode.CHAT_RATE_LIMITED.getCode(), TokenUsage.empty(), 0L));
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
        AtomicBoolean remotelyCancelled = new AtomicBoolean();
        Flux<Long> cancellationSignal = Flux.interval(Duration.ZERO, REMOTE_CANCELLATION_POLL_INTERVAL)
                .filter(ignored -> isCancelled(context))
                .doOnNext(ignored -> remotelyCancelled.set(true))
                .take(1);
        return context.resolution().getChatModel().stream(modelPrompt(context, messages))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .concatMap(response -> isCancelled(context)
                        ? Flux.error(new AiChatCancelledException())
                        : mapResponse(response, context.thinkingEnabled(), accumulator))
                .takeUntilOther(cancellationSignal)
                .concatWith(Mono.defer(() -> remotelyCancelled.get() || isCancelled(context)
                        ? Mono.error(new AiChatCancelledException())
                        : completeConversation(context, accumulator, invocation)))
                .onErrorResume(AiChatCancelledException.class,
                        error -> cancelled(context, accumulator, invocation))
                .onErrorResume(error -> modelError(context, accumulator, invocation, error))
                .doOnCancel(() -> {
                    if (!invocation.isDeliveryFailed() && !invocation.isTerminalPublished()) {
                        recordInvocationOnce(invocation, context, CANCELLED, null, accumulator.tokenUsage(),
                                accumulator.content().getBytes(StandardCharsets.UTF_8).length);
                    }
                });
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
            ConversationAccumulator accumulator,
            ActiveInvocation invocation) {
        return Mono.fromCallable(() -> {
            String assistantText = accumulator.content();
            Require.isTrue(StringUtils.hasText(assistantText) || !accumulator.media().isEmpty(),
                    AiCode.CHAT_MODEL_UNAVAILABLE, "AI 模型未返回有效内容");
            List<AiMessageContentPartVO> assistantParts = assistantParts(
                    context.service(), assistantText, accumulator.media(), context.requestId());
            conversationStore.saveExchange(new AiConversationExchange(
                    conversationScope(context),
                    context.userContentParts(),
                    assistantParts,
                    context.thinkingEnabled(),
                    context.resolution()));
            invocation.prepareTerminalAudit(new TerminalAudit(
                    SUCCESS,
                    null,
                    accumulator.tokenUsage(),
                    assistantText.getBytes(StandardCharsets.UTF_8).length));
            return doneEvent(context, assistantParts);
        });
    }

    private Flux<String> modelError(
            InvocationContext context,
            ConversationAccumulator accumulator,
            ActiveInvocation invocation,
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
        invocation.prepareTerminalAudit(new TerminalAudit(
                FAILED,
                AiCode.CHAT_MODEL_UNAVAILABLE.getCode(),
                accumulator.tokenUsage(),
                accumulator.content().getBytes(StandardCharsets.UTF_8).length));
        return Flux.just(errorEvent(AiCode.CHAT_MODEL_UNAVAILABLE.getMessage()));
    }

    private Flux<String> cancelled(
            InvocationContext context,
            ConversationAccumulator accumulator,
            ActiveInvocation invocation) {
        recordInvocationOnce(invocation, context, CANCELLED, null, accumulator.tokenUsage(),
                accumulator.content().getBytes(StandardCharsets.UTF_8).length);
        return Flux.empty();
    }

    private void recordInvocationOnce(
            ActiveInvocation invocation,
            InvocationContext context,
            String result,
            Integer errorCode,
            TokenUsage usage,
            long outputBytes) {
        if (!invocation.beginAudit()) {
            return;
        }
        try {
            recordInvocation(context, result, errorCode, usage, outputBytes);
            invocation.completeAudit();
        } catch (RuntimeException exception) {
            invocation.resetAudit();
            Require.rethrow(exception);
        }
    }

    private boolean isCancelled(InvocationContext context) {
        return cache.exists(requestCancelledKey(context.requestId()));
    }

    private IAiChatConversationStore.ConversationState loadConversation(InvocationContext context) {
        return conversationStore.load(conversationScope(context), maxHistoryMessages);
    }

    private AiConversationScope conversationScope(InvocationContext context) {
        return new AiConversationScope(
                context.tenantId(),
                context.userId(),
                context.serviceCode(),
                context.sessionId());
    }

    private AiConversationScope conversationScope(String serviceCode, String sessionId) {
        return new AiConversationScope(requireTenantId(), requireUserId(), serviceCode, sessionId);
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

    private String requestLockKey(String requestId) {
        return "ai-service-chat:request-lock:" + requestId;
    }

    private String requestOwnerKey(String requestId) {
        return "ai-service-chat:request-owner:" + requestId;
    }

    private String requestCancelledKey(String requestId) {
        return "ai-service-chat:request-cancelled:" + requestId;
    }

    private String requestOwner(String tenantId, Long userId) {
        return tenantId + ':' + userId;
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
                new AiUserMessageInput(
                        contentParts,
                        context.resolution(),
                        structuredUserPrompt(context, contentParts)));
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
            JsonNode resolvedValue = Require.nonNull(value, AiCode.SERVICE_INPUT_INVALID,
                    "Prompt 变量不存在: " + matcher.group(1));
            Require.isTrue(!resolvedValue.isMissingNode(), AiCode.SERVICE_INPUT_INVALID,
                    "Prompt 变量不存在: " + matcher.group(1));
            rendered.append(resolvedValue.isTextual() ? resolvedValue.textValue() : resolvedValue.toString());
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
                parts.add(contentResolver.saveAssistantMedia(
                        new AiAssistantMediaInput(media.get(index), requestId, index + 1)));
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
                    ? normalized.substring(MARKDOWN_FENCE_LENGTH,
                            normalized.length() - MARKDOWN_FENCE_LENGTH).trim()
                    : normalized.substring(firstLine + 1,
                            normalized.length() - MARKDOWN_FENCE_LENGTH).trim();
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

    private record TerminalAudit(String result, Integer errorCode, TokenUsage usage, long outputBytes) {
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

    private static final class ActiveInvocation {
        private final AtomicReference<Disposable> subscription = new AtomicReference<>();
        private final AtomicBoolean terminated = new AtomicBoolean();
        private final AtomicBoolean deliveryFailed = new AtomicBoolean();
        private final AtomicBoolean terminalPublished = new AtomicBoolean();
        private final AtomicInteger auditState = new AtomicInteger();
        private final AtomicReference<TerminalAudit> terminalAudit = new AtomicReference<>();

        private void attach(Disposable disposable) {
            subscription.set(disposable);
            if (terminated.get()) {
                disposable.dispose();
            }
        }

        private void cancel() {
            Disposable disposable = subscription.get();
            if (disposable != null) {
                disposable.dispose();
            }
        }

        private boolean terminate() {
            return terminated.compareAndSet(false, true);
        }

        private void markDeliveryFailed() {
            deliveryFailed.set(true);
        }

        private boolean isDeliveryFailed() {
            return deliveryFailed.get();
        }

        private void markTerminalPublished() {
            terminalPublished.set(true);
        }

        private boolean isTerminalPublished() {
            return terminalPublished.get();
        }

        private void prepareTerminalAudit(TerminalAudit audit) {
            Require.isTrue(terminalAudit.compareAndSet(null, audit), AiCode.SERVICE_AUDIT_FAILED,
                    "AI 终态审计上下文重复创建");
        }

        private TerminalAudit takeTerminalAudit() {
            return terminalAudit.getAndSet(null);
        }

        private boolean beginAudit() {
            return auditState.compareAndSet(0, 1);
        }

        private void completeAudit() {
            auditState.set(2);
        }

        private void resetAudit() {
            auditState.compareAndSet(1, 0);
        }
    }

    private static final class AiChatCancelledException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
