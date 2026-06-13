package io.mango.payment.core.service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 支付通道原始公网回调。
 *
 * @param channelCode 通道编码。
 * @param method HTTP 方法。
 * @param uri 请求 URI。
 * @param queryString 查询串。
 * @param contentType 内容类型。
 * @param remoteAddr 来源地址。
 * @param params 表单或查询参数。
 * @param rawBody 原始请求体。
 * @param receivedAt 接收时间。
 */
public record PaymentChannelRawCallback(
        String channelCode,
        String method,
        String uri,
        String queryString,
        String contentType,
        String remoteAddr,
        Map<String, String> params,
        String rawBody,
        LocalDateTime receivedAt) {
}
