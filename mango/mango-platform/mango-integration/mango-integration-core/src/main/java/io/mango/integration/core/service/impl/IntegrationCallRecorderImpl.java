package io.mango.integration.core.service.impl;

import io.mango.infra.context.api.MangoContextHolder;
import io.mango.integration.core.entity.IntegrationCallLogEntity;
import io.mango.integration.core.mapper.IntegrationCallLogMapper;
import io.mango.integration.support.client.IntegrationCallRecord;
import io.mango.integration.support.client.IntegrationCallRecorder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 集成调用记录写入实现。
 */
@Service
@RequiredArgsConstructor
public class IntegrationCallRecorderImpl implements IntegrationCallRecorder {

    private static final int MAX_MESSAGE_LENGTH = 500;

    private final IntegrationCallLogMapper callLogMapper;

    @Override
    public void record(IntegrationCallRecord record) {
        if (record == null || !StringUtils.hasText(record.getInstanceCode())) {
            return;
        }
        IntegrationCallLogEntity entity = new IntegrationCallLogEntity();
        String tenantId = MangoContextHolder.tenantId();
        entity.setTenantId(StringUtils.hasText(tenantId) ? tenantId : "default");
        entity.setInstanceCode(record.getInstanceCode());
        entity.setConnectorCode(record.getConnectorCode());
        entity.setHttpMethod(record.getMethod());
        entity.setPath(record.getPath());
        entity.setStatusCode(record.getStatusCode());
        entity.setElapsedMs(record.getElapsedMs());
        entity.setSuccess(record.isSuccess() ? 1 : 0);
        entity.setMessage(truncate(record.getMessage()));
        callLogMapper.insert(entity);
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= MAX_MESSAGE_LENGTH ? message : message.substring(0, MAX_MESSAGE_LENGTH);
    }
}
