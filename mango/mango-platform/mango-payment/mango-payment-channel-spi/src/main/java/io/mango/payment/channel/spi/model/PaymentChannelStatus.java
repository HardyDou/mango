package io.mango.payment.channel.spi.model;

import io.mango.payment.api.enums.PaymentOrderStatus;

public record PaymentChannelStatus(
        PaymentOrderStatus status,
        String channelOrderNo
) {
}
