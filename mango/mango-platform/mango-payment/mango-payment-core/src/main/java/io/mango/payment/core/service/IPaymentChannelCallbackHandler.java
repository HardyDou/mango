package io.mango.payment.core.service;

/**
 * 支付通道公网回调处理器。
 */
public interface IPaymentChannelCallbackHandler {

    /**
     * 通道编码。
     *
     * @return 通道编码。
     */
    String channelCode();

    /**
     * 处理通道原始回调。
     *
     * @param callback 原始回调。
     * @return 通道 ACK 响应。
     */
    PaymentChannelCallbackHandleResult handle(PaymentChannelRawCallback callback);
}
