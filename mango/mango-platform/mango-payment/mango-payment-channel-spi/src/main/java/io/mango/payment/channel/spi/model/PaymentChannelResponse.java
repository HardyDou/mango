package io.mango.payment.channel.spi.model;

import io.mango.payment.api.enums.PaymentMaterialType;

public record PaymentChannelResponse(
        String channelCode,
        String channelOrderNo,
        PaymentMaterialType materialType,
        String materialContent
) {
}
