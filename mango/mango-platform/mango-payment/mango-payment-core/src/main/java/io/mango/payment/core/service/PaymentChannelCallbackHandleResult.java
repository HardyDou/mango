package io.mango.payment.core.service;

import io.mango.common.result.Require;
import io.mango.payment.api.PaymentCode;

/**
 * 支付通道公网回调处理结果。
 *
 * @param responseBody 通道 ACK 响应体。
 * @param contentType 响应内容类型。
 */
public record PaymentChannelCallbackHandleResult(String responseBody, String contentType) {

    private static final String DEFAULT_CONTENT_TYPE = "text/plain;charset=UTF-8";

    public static PaymentChannelCallbackHandleResult text(String responseBody) {
        Require.notNull(responseBody, PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "通道回调响应不能为空");
        return new PaymentChannelCallbackHandleResult(responseBody, DEFAULT_CONTENT_TYPE);
    }
}
