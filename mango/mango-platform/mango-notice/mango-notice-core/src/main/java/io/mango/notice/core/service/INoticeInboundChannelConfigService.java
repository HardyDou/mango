package io.mango.notice.core.service;

import io.mango.notice.api.enums.NoticeChannelType;

/** Resolves tenant-owned inbound channel configuration. */
public interface INoticeInboundChannelConfigService {

    NoticeInboundChannelConfigService.ResolvedInboundChannelConfig resolve(
            Long channelConfigId, NoticeChannelType expectedType);

    NoticeInboundChannelConfigService.ResolvedInboundChannelConfig resolve(
            String configCode, NoticeChannelType expectedType);
}
