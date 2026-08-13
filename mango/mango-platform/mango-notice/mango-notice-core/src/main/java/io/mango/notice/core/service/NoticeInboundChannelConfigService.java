package io.mango.notice.core.service;

import io.mango.common.result.Require;
import io.mango.notice.api.enums.NoticeCode;
import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.core.entity.NoticeChannelConfigEntity;
import io.mango.notice.core.mapper.NoticeChannelConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Resolves a public inbound callback to its persisted tenant-owned channel account. */
@Service
@RequiredArgsConstructor
public class NoticeInboundChannelConfigService implements INoticeInboundChannelConfigService {

    private final NoticeChannelConfigMapper channelConfigMapper;
    private final NoticeChannelSecretMaterializer secretMaterializer;

    public ResolvedInboundChannelConfig resolve(Long channelConfigId, NoticeChannelType expectedType) {
        Require.notNull(channelConfigId, NoticeCode.NOTICE_BUSINESS_ERROR, "接收渠道配置 ID 不能为空");
        NoticeInboundChannelConfigService.ResolvedInboundChannelConfig result;
        NoticeChannelConfigEntity entity = Require.nonNull(
                channelConfigMapper.selectInboundConfigById(channelConfigId), NoticeCode.NOTICE_BUSINESS_ERROR, "接收渠道配置不存在");
        Require.isTrue(Boolean.TRUE.equals(entity.getEnabled()), NoticeCode.NOTICE_BUSINESS_ERROR, "接收渠道配置未启用");
        Require.isTrue(
                NoticeChannelCapabilityPolicy.normalize(entity.getCapabilityMode()).supportsReceive(), NoticeCode.NOTICE_BUSINESS_ERROR,
                "渠道用途不允许接收消息");
        Require.isTrue(entity.getChannelType() == expectedType, NoticeCode.NOTICE_BUSINESS_ERROR, "接收渠道类型不匹配");
        Require.notBlank(entity.getTenantId(), NoticeCode.NOTICE_BUSINESS_ERROR, "接收渠道租户不存在");
        result = new ResolvedInboundChannelConfig(
                entity.getId(), entity.getTenantId(), entity.getConfigCode(), entity.getChannelType(),
                entity.getProviderCode(), secretMaterializer.materialize(entity));
        return result;
    }

    public ResolvedInboundChannelConfig resolve(String configCode, NoticeChannelType expectedType) {
        Require.notBlank(configCode, NoticeCode.NOTICE_BUSINESS_ERROR, "接收渠道配置编码不能为空");
        NoticeChannelConfigEntity entity = Require.nonNull(
                channelConfigMapper.selectInboundConfigByCode(configCode.trim()), NoticeCode.NOTICE_BUSINESS_ERROR, "接收渠道配置不存在");
        Require.isTrue(Boolean.TRUE.equals(entity.getEnabled()), NoticeCode.NOTICE_BUSINESS_ERROR, "接收渠道配置未启用");
        Require.isTrue(
                NoticeChannelCapabilityPolicy.normalize(entity.getCapabilityMode()).supportsReceive(), NoticeCode.NOTICE_BUSINESS_ERROR,
                "渠道用途不允许接收消息");
        Require.isTrue(entity.getChannelType() == expectedType, NoticeCode.NOTICE_BUSINESS_ERROR, "接收渠道类型不匹配");
        Require.notBlank(entity.getTenantId(), NoticeCode.NOTICE_BUSINESS_ERROR, "接收渠道租户不存在");
        return new ResolvedInboundChannelConfig(
                entity.getId(), entity.getTenantId(), entity.getConfigCode(), entity.getChannelType(),
                entity.getProviderCode(), secretMaterializer.materialize(entity));
    }

    public record ResolvedInboundChannelConfig(
            Long id,
            String tenantId,
            String configCode,
            NoticeChannelType channelType,
            String providerCode,
            String materializedConfigJson) {
    }
}
