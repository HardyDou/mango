package io.mango.payment.core.model;

import lombok.Data;

@Data
public class PaymentChannelFailureMetric {

    private String channelCode;

    private Long totalCount;

    private Long failedCount;
}
