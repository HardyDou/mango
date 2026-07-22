package io.mango.resource.sync.starter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.exception.BizException;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.resource.api.ResourceDeclarationApi;
import io.mango.resource.api.command.RegisterResourceDeclarationsCommand;
import io.mango.resource.api.enums.ResourceCode;
import io.mango.resource.support.config.ResourceRegistryProperties;
import io.mango.resource.support.declaration.InvalidResourceDeclarationException;
import io.mango.resource.support.declaration.ResourceDeclarationCollector;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.sync.ResourceSynchronizationCompletedEvent;
import io.mango.resource.support.sync.ResourceSynchronizationPrerequisitesReadyEvent;
import io.mango.resource.support.sync.ResourceSynchronizationStatus;
import io.mango.resource.support.sync.StartupReadinessChangedEvent;
import io.mango.resource.support.sync.StartupReadinessState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 扫描当前应用资源声明并调用资源注册中心 API。
 */
@Slf4j
@RequiredArgsConstructor
public class ResourceSyncRunner implements ApplicationRunner, Ordered, ResourceSynchronizationStatus {

    private static final int RESOURCE_SYNC_ORDER_OFFSET = 50;

    private final ResourceRegistryProperties properties;
    private final ResourceDeclarationCollector collector;
    private final ResourceDeclarationApi resourceDeclarationApi;
    private final ObjectMapper objectMapper;
    private final String applicationName;
    private final ApplicationEventPublisher eventPublisher;
    private final AtomicBoolean syncCompleted = new AtomicBoolean();
    private final AtomicBoolean syncInProgress = new AtomicBoolean();
    private final AtomicReference<StartupReadinessState> readinessState =
            new AtomicReference<>(StartupReadinessState.BOOTSTRAPPING);
    private final AtomicInteger transientFailures = new AtomicInteger();
    private volatile String permanentlyFailedSnapshot;
    private volatile long nextRetryAtMillis;
    private volatile long lastAttemptAtMillis;
    private volatile long lastFailureAtMillis;
    private volatile String lastErrorType;

    @Override
    public void run(ApplicationArguments args) {
        trySync(false, true);
    }

    /**
     * 远程目标或其前置资源尚未就绪时持续重试，避免微服务启动顺序决定资源是否最终收敛。
     */
    @Scheduled(
            fixedDelayString = "${mango.resource.registry.remote.retry-interval:10s}",
            initialDelayString = "${mango.resource.registry.remote.retry-interval:10s}")
    public void retryUntilSynchronized() {
        if (!syncCompleted.get()) {
            trySync(true, false);
        }
    }

    /**
     * Retries immediately after tenant provisioning creates declaration prerequisites such as built-in roles.
     *
     * @param event startup prerequisites ready event
     */
    @EventListener
    public void onSynchronizationPrerequisitesReady(ResourceSynchronizationPrerequisitesReadyEvent event) {
        if (!syncCompleted.get()) {
            trySync(true, true);
        }
    }

    private void trySync(boolean publishCompletionEvent, boolean forceRetry) {
        if (!forceRetry && !isRetryDue()) {
            return;
        }
        if (!syncInProgress.compareAndSet(false, true)) {
            return;
        }
        lastAttemptAtMillis = System.currentTimeMillis();
        boolean notifyCompletion = false;
        String attemptSnapshot = null;
        try {
            SyncRequest request = prepareSyncRequest();
            attemptSnapshot = request.snapshot();
            if (readinessState.get() == StartupReadinessState.PERMANENT_FAILED
                    && request.snapshot().equals(permanentlyFailedSnapshot)) {
                return;
            }
            transitionTo(StartupReadinessState.SYNCING);
            synchronizeDeclarations(request);
            transientFailures.set(0);
            nextRetryAtMillis = 0L;
            lastFailureAtMillis = 0L;
            lastErrorType = null;
            permanentlyFailedSnapshot = null;
            notifyCompletion = syncCompleted.compareAndSet(false, true) && publishCompletionEvent;
            transitionTo(StartupReadinessState.READY);
        } catch (RuntimeException exception) {
            handleFailure(exception, attemptSnapshot);
        } finally {
            syncInProgress.set(false);
        }
        if (notifyCompletion) {
            publishCompletionEvent();
        }
    }

