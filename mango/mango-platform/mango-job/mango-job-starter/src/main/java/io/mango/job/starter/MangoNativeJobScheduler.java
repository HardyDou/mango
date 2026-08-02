package io.mango.job.starter;

import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.job.core.service.nativeengine.IMangoNativeJobRuntime;
import io.mango.job.support.nativeengine.MangoNativeJobProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * Mango 原生 Job 调度扫描驱动。
 */
@RequiredArgsConstructor
public class MangoNativeJobScheduler {

    private final IMangoNativeJobRuntime nativeJobRuntime;

    private final MangoNativeJobProperties properties;

    private volatile boolean ready;

    @EventListener(ApplicationReadyEvent.class)
    public void startOnReady() {
        ready = true;
        tick();
    }

    @Scheduled(fixedDelayString = "${mango.job.native.scan-interval-millis:5000}")
    public void tick() {
        if (ready && properties.isSchedulerEnabled()) {
            MangoContextSnapshot previous = MangoContextHolder.get();
            try {
                MangoContextHolder.set(schedulerContext());
                nativeJobRuntime.tick();
            } finally {
                MangoContextHolder.set(previous);
            }
        }
    }

    private MangoContextSnapshot schedulerContext() {
        String traceId = "job-scheduler-" + UUID.randomUUID();
        String tenantId = StringUtils.hasText(properties.getSchedulerTenantId())
                ? properties.getSchedulerTenantId().trim()
                : "1";
        return MangoContextSnapshot.empty()
                .withRequest(traceId, traceId, tenantId, "mango-job", "scheduler")
                .withSecurity(0L, tenantId, "job-system", "SYSTEM", "JOB", "SYSTEM", 0L, "mango-job");
    }
}
