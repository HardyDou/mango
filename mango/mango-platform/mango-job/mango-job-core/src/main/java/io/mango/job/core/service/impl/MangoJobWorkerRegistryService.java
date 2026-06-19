package io.mango.job.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.common.result.Require;
import io.mango.job.api.command.CreateMangoJobWorkerCommand;
import io.mango.job.api.command.RegisterMangoJobWorkerCommand;
import io.mango.job.api.command.UpdateMangoJobWorkerStatusCommand;
import io.mango.job.api.enums.JobEngineType;
import io.mango.job.api.enums.JobTransportType;
import io.mango.job.api.enums.JobWorkerRegisterSource;
import io.mango.job.api.enums.JobWorkerStatus;
import io.mango.job.api.vo.MangoJobHandlerVO;
import io.mango.job.core.entity.MangoJobWorkerCapabilityEntity;
import io.mango.job.core.entity.MangoJobWorkerSnapshotEntity;
import io.mango.job.core.mapper.MangoJobWorkerCapabilityMapper;
import io.mango.job.core.mapper.MangoJobWorkerSnapshotMapper;
import io.mango.job.core.service.IMangoJobWorkerRegistryService;
import io.mango.job.support.nativeengine.MangoJobTransportAddresses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mango Job Worker 注册服务实现。
 */
