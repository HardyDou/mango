package io.mango.job.starter.remote;

import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import io.mango.job.api.command.RegisterMangoJobWorkerCommand;
import io.mango.job.api.enums.JobTransportType;
import io.mango.job.support.service.IMangoJobHandlerRegistry;
import io.mango.job.support.nativeengine.MangoNativeJobProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

/**
 * 远程 Worker 向 JobCenter 注册自身能力。
 */
@Component
@RequiredArgsConstructor
public class MangoJobRemoteWorkerRegistrar {

    private static final Logger LOGGER = LoggerFactory.getLogger(MangoJobRemoteWorkerRegistrar.class);

    private final MangoJobFeignClient jobFeignClient;

    private final IMangoJobHandlerRegistry handlerRegistry;

    private final MangoNativeJobProperties properties;

    @EventListener(ApplicationReadyEvent.class)
    public void registerOnReady() {
        register();
    }

    @Scheduled(fixedDelayString = "${mango.job.native.worker-heartbeat-interval-millis:15000}")
    public void heartbeat() {
        register();
    }

    public void register() {
        if (!StringUtils.hasText(properties.getJobCenterAddress())) {
            return;
        }
        if (!StringUtils.hasText(properties.getWorkerAddress())) {
            LOGGER.warn("Mango Job remote worker registration skipped because worker-address is empty");
            return;
        }
        RegisterMangoJobWorkerCommand command = toCommand();
        if (command.getHandlers().isEmpty()) {
            LOGGER.warn("Mango Job remote worker registration skipped because no handler is registered, workerAddress={}",
                    command.getWorkerAddress());
            return;
        }
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            MangoContextHolder.set(MangoContextSnapshot.empty()
                    .withRequest("job-worker-register", "job-worker-register", command.getTenantId(),
                            command.getAppCode(), command.getWorkerAddress())
                    .withSecurity(null, command.getTenantId(), "job-worker", "SYSTEM",
                            "JOB", "SYSTEM", null, command.getAppCode()));
            R<Long> response = jobFeignClient.registerWorker(URI.create(properties.getJobCenterAddress().trim()),
                    command);
            Require.notNull(response, "Worker 注册 JobCenter 无响应");
            Require.isTrue(response.isSuccess(), response.getMsg());
        } catch (RuntimeException ex) {
            LOGGER.warn("Mango Job remote worker registration failed, jobCenter={}, workerAddress={}",
                    properties.getJobCenterAddress(), properties.getWorkerAddress(), ex);
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    private RegisterMangoJobWorkerCommand toCommand() {
        RegisterMangoJobWorkerCommand command = new RegisterMangoJobWorkerCommand();
        command.setTenantId(properties.getSchedulerTenantId());
        command.setAppCode(resolveAppCode());
        command.setWorkerAddress(properties.getWorkerAddress().trim());
        command.setTransportType(JobTransportType.HTTP_INTERNAL);
        command.setWorkerInstanceId(ManagementFactory.getRuntimeMXBean().getName());
        command.getHandlers().addAll(handlerRegistry.listHandlers().stream()
                .filter(handler -> command.getAppCode().equals(handler.getAppCode()))
                .toList());
        return command;
    }

    private String resolveAppCode() {
        return handlerRegistry.listHandlers().stream()
                .filter(handler -> StringUtils.hasText(handler.getAppCode()))
                .map(handler -> handler.getAppCode().trim())
                .findFirst()
                .orElse(hostName());
    }

    private String hostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            return "mango-job-worker";
        }
    }
}
