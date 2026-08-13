package io.mango.notice.api;

import java.util.Map;

/** SPI for providers that push real inbound mail, not sending receipts. */
public interface NoticeInboundWebhookProvider {

    String providerCode();

    boolean supportsInboundReception();

    boolean supports(Map<String, String> headers, Map<String, String> parameters, String body);

    InboundNoticeMessage parse(Map<String, String> headers, Map<String, String> parameters, String body,
                               String materializedConfigJson, String tenantId, Long channelConfigId);
}
