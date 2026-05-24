package io.mango.payment.channel.spi.model;

import io.mango.payment.api.enums.RefundOrderStatus;

public record RefundChannelStatus(
        RefundOrderStatus status,
        String channelRefundNo
) {
}
