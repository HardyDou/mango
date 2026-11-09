package io.mango.integration.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.common.result.Require;
import io.mango.integration.api.command.IntegrationMaintenanceCleanupCommand;
import io.mango.integration.api.enums.IntegrationCode;
import io.mango.integration.core.entity.IntegrationCallLogEntity;
import io.mango.integration.core.entity.IntegrationCredentialAccessLogEntity;
import io.mango.integration.core.entity.IntegrationCredentialRotationEntity;
import io.mango.integration.core.mapper.IntegrationCallLogMapper;
import io.mango.integration.core.mapper.IntegrationCredentialAccessLogMapper;
import io.mango.integration.core.mapper.IntegrationCredentialRotationMapper;
import io.mango.integration.core.service.IIntegrationMaintenanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 集成模块维护服务实现。
 */
@Service
@RequiredArgsConstructor
public class IntegrationMaintenanceService implements IIntegrationMaintenanceService {

    private static final int MIN_RETENTION_DAYS = 1;
    private static final int MAX_RETENTION_DAYS = 3650;

    private final IntegrationCallLogMapper callLogMapper;
    private final IntegrationCredentialAccessLogMapper accessLogMapper;
    private final IntegrationCredentialRotationMapper rotationMapper;

    @Override
    public Integer cleanup(IntegrationMaintenanceCleanupCommand command) {
        IntegrationMaintenanceCleanupCommand validated = Require.nonNull(
                command, IntegrationCode.INTEGRATION_BUSINESS_ERROR, "清理命令不能为空");
        Integer retentionDays = validated.getRetentionDays();
        Require.isTrue(retentionDays != null
                        && retentionDays >= MIN_RETENTION_DAYS && retentionDays <= MAX_RETENTION_DAYS,
                IntegrationCode.INTEGRATION_BUSINESS_ERROR, "保留天数必须在1到3650之间");
        LocalDateTime cutoff = LocalDateTime.now().minusDays(Objects.requireNonNull(retentionDays));
        String resource = validated.getResource() == null ? "" : validated.getResource().trim().toUpperCase();
        int deleted = switch (resource) {
            case "CALL_LOG" -> callLogMapper.delete(
                    new LambdaQueryWrapper<IntegrationCallLogEntity>()
                            .lt(IntegrationCallLogEntity::getCreatedAt, cutoff));
            case "ACCESS_LOG" -> accessLogMapper.delete(
                    new LambdaQueryWrapper<IntegrationCredentialAccessLogEntity>()
                            .lt(IntegrationCredentialAccessLogEntity::getCreatedAt, cutoff));
            case "ROTATION" -> rotationMapper.delete(
                    new LambdaQueryWrapper<IntegrationCredentialRotationEntity>()
                            .lt(IntegrationCredentialRotationEntity::getCreatedAt, cutoff));
            default -> 0;
        };
        Require.isTrue(deleted >= 0, IntegrationCode.INTEGRATION_BUSINESS_ERROR, "清理对象不支持");
        return deleted;
    }
}