    private void publishCompletionEvent() {
        try {
            eventPublisher.publishEvent(new ResourceSynchronizationCompletedEvent(applicationName));
        } catch (RuntimeException exception) {
            log.error("Resource synchronization completion listener failed: application={}",
                    applicationName, exception);
        }
    }

    @Override
    public boolean isSynchronizationComplete() {
        return syncCompleted.get();
    }

    @Override
    public StartupReadinessState getReadinessState() {
        return readinessState.get();
    }

    @Override
    public Map<String, Object> getReadinessDetails() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("state", getReadinessState().name());
        details.put("failureCount", transientFailures.get());
        putTimestamp(details, "lastAttemptAt", lastAttemptAtMillis);
        putTimestamp(details, "lastFailureAt", lastFailureAtMillis);
        putTimestamp(details, "nextRetryAt", nextRetryAtMillis);
        if (lastErrorType != null) {
            details.put("lastErrorType", lastErrorType);
        }
        return details;
    }

    private SyncRequest prepareSyncRequest() {
        if (!properties.isEnabled() || !properties.getRemote().isEnabled()) {
            return SyncRequest.skipped("disabled");
        }
        List<ResourceDeclaration> declarations = collector.collect();
        List<String> moduleCodes = collector.managedModuleCodes(declarations).stream().sorted().toList();
        if (declarations.isEmpty() && moduleCodes.isEmpty()) {
            return SyncRequest.skipped("empty");
        }
        RegisterResourceDeclarationsCommand command = new RegisterResourceDeclarationsCommand();
        command.setAppCode(resolveAppCode());
        command.setServiceCode(resolveServiceCode());
        command.setModuleCodes(moduleCodes);
        command.setDeclarations(serializeDeclarations(declarations));
        return new SyncRequest(command, declarations.size(), snapshot(command), null);
    }

    private void synchronizeDeclarations(SyncRequest request) {
        if (request.skipReason() != null) {
            log.info("Mango resource declaration sync skipped: reason={}", request.skipReason());
            return;
        }
        RegisterResourceDeclarationsCommand command = request.command();
        R<Boolean> response = resourceDeclarationApi.registerDeclarations(command);
        if (response == null) {
            throw SyncFailure.transientFailure("资源注册中心无响应");
        }
        if (!response.isSuccess()) {
            throw response.getCode() >= 400 && response.getCode() < 500
                    ? SyncFailure.permanentFailure(response.getMsg())
                    : SyncFailure.transientFailure(response.getMsg());
        }
        if (!Boolean.TRUE.equals(response.getData())) {
            throw SyncFailure.transientFailure("资源注册中心同步尚未完成");
        }
        log.info("Mango resource declaration sync complete: appCode={}, serviceCode={}, declarations={}",
                command.getAppCode(), command.getServiceCode(), request.declarationCount());
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - RESOURCE_SYNC_ORDER_OFFSET;
    }

    private String resolveAppCode() {
        String appCode = properties.getRemote().getAppCode();
        if (StringUtils.hasText(appCode)) {
            return appCode;
        }
        Require.notBlank(applicationName, ResourceCode.RESOURCE_INVALID, "资源注册 appCode 不能为空");
        return applicationName;
    }

    private String resolveServiceCode() {
        String serviceCode = properties.getRemote().getServiceCode();
        if (StringUtils.hasText(serviceCode)) {
            return serviceCode;
        }
        return applicationName;
    }

    private String serializeDeclarations(List<ResourceDeclaration> declarations) {
        try {
            return objectMapper.writeValueAsString(declarations);
        } catch (JsonProcessingException exception) {
            throw SyncFailure.permanentFailure("资源声明序列化失败", exception);
        }
    }

    private boolean isRetryDue() {
        return readinessState.get() != StartupReadinessState.TRANSIENT_WAIT
                || System.currentTimeMillis() >= nextRetryAtMillis;
    }

    private void handleFailure(RuntimeException exception, String snapshot) {
        lastFailureAtMillis = System.currentTimeMillis();
        lastErrorType = exception.getClass().getSimpleName();
        boolean permanent = exception instanceof SyncFailure failure && failure.permanent()
                || exception instanceof InvalidResourceDeclarationException
                || exception instanceof BizException bizException
                && bizException.getCode() >= 400 && bizException.getCode() < 500;
        if (permanent) {
            String failedSnapshot = snapshot == null ? failureFingerprint(exception) : snapshot;
            if (readinessState.get() == StartupReadinessState.PERMANENT_FAILED
                    && Objects.equals(permanentlyFailedSnapshot, failedSnapshot)) {
                return;
            }
            permanentlyFailedSnapshot = failedSnapshot;
            transitionTo(StartupReadinessState.PERMANENT_FAILED);
            log.error("Mango resource declaration sync permanently failed for current snapshot: application={}, error={}",
                    applicationName, exception.getMessage(), exception);
            return;
        }
        int failureCount = transientFailures.incrementAndGet();
        long retryDelayMillis = retryDelayMillis(failureCount);
        nextRetryAtMillis = System.currentTimeMillis() + retryDelayMillis;
        transitionTo(StartupReadinessState.TRANSIENT_WAIT);
        log.warn("Mango resource declaration sync deferred: application={}, retryInMs={}, failureCount={}, error={}",
                applicationName, retryDelayMillis, failureCount, exception.getMessage());
    }

    private long retryDelayMillis(int failureCount) {
        Duration initial = positiveDuration(properties.getRemote().getRetryInterval(), Duration.ofSeconds(10));
        Duration maximum = positiveDuration(properties.getRemote().getRetryMaxInterval(), Duration.ofMinutes(1));
        long initialMillis = initial.toMillis();
        long maximumMillis = Math.max(initialMillis, maximum.toMillis());
        int shift = Math.min(Math.max(0, failureCount - 1), Long.SIZE - 2);
        long exponential = initialMillis > (maximumMillis >> shift)
                ? maximumMillis : initialMillis << shift;
        long bounded = Math.min(exponential, maximumMillis);
        long jitterRange = Math.max(1L, bounded / 5L);
        long jitter = ThreadLocalRandom.current().nextLong(-jitterRange, jitterRange + 1L);
        return Math.max(1L, bounded + jitter);
    }

    private Duration positiveDuration(Duration configured, Duration defaultValue) {
        return configured == null || configured.isNegative() || configured.isZero() ? defaultValue : configured;
    }

    private void transitionTo(StartupReadinessState nextState) {
        StartupReadinessState previous = readinessState.getAndSet(nextState);
        if (previous == nextState) {
            return;
        }
        try {
            eventPublisher.publishEvent(new StartupReadinessChangedEvent(getReadinessComponent(), nextState));
        } catch (RuntimeException exception) {
            log.error("Resource startup readiness listener failed: application={}, state={}",
                    applicationName, nextState, exception);
        }
    }

    private String snapshot(RegisterResourceDeclarationsCommand command) {
        String source = command.getAppCode() + '\n' + command.getServiceCode() + '\n'
                + String.join(",", command.getModuleCodes()) + '\n' + command.getDeclarations();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(source.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String failureFingerprint(Throwable failure) {
        StringBuilder source = new StringBuilder();
        Throwable current = failure;
        while (current != null) {
            source.append(current.getClass().getName())
                    .append(':')
                    .append(current.getMessage())
                    .append('\n');
            current = current.getCause();
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(source.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private void putTimestamp(Map<String, Object> details, String name, long epochMillis) {
        if (epochMillis > 0L) {
            details.put(name, Instant.ofEpochMilli(epochMillis).toString());
        }
    }

    private record SyncRequest(RegisterResourceDeclarationsCommand command,
                               int declarationCount,
                               String snapshot,
                               String skipReason) {

        private static SyncRequest skipped(String reason) {
            return new SyncRequest(null, 0, reason, reason);
        }
    }

    private static final class SyncFailure extends RuntimeException {

        private final boolean permanent;

        private SyncFailure(String message, Throwable cause, boolean permanent) {
            super(message, cause);
            this.permanent = permanent;
        }

        private static SyncFailure transientFailure(String message) {
            return new SyncFailure(message, null, false);
        }

        private static SyncFailure permanentFailure(String message) {
            return permanentFailure(message, null);
        }

        private static SyncFailure permanentFailure(String message, Throwable cause) {
            return new SyncFailure(message, cause, true);
        }

        private boolean permanent() {
            return permanent;
        }
    }
}
