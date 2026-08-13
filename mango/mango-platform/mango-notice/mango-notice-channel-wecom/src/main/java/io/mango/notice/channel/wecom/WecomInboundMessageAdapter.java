package io.mango.notice.channel.wecom;

import io.mango.notice.api.InboundNoticeMessageRequest;

public interface WecomInboundMessageAdapter {

    String verifyUrl(WecomInboundRequest request, WecomInboundConfig config);

    InboundNoticeMessageRequest parseMessage(WecomInboundRequest request, WecomInboundConfig config,
                                      String tenantId, Long channelConfigId);
}
