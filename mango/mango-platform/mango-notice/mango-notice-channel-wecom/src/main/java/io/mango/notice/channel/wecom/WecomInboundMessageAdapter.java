package io.mango.notice.channel.wecom;

import io.mango.notice.api.InboundNoticeMessage;

public interface WecomInboundMessageAdapter {

    String verifyUrl(WecomInboundRequest request, WecomInboundConfig config);

    InboundNoticeMessage parseMessage(WecomInboundRequest request, WecomInboundConfig config,
                                      String tenantId, Long channelConfigId);
}