@Service
public class MangoJobWorkerRegistryService implements IMangoJobWorkerRegistryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MangoJobWorkerRegistryService.class);

    private static final int MAX_HANDLER_NAME_LENGTH = 128;

    private final MangoJobWorkerSnapshotMapper workerSnapshotMapper;

    private final MangoJobWorkerCapabilityMapper workerCapabilityMapper;

    private final MangoJobDataSourceRouter dataSourceRouter;

    public MangoJobWorkerRegistryService(MangoJobWorkerSnapshotMapper workerSnapshotMapper,
                                         MangoJobWorkerCapabilityMapper workerCapabilityMapper,
                                         MangoJobDataSourceRouter dataSourceRouter) {
        this.workerSnapshotMapper = workerSnapshotMapper;
        this.workerCapabilityMapper = workerCapabilityMapper;
        this.dataSourceRouter = dataSourceRouter;
    }

    @Override
    public Long registerWorker(RegisterMangoJobWorkerCommand command) {
        Require.notNull(command, "Worker 注册命令不能为空");
        return dataSourceRouter.route(() -> {
            validate(command);
            MangoJobWorkerSnapshotEntity worker = upsertWorker(command, false);
            upsertCapabilities(command, worker);
            disableMissingCapabilities(command, worker);
            LOGGER.info("Mango job worker registered, workerId={}, appCode={}, serviceCode={}, workerGroup={}, transportType={}, registerSource={}, handlerCount={}, workerAddress={}",
                    worker.getId(), worker.getAppCode(), worker.getServiceCode(), worker.getWorkerGroup(),
                    worker.getTransportType(), worker.getRegisterSource(), command.getHandlers().size(), worker.getWorkerAddress());
            return worker.getId();
        });
    }

    @Override
    public Long createWorker(CreateMangoJobWorkerCommand command) {
        Require.notNull(command, "Worker 登记命令不能为空");
        RegisterMangoJobWorkerCommand registerCommand = new RegisterMangoJobWorkerCommand();
        registerCommand.setTenantId(currentTenantId());
        registerCommand.setAppCode(command.getAppCode());
        registerCommand.setServiceCode(command.getServiceCode());
        registerCommand.setWorkerGroup(command.getWorkerGroup());
        registerCommand.setWorkerAddress(command.getWorkerAddress());
        registerCommand.setRuntimeAddress(command.getRuntimeAddress());
        registerCommand.setTransportType(command.getTransportType());
        registerCommand.setRegisterSource(JobWorkerRegisterSource.MANUAL);
        registerCommand.setWorkerInstanceId(command.getWorkerInstanceId());
        registerCommand.setHandlers(command.getHandlers());
        return dataSourceRouter.route(() -> {
            validate(registerCommand);
            Require.isTrue(command.getTransportType() == JobTransportType.HTTP_INTERNAL,
                    "手动登记 Worker 仅支持 HTTP_INTERNAL，内嵌 Worker 由系统自动注册");
            MangoJobWorkerSnapshotEntity worker = upsertWorker(registerCommand, true);
            upsertCapabilities(registerCommand, worker);
            disableMissingCapabilities(registerCommand, worker);
            return worker.getId();
        });
    }

    @Override
    public Boolean updateWorkerStatus(UpdateMangoJobWorkerStatusCommand command) {
        Require.notNull(command, "Worker 状态命令不能为空");
        Require.notNull(command.getId(), "Worker ID 不能为空");
        Require.notNull(command.getStatus(), "Worker 状态不能为空");
        Require.isTrue(manualStatuses().contains(command.getStatus()), "不支持的 Worker 治理状态：" + command.getStatus());
        return dataSourceRouter.route(() -> {
            String tenantId = currentTenantId();
            MangoJobWorkerSnapshotEntity worker = workerSnapshotMapper.selectById(command.getId());
            Require.notNull(worker, 404, "Worker 不存在");
            Require.isTrue(tenantId.equals(worker.getTenantId()), 404, "Worker 不存在");
            int updated = workerSnapshotMapper.update(null, new LambdaUpdateWrapper<MangoJobWorkerSnapshotEntity>()
                    .eq(MangoJobWorkerSnapshotEntity::getId, command.getId())
                    .eq(MangoJobWorkerSnapshotEntity::getTenantId, tenantId)
                    .set(MangoJobWorkerSnapshotEntity::getStatus, command.getStatus().name()));
            return updated > 0;
        });
    }

    private void validate(RegisterMangoJobWorkerCommand command) {
        Require.notBlank(command.getTenantId(), "租户 ID 不能为空");
        Require.notBlank(command.getAppCode(), "所属应用不能为空");
        resolveServiceCode(command);
        resolveWorkerGroup(command);
        Require.notBlank(command.getWorkerAddress(), "Worker 地址不能为空");
        Require.notNull(command.getTransportType(), "通信方式不能为空");
        Require.notNull(resolveRegisterSource(command), "注册来源不能为空");
        Require.isTrue(MangoJobSupport.hasValidWorkerAddress(command.getWorkerAddress()), "Worker 地址无效");
        if (command.getTransportType() == JobTransportType.IN_MEMORY) {
            Require.isTrue(command.getRegisterSource() == JobWorkerRegisterSource.EMBEDDED_AUTO,
                    "IN_MEMORY Worker 只能由系统自动注册");
            Require.isTrue(MangoJobTransportAddresses.isEmbedded(command.getWorkerAddress()),
                    "IN_MEMORY Worker 地址必须使用 " + MangoJobTransportAddresses.EMBEDDED_PREFIX
                            + " 或兼容旧地址 " + MangoJobTransportAddresses.IN_MEMORY_PREFIX);
        }
        if (command.getTransportType() == JobTransportType.HTTP_INTERNAL) {
            Require.isTrue(MangoJobTransportAddresses.isHttpInternal(command.getWorkerAddress()),
                    "HTTP_INTERNAL Worker 地址必须使用 http(s)://");
        }
        Require.isTrue(command.getHandlers() != null && !command.getHandlers().isEmpty(), "Worker 处理器清单不能为空");
        for (MangoJobHandlerVO handler : command.getHandlers()) {
            Require.notNull(handler, "Worker 处理器不能为空");
            Require.notBlank(handler.getHandlerName(), "Worker 处理器名称不能为空");
            Require.isTrue(handler.getHandlerName().trim().length() <= MAX_HANDLER_NAME_LENGTH,
                    "Worker 处理器名称不能超过128个字符");
        }
    }

    private MangoJobWorkerSnapshotEntity upsertWorker(RegisterMangoJobWorkerCommand command, boolean forceOnline) {
        String tenantId = command.getTenantId().trim();
        String appCode = command.getAppCode().trim();
        String serviceCode = resolveServiceCode(command);
        String workerGroup = resolveWorkerGroup(command);
        String address = command.getWorkerAddress().trim();
        LocalDateTime now = LocalDateTime.now();
        MangoJobWorkerSnapshotEntity existing = selectWorker(tenantId, serviceCode, workerGroup, address);
        int updated = updateWorkerHeartbeat(command, tenantId, serviceCode, workerGroup, address, now,
                forceOnline || canHeartbeatSetOnline(existing));
        if (updated > 0) {
            return selectWorker(tenantId, serviceCode, workerGroup, address);
        }

        MangoJobWorkerSnapshotEntity worker = new MangoJobWorkerSnapshotEntity();
        worker.setTenantId(tenantId);
        worker.setAppCode(appCode);
        worker.setServiceCode(serviceCode);
        worker.setWorkerGroup(workerGroup);
        worker.setWorkerAddress(address);
        worker.setRuntimeAddress(resolveRuntimeAddress(command, address));
        worker.setTransportType(command.getTransportType().name());
        worker.setRegisterSource(resolveRegisterSource(command).name());
        worker.setEngineType(JobEngineType.MANGO_NATIVE.name());
        worker.setEngineWorkerId(resolveWorkerInstanceId(command, address));
        worker.setInstanceId(resolveWorkerInstanceId(command, address));
        worker.setStatus(JobWorkerStatus.ONLINE.name());
        worker.setLastHeartbeatAt(now);
        try {
            workerSnapshotMapper.insert(worker);
            return worker;
        } catch (DuplicateKeyException ex) {
            updateWorkerHeartbeat(command, tenantId, serviceCode, workerGroup, address, now, true);
            return selectWorker(tenantId, serviceCode, workerGroup, address);
        }
    }

    private int updateWorkerHeartbeat(RegisterMangoJobWorkerCommand command,
                                      String tenantId,
                                      String serviceCode,
                                      String workerGroup,
                                      String address,
                                      LocalDateTime now,
                                      boolean setOnline) {
        LambdaUpdateWrapper<MangoJobWorkerSnapshotEntity> wrapper = new LambdaUpdateWrapper<MangoJobWorkerSnapshotEntity>()
                .eq(MangoJobWorkerSnapshotEntity::getTenantId, tenantId)
                .eq(MangoJobWorkerSnapshotEntity::getServiceCode, serviceCode)
                .eq(MangoJobWorkerSnapshotEntity::getWorkerGroup, workerGroup)
                .eq(MangoJobWorkerSnapshotEntity::getEngineType, JobEngineType.MANGO_NATIVE.name())
                .eq(MangoJobWorkerSnapshotEntity::getWorkerAddress, address)
                .set(MangoJobWorkerSnapshotEntity::getAppCode, command.getAppCode().trim())
                .set(MangoJobWorkerSnapshotEntity::getRuntimeAddress, resolveRuntimeAddress(command, address))
                .set(MangoJobWorkerSnapshotEntity::getTransportType, command.getTransportType().name())
                .set(MangoJobWorkerSnapshotEntity::getRegisterSource, resolveRegisterSource(command).name())
                .set(MangoJobWorkerSnapshotEntity::getEngineWorkerId, resolveWorkerInstanceId(command, address))
                .set(MangoJobWorkerSnapshotEntity::getInstanceId, resolveWorkerInstanceId(command, address))
                .set(MangoJobWorkerSnapshotEntity::getLastHeartbeatAt, now);
        if (setOnline) {
            wrapper.set(MangoJobWorkerSnapshotEntity::getStatus, JobWorkerStatus.ONLINE.name());
        }
        return workerSnapshotMapper.update(null, wrapper);
    }

    private MangoJobWorkerSnapshotEntity selectWorker(String tenantId, String serviceCode, String workerGroup, String address) {
        return workerSnapshotMapper.selectOne(new LambdaQueryWrapper<MangoJobWorkerSnapshotEntity>()
                .eq(MangoJobWorkerSnapshotEntity::getTenantId, tenantId)
                .eq(MangoJobWorkerSnapshotEntity::getServiceCode, serviceCode)
                .eq(MangoJobWorkerSnapshotEntity::getWorkerGroup, workerGroup)
                .eq(MangoJobWorkerSnapshotEntity::getEngineType, JobEngineType.MANGO_NATIVE.name())
                .eq(MangoJobWorkerSnapshotEntity::getWorkerAddress, address)
                .last("limit 1"));
    }

    private String resolveWorkerInstanceId(RegisterMangoJobWorkerCommand command, String address) {
        if (StringUtils.hasText(command.getWorkerInstanceId())) {
            return command.getWorkerInstanceId().trim();
        }
        return address;
    }

    private void upsertCapabilities(RegisterMangoJobWorkerCommand command, MangoJobWorkerSnapshotEntity worker) {
        for (MangoJobHandlerVO handler : command.getHandlers()) {
            String handlerName = handler.getHandlerName().trim();
            String appCode = resolveCapabilityAppCode(handler, command);
            String serviceCode = resolveCapabilityServiceCode(handler, command);
            String workerGroup = resolveCapabilityWorkerGroup(handler, command);
            Set<String> jobCodes = resolveCapabilityJobCodes(handler);
            if (jobCodes.isEmpty()) {
                upsertCapability(command, worker, handler, handlerName, appCode, serviceCode, workerGroup, null);
                continue;
            }
            for (String jobCode : jobCodes) {
                upsertCapability(command, worker, handler, handlerName, appCode, serviceCode, workerGroup, jobCode);
            }
        }
    }

    private void upsertCapability(RegisterMangoJobWorkerCommand command,
                                  MangoJobWorkerSnapshotEntity worker,
                                  MangoJobHandlerVO handler,
                                  String handlerName,
                                  String appCode,
                                  String serviceCode,
                                  String workerGroup,
                                  String jobCode) {
        String normalizedJobCode = capabilityJobCode(jobCode);
        MangoJobWorkerCapabilityEntity capability = selectCapability(worker.getId(), serviceCode, workerGroup,
                appCode, handlerName, normalizedJobCode);
        if (capability == null) {
            capability = new MangoJobWorkerCapabilityEntity();
            capability.setTenantId(command.getTenantId().trim());
            capability.setWorkerId(worker.getId());
            capability.setServiceCode(serviceCode);
            capability.setWorkerGroup(workerGroup);
            capability.setAppCode(appCode);
            capability.setJobCode(normalizedJobCode);
            capability.setHandlerName(handlerName);
            capability.setHandlerVersion(null);
            capability.setEnabled(1);
            capability.setParamSchemaHash(schemaHash(handler.getParamSchema()));
            try {
                workerCapabilityMapper.insert(capability);
            } catch (DuplicateKeyException ex) {
                capability = selectCapability(worker.getId(), serviceCode, workerGroup, appCode,
                        handlerName, normalizedJobCode);
                Require.notNull(capability, "Worker 能力并发注册失败");
                capability.setEnabled(1);
                capability.setParamSchemaHash(schemaHash(handler.getParamSchema()));
                workerCapabilityMapper.updateById(capability);
            }
            return;
        }
        capability.setEnabled(1);
        capability.setParamSchemaHash(schemaHash(handler.getParamSchema()));
        workerCapabilityMapper.updateById(capability);
    }

    private MangoJobWorkerCapabilityEntity selectCapability(Long workerId,
                                                            String serviceCode,
                                                            String workerGroup,
                                                            String appCode,
                                                            String handlerName,
                                                            String jobCode) {
        return workerCapabilityMapper.selectOne(new LambdaQueryWrapper<MangoJobWorkerCapabilityEntity>()
                .eq(MangoJobWorkerCapabilityEntity::getWorkerId, workerId)
                .eq(MangoJobWorkerCapabilityEntity::getServiceCode, serviceCode)
                .eq(MangoJobWorkerCapabilityEntity::getWorkerGroup, workerGroup)
                .eq(MangoJobWorkerCapabilityEntity::getAppCode, appCode)
                .eq(MangoJobWorkerCapabilityEntity::getHandlerName, handlerName)
                .eq(MangoJobWorkerCapabilityEntity::getJobCode, jobCode)
                .last("limit 1"));
    }

    private void disableMissingCapabilities(RegisterMangoJobWorkerCommand command, MangoJobWorkerSnapshotEntity worker) {
        Set<String> activeKeys = command.getHandlers().stream()
                .filter(Objects::nonNull)
                .map(handler -> {
                    String appCode = resolveCapabilityAppCode(handler, command);
                    String serviceCode = resolveCapabilityServiceCode(handler, command);
                    String workerGroup = resolveCapabilityWorkerGroup(handler, command);
                    Set<String> jobCodes = resolveCapabilityJobCodes(handler);
                    if (jobCodes.isEmpty()) {
                        return Set.of(serviceCode + ":" + workerGroup + ":" + appCode + ":"
                                + handler.getHandlerName().trim() + ":");
                    }
                    return jobCodes.stream()
                            .map(jobCode -> serviceCode + ":" + workerGroup + ":" + appCode + ":"
                                    + handler.getHandlerName().trim() + ":" + jobCode)
                            .collect(Collectors.toSet());
                })
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
        for (MangoJobWorkerCapabilityEntity capability : workerCapabilityMapper.selectList(
                new LambdaQueryWrapper<MangoJobWorkerCapabilityEntity>()
                        .eq(MangoJobWorkerCapabilityEntity::getWorkerId, worker.getId()))) {
            String key = capability.getServiceCode() + ":" + capability.getWorkerGroup() + ":"
                    + capability.getAppCode() + ":" + capability.getHandlerName() + ":"
                    + capabilityJobCode(capability.getJobCode());
            if (!activeKeys.contains(key)) {
                workerCapabilityMapper.update(null, new LambdaUpdateWrapper<MangoJobWorkerCapabilityEntity>()
                        .eq(MangoJobWorkerCapabilityEntity::getId, capability.getId())
                        .set(MangoJobWorkerCapabilityEntity::getEnabled, 0));
            }
        }
    }

    private String schemaHash(String schema) {
        if (!StringUtils.hasText(schema)) {
            return null;
        }
        return DigestUtils.md5DigestAsHex(schema.getBytes(StandardCharsets.UTF_8));
    }

    private String resolveCapabilityAppCode(MangoJobHandlerVO handler, RegisterMangoJobWorkerCommand command) {
        if (StringUtils.hasText(handler.getAppCode())) {
            return handler.getAppCode().trim();
        }
        return command.getAppCode().trim();
    }

    private String resolveServiceCode(RegisterMangoJobWorkerCommand command) {
        if (StringUtils.hasText(command.getServiceCode())) {
            return command.getServiceCode().trim();
        }
        return command.getAppCode().trim();
    }

    private String resolveWorkerGroup(RegisterMangoJobWorkerCommand command) {
        if (StringUtils.hasText(command.getWorkerGroup())) {
            return command.getWorkerGroup().trim();
        }
        return resolveServiceCode(command);
    }

    private String resolveCapabilityServiceCode(MangoJobHandlerVO handler, RegisterMangoJobWorkerCommand command) {
        if (StringUtils.hasText(handler.getServiceCode())) {
            return handler.getServiceCode().trim();
        }
        return resolveServiceCode(command);
    }

    private String resolveCapabilityWorkerGroup(MangoJobHandlerVO handler, RegisterMangoJobWorkerCommand command) {
        if (StringUtils.hasText(handler.getWorkerGroup())) {
            return handler.getWorkerGroup().trim();
        }
        return resolveCapabilityServiceCode(handler, command);
    }

    private Set<String> resolveCapabilityJobCodes(MangoJobHandlerVO handler) {
        if (handler.getSupportedJobCodes() == null || handler.getSupportedJobCodes().isEmpty()) {
            return Set.of();
        }
        return handler.getSupportedJobCodes().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    private String capabilityJobCode(String jobCode) {
        return StringUtils.hasText(jobCode) ? jobCode.trim() : "";
    }

    private String resolveRuntimeAddress(RegisterMangoJobWorkerCommand command, String address) {
        if (StringUtils.hasText(command.getRuntimeAddress())) {
            return command.getRuntimeAddress().trim();
        }
        return address;
    }

    private JobWorkerRegisterSource resolveRegisterSource(RegisterMangoJobWorkerCommand command) {
        if (command.getRegisterSource() != null) {
            return command.getRegisterSource();
        }
        if (command.getTransportType() == JobTransportType.IN_MEMORY) {
            command.setRegisterSource(JobWorkerRegisterSource.EMBEDDED_AUTO);
            return command.getRegisterSource();
        }
        command.setRegisterSource(JobWorkerRegisterSource.REMOTE_AUTO);
        return command.getRegisterSource();
    }

    private boolean canHeartbeatSetOnline(MangoJobWorkerSnapshotEntity worker) {
        if (worker == null || !StringUtils.hasText(worker.getStatus())) {
            return true;
        }
        JobWorkerStatus status;
        try {
            status = JobWorkerStatus.valueOf(worker.getStatus());
        } catch (IllegalArgumentException ex) {
            return false;
        }
        return status == JobWorkerStatus.REGISTERED
                || status == JobWorkerStatus.ONLINE
                || status == JobWorkerStatus.EXPIRED
                || status == JobWorkerStatus.UNKNOWN;
    }

    private Set<JobWorkerStatus> manualStatuses() {
        return EnumSet.of(JobWorkerStatus.ONLINE, JobWorkerStatus.DRAINING,
                JobWorkerStatus.OFFLINE, JobWorkerStatus.DISABLED);
    }

    private String currentTenantId() {
        String tenantId = MangoContextHolder.tenantId();
        Require.notBlank(tenantId, "缺少当前租户上下文");
        return tenantId;
    }
}
