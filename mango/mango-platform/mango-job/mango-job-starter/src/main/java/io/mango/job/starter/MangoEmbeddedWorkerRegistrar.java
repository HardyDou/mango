package io.mango.job.starter;

import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.job.core.service.nativeengine.IMangoNativeJobRuntime;
import io.mango.job.support.nativeengine.MangoNativeJobProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * Mango 原生内嵌 Worker 自动注册和心跳。
 */
@RequiredArgsConstructor
public class MangoEmbeddedWorkerRegistrar {

    private static final Logger LOGGER = LoggerFactory.getLogger(MangoEmbeddedWorkerRegistrar.class);

    private final IMangoNativeJobRuntime nativeJobRuntime;

    private final MangoNativeJobProperties properties;

    @EventListener(ApplicationReadyEvent.class)
    public void registerOnReady() {
        register();
    }

    @Scheduled(fixedDelayString = "${mango.job.native.worker-heartbeat-interval-millis:15000}")
    public void heartbeat() {
        register();
    }

    private void register() {
        if (!properties.isEmbeddedWorkerEnabled()) {
            LOGGER.debug("Mango embedded job worker registration skipped, embeddedWorkerEnabled=false");
            return;
        }
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            String tenantId = schedulerTenantId();
            MangoContextHolder.set(workerContext(tenantId));
            nativeJobRuntime.registerEmbeddedWorkers(tenantId);
        } catch (RuntimeException ex) {
            LOGGER.warn("Mango embedded job worker registration failed", ex);
            throw ex;
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    private MangoContextSnapshot workerContext(String tenantId) {
        String traceId = "job-worker-heartbeat-" + UUID.randomUUID();
        return MangoContextSnapshot.empty()
                .withRequest(traceId, traceId, tenantId, "mango-job", "embedded-worker")
                .withSecurity(0L, tenantId, "job-system", "SYSTEM", "JOB", "SYSTEM", 0L, "mango-job");
    }

    private String schedulerTenantId() {
        if (StringUtils.hasText(properties.getSchedulerTenantId())) {
            return properties.getSchedulerTenantId().trim();
        }
        return "1";
    }
}
