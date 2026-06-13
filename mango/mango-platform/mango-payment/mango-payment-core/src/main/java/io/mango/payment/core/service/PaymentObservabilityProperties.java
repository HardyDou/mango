package io.mango.payment.core.service;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentObservabilityProperties {

    private BigDecimal channelFailureRateThreshold = new BigDecimal("0.1000");

    private BigDecimal paymentSuccessRateMinimum = new BigDecimal("0.9500");

    private BigDecimal refundSuccessRateMinimum = new BigDecimal("0.9500");

    private long paymentBacklogThreshold = 100L;

    private long callbackFailureThreshold = 1L;

    private long notificationFailureThreshold = 1L;

    private long refundFailureThreshold = 1L;

    private long differenceThreshold = 1L;

    private long unhandledExceptionThreshold = 1L;

    private long expiringCertificateThreshold = 1L;

    private int certificateWarningDays = 30;
}
