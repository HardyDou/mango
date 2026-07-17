package io.mango.job.starter.remote;

import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.job.api.enums.JobCode;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.job.api.command.RegisterMangoJobWorkerCommand;
import io.mango.job.api.command.MangoJobHandlerCommand;
import io.mango.job.api.enums.JobTransportType;
import io.mango.job.api.enums.JobWorkerRegisterSource;
import io.mango.job.api.vo.MangoJobHandlerVO;
import io.mango.job.support.service.MangoJobHandlerRegistry;
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
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 远程 Worker 向 JobCenter 注册自身能力。
 */
@Component
@RequiredArgsConstructor
public class MangoJobRemoteWorkerRegistrar {

    private static final Logger LOGGER = LoggerFactory.getLogger(MangoJobRemoteWorkerRegistrar.class);

    private final MangoJobDynamicHttpClient dynamicHttpClient;

    private final MangoJobHandlerRegistry handlerRegistry;

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
        List<RegisterMangoJobWorkerCommand> commands = toCommands();
        if (commands.isEmpty()) {
            LOGGER.warn("Mango Job remote worker registration skipped because no handler is registered, workerAddress={}",
                    properties.getWorkerAddress());
            return;
        }
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            for (RegisterMangoJobWorkerCommand command : commands) {
                MangoContextHolder.set(MangoContextSnapshot.empty()
                        .withRequest("job-worker-register", "job-worker-register", command.getTenantId(),
                                command.getAppCode(), command.getWorkerAddress())
                        .withSecurity(null, command.getTenantId(), "job-worker", "SYSTEM",
                                "JOB", "SYSTEM", null, command.getAppCode()));
                R<Long> response = dynamicHttpClient.registerWorker(URI.create(properties.getJobCenterAddress().trim()),
                        command);
                Require.notNull(response, JobCode.JOB_INVALID, "Worker 注册 JobCenter 无响应");
                Require.isTrue(response.isSuccess(), JobCode.JOB_INVALID, response.getMsg());
            }
        } catch (RuntimeException ex) {
            LOGGER.warn("Mango Job remote worker registration failed, jobCenter={}, workerAddress={}",
                    properties.getJobCenterAddress(), properties.getWorkerAddress(), ex);
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    private List<RegisterMangoJobWorkerCommand> toCommands() {
        return handlerRegistry.listHandlers().stream()
                .collect(Collectors.groupingBy(this::workerRegistrationKey))
                .values()
                .stream()
                .map(this::toCommand)
                .toList();
    }

    private RegisterMangoJobWorkerCommand toCommand(List<MangoJobHandlerVO> handlers) {
        MangoJobHandlerVO first = handlers.get(0);
        RegisterMangoJobWorkerCommand command = new RegisterMangoJobWorkerCommand();
        command.setTenantId(properties.getSchedulerTenantId());
        command.setAppCode(first.getAppCode());
        command.setServiceCode(first.getServiceCode());
        command.setWorkerGroup(first.getWorkerGroup());
        command.setWorkerAddress(properties.getWorkerAddress().trim());
        command.setRuntimeAddress(properties.getWorkerAddress().trim());
        command.setTransportType(JobTransportType.HTTP_INTERNAL);
        command.setRegisterSource(JobWorkerRegisterSource.REMOTE_AUTO);
        command.setWorkerInstanceId(ManagementFactory.getRuntimeMXBean().getName());
        command.setHandlers(handlers.stream().map(this::toHandlerCommand).toList());
        return command;
    }

    private MangoJobHandlerCommand toHandlerCommand(MangoJobHandlerVO handler) {
        MangoJobHandlerCommand command = new MangoJobHandlerCommand();
        command.setAppCode(handler.getAppCode());
        command.setServiceCode(handler.getServiceCode());
        command.setWorkerGroup(handler.getWorkerGroup());
        command.setHandlerName(handler.getHandlerName());
        command.setSupportedJobCodes(handler.getSupportedJobCodes());
        command.setJobType(handler.getJobType());
        command.setParamSchema(handler.getParamSchema());
        command.setConcurrent(handler.getConcurrent());
        command.setTimeoutSeconds(handler.getTimeoutSeconds());
        command.setRetryPolicy(handler.getRetryPolicy());
        return command;
    }

    private String workerRegistrationKey(MangoJobHandlerVO handler) {
        return handler.getAppCode() + ":" + handler.getServiceCode() + ":" + handler.getWorkerGroup();
    }
}
