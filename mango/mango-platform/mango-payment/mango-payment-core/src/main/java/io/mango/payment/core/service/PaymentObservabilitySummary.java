package io.mango.payment.core.service;

public record PaymentObservabilitySummary(
        String event,
        String orderNo,
        String status,
        Long amount,
        String channelCode,
        long durationMillis,
        String result) {
}
