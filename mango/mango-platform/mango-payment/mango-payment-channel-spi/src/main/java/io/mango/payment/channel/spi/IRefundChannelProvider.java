package io.mango.payment.channel.spi;

import io.mango.payment.channel.spi.model.RefundChannelStatus;

public interface IRefundChannelProvider {

    String channelCode();

    RefundChannelStatus refund(Long refundOrderId, Long paymentOrderId, Long refundAmount);

    RefundChannelStatus queryRefund(Long refundOrderId, String channelRefundNo);
}
