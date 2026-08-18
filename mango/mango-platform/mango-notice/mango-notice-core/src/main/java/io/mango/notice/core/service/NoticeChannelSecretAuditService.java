package io.mango.notice.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import io.mango.common.result.Require;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.notice.api.enums.NoticeCode;
import io.mango.notice.core.entity.NoticeAuditLogEntity;
import io.mango.notice.core.mapper.NoticeAuditLogMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "Spring singleton collaborators are injected and intentionally shared")
public class NoticeChannelSecretAuditService {
    private static final String ACTION_TYPE = "CHANNEL_SECRET_REVEAL";
    private static final String TARGET_TYPE = "NOTICE_CHANNEL_CONFIG";

    private final NoticeAuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void record(Long channelConfigId, String secretKey, String source, String result) {
        NoticeAuditLogEntity entity = new NoticeAuditLogEntity();
        entity.setActionType(ACTION_TYPE);
        entity.setTargetType(TARGET_TYPE);
        entity.setTargetId(channelConfigId);
        entity.setOperatorId(MangoContextHolder.userId());
        entity.setTenantId(
                StringUtils.hasText(MangoContextHolder.tenantId())
                        ? MangoContextHolder.tenantId()
                        : "default");
        entity.setCreatedAt(LocalDateTime.now());
        entity.setAuditSnapshot(snapshot(secretKey, source, result));
        Require.isTrue(
                auditLogMapper.insert(entity) > 0,
                NoticeCode.NOTICE_CHANNEL_SECRET_INVALID,
                "渠道 Secret 查看审计记录失败");
    }

    private String snapshot(String secretKey, String source, String result) {
        Map<String, String> safeSnapshot = new LinkedHashMap<>();
        safeSnapshot.put("secretKey", secretKey);
        safeSnapshot.put("source", source);
        safeSnapshot.put("result", result);
        try {
            return objectMapper.writeValueAsString(safeSnapshot);
        } catch (JsonProcessingException exception) {
            return Require.fail(
                    NoticeCode.NOTICE_CHANNEL_SECRET_INVALID,
                    "渠道 Secret 查看审计摘要生成失败",
                    exception);
        }
    }
}
