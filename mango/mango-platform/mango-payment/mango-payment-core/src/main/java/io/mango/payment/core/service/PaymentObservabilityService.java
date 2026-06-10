package io.mango.payment.core.service;

import io.mango.common.result.Require;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.enums.PaymentOrderStatusEnum;
import io.mango.payment.api.enums.PaymentRefundOrderStatusEnum;
import io.mango.payment.api.vo.PaymentObservabilityAlertVO;
import io.mango.payment.api.vo.PaymentObservabilitySnapshotVO;
import io.mango.payment.core.mapper.PaymentChannelContractCapabilityMapper;
import io.mango.payment.core.mapper.PaymentDifferenceMapper;
import io.mango.payment.core.mapper.PaymentExceptionOrderMapper;
import io.mango.payment.core.mapper.PaymentNotificationRecordMapper;
import io.mango.payment.core.mapper.PaymentOrderMapper;
import io.mango.payment.core.mapper.PaymentRefundOrderMapper;
import io.mango.payment.core.model.PaymentChannelFailureMetric;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentObservabilityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentObservabilityService.class);
    private static final BigDecimal ONE = BigDecimal.ONE;

    private final PaymentOrderMapper paymentOrderMapper;
    private final PaymentRefundOrderMapper refundOrderMapper;
    private final PaymentNotificationRecordMapper notificationRecordMapper;
    private final PaymentDifferenceMapper differenceMapper;
    private final PaymentExceptionOrderMapper exceptionOrderMapper;
    private final PaymentChannelContractCapabilityMapper contractCapabilityMapper;
    private final PaymentObservabilityProperties properties;

    public PaymentObservabilitySnapshotVO currentSnapshot() {
        Long tenantId = PaymentContextSupport.currentTenantId();
        PaymentObservabilitySnapshotVO snapshot = new PaymentObservabilitySnapshotVO();
        long paymentTotal = paymentOrderMapper.countPaymentOrdersByStatus(tenantId, null);
        long paymentSuccess = paymentOrderMapper.countPaymentOrdersByStatus(tenantId, PaymentOrderStatusEnum.SUCCESS.getCode());
        long paymentFailed = paymentOrderMapper.countPaymentOrdersByStatus(tenantId, PaymentOrderStatusEnum.FAILED.getCode());
        long paymentBacklog = paymentOrderMapper.countProcessingPaymentBacklog(tenantId);
        long refundTotal = refundOrderMapper.countRefundOrdersByStatus(tenantId, null);
        long refundSuccess = refundOrderMapper.countRefundOrdersByStatus(tenantId, PaymentRefundOrderStatusEnum.SUCCESS.getCode());
        long refundFailure = refundOrderMapper.countRefundOrdersByStatus(tenantId, PaymentRefundOrderStatusEnum.FAILED.getCode());
        long callbackFailure = exceptionOrderMapper.countCallbackFailureExceptionOrders(tenantId);
        long notificationFailure = notificationRecordMapper.countFailedNotificationRecords(tenantId);
        long differenceCount = differenceMapper.countDifferences(tenantId, null, null);
        long unhandledExceptionCount = exceptionOrderMapper.countExceptionOrders(tenantId, null, "PENDING")
                + exceptionOrderMapper.countExceptionOrders(tenantId, null, "PROCESSING");
        long expiringCertificateCount = contractCapabilityMapper
                .selectExpiringCertificates(tenantId, certificateDeadline(), LocalDateTime.now())
                .size();

        snapshot.setPaymentTotalCount(paymentTotal);
        snapshot.setPaymentSuccessCount(paymentSuccess);
        snapshot.setPaymentFailedCount(paymentFailed);
        snapshot.setPaymentBacklogCount(paymentBacklog);
        snapshot.setPaymentSuccessRate(rate(paymentSuccess, paymentTotal));
        snapshot.setChannelFailureRate(channelFailureRate(tenantId));
        snapshot.setCallbackFailureCount(callbackFailure);
        snapshot.setNotificationFailureCount(notificationFailure);
        snapshot.setRefundTotalCount(refundTotal);
        snapshot.setRefundSuccessCount(refundSuccess);
        snapshot.setRefundFailureCount(refundFailure);
        snapshot.setRefundSuccessRate(rate(refundSuccess, refundTotal));
        snapshot.setDifferenceCount(differenceCount);
        snapshot.setUnhandledExceptionCount(unhandledExceptionCount);
        snapshot.setExpiringCertificateCount(expiringCertificateCount);
        snapshot.setAlerts(alerts(snapshot, tenantId));
        return snapshot;
    }

    public void logSummary(
            String event,
            String orderNo,
            String status,
            Long amount,
            String channelCode,
            long durationMillis,
            String result) {
        Require.notBlank(event, PaymentCode.PAYMENT_OBSERVABILITY_INVALID.getCode(), "摘要日志事件不能为空");
        LOGGER.info(
                "payment.summary event={} orderNo={} status={} amount={} channel={} durationMs={} result={}",
                event,
                safe(orderNo),
                safe(status),
                amount,
                safe(channelCode),
                durationMillis,
                safe(result));
    }

    private List<PaymentObservabilityAlertVO> alerts(PaymentObservabilitySnapshotVO snapshot, Long tenantId) {
        List<PaymentObservabilityAlertVO> alerts = new ArrayList<>();
        if (positive(snapshot.getPaymentTotalCount())
                && snapshot.getPaymentSuccessRate().compareTo(properties.getPaymentSuccessRateMinimum()) < 0) {
            alerts.add(alert("PAYMENT_SUCCESS_RATE", "HIGH", "PAYMENT",
                    snapshot.getPaymentSuccessRate().toPlainString(),
                    properties.getPaymentSuccessRateMinimum().toPlainString(),
                    "支付成功率低于最小阈值"));
        }
        if (positive(snapshot.getRefundTotalCount())
                && snapshot.getRefundSuccessRate().compareTo(properties.getRefundSuccessRateMinimum()) < 0) {
            alerts.add(alert("REFUND_SUCCESS_RATE", "HIGH", "REFUND",
                    snapshot.getRefundSuccessRate().toPlainString(),
                    properties.getRefundSuccessRateMinimum().toPlainString(),
                    "退款成功率低于最小阈值"));
        }
        if (snapshot.getPaymentBacklogCount() >= properties.getPaymentBacklogThreshold()) {
            alerts.add(alert("ORDER_BACKLOG", "HIGH", "PAYING",
                    String.valueOf(snapshot.getPaymentBacklogCount()),
                    String.valueOf(properties.getPaymentBacklogThreshold()),
                    "支付中订单积压达到阈值"));
        }
        if (snapshot.getCallbackFailureCount() >= properties.getCallbackFailureThreshold()) {
            alerts.add(alert("CALLBACK_FAILURE", "HIGH", "CHANNEL_CALLBACK",
                    String.valueOf(snapshot.getCallbackFailureCount()),
                    String.valueOf(properties.getCallbackFailureThreshold()),
                    "通道回调失败异常达到阈值"));
        }
        if (snapshot.getNotificationFailureCount() >= properties.getNotificationFailureThreshold()) {
            alerts.add(alert("NOTIFICATION_FAILURE", "MEDIUM", "BUSINESS_NOTIFY",
                    String.valueOf(snapshot.getNotificationFailureCount()),
                    String.valueOf(properties.getNotificationFailureThreshold()),
                    "业务通知失败达到阈值"));
        }
        if (snapshot.getRefundFailureCount() >= properties.getRefundFailureThreshold()) {
            alerts.add(alert("REFUND_FAILURE", "HIGH", "REFUND",
                    String.valueOf(snapshot.getRefundFailureCount()),
                    String.valueOf(properties.getRefundFailureThreshold()),
                    "退款失败数达到阈值"));
        }
        if (snapshot.getDifferenceCount() >= properties.getDifferenceThreshold()) {
            alerts.add(alert("RECONCILIATION_DIFFERENCE", "HIGH", "RECONCILIATION",
                    String.valueOf(snapshot.getDifferenceCount()),
                    String.valueOf(properties.getDifferenceThreshold()),
                    "对账差异数达到阈值"));
        }
        if (snapshot.getUnhandledExceptionCount() >= properties.getUnhandledExceptionThreshold()) {
            alerts.add(alert("UNHANDLED_EXCEPTION", "HIGH", "EXCEPTION_ORDER",
                    String.valueOf(snapshot.getUnhandledExceptionCount()),
                    String.valueOf(properties.getUnhandledExceptionThreshold()),
                    "未处理异常订单数达到阈值"));
        }
        if (snapshot.getExpiringCertificateCount() >= properties.getExpiringCertificateThreshold()) {
            alerts.add(alert("CERTIFICATE_EXPIRING", "HIGH", "CHANNEL_CONTRACT",
                    String.valueOf(snapshot.getExpiringCertificateCount()),
                    String.valueOf(properties.getExpiringCertificateThreshold()),
                    "通道证书即将过期数达到阈值"));
        }
        alerts.addAll(channelFailureAlerts(tenantId));
        return alerts;
    }

    private List<PaymentObservabilityAlertVO> channelFailureAlerts(Long tenantId) {
        List<PaymentObservabilityAlertVO> alerts = new ArrayList<>();
        List<PaymentChannelFailureMetric> metrics = paymentOrderMapper.selectChannelFailureMetrics(tenantId);
        for (PaymentChannelFailureMetric metric : metrics) {
            BigDecimal failureRate = rate(metric.getFailedCount(), metric.getTotalCount());
            if (positive(metric.getTotalCount())
                    && failureRate.compareTo(properties.getChannelFailureRateThreshold()) >= 0) {
                alerts.add(alert("CHANNEL_FAILURE_RATE", "HIGH", safe(metric.getChannelCode()),
                        failureRate.toPlainString(),
                        properties.getChannelFailureRateThreshold().toPlainString(),
                        "通道失败率达到阈值"));
            }
        }
        return alerts;
    }

    private BigDecimal channelFailureRate(Long tenantId) {
        long total = 0L;
        long failed = 0L;
        for (PaymentChannelFailureMetric metric : paymentOrderMapper.selectChannelFailureMetrics(tenantId)) {
            total += value(metric.getTotalCount());
            failed += value(metric.getFailedCount());
        }
        return rate(failed, total);
    }

    private LocalDateTime certificateDeadline() {
        return LocalDateTime.now().plusDays(properties.getCertificateWarningDays());
    }

    private PaymentObservabilityAlertVO alert(
            String alertType,
            String severity,
            String target,
            String currentValue,
            String threshold,
            String message) {
        PaymentObservabilityAlertVO vo = new PaymentObservabilityAlertVO();
        vo.setAlertType(alertType);
        vo.setSeverity(severity);
        vo.setTarget(target);
        vo.setCurrentValue(currentValue);
        vo.setThreshold(threshold);
        vo.setMessage(message);
        return vo;
    }

    private BigDecimal rate(Long numerator, Long denominator) {
        long total = value(denominator);
        if (total <= 0) {
            return ONE;
        }
        return BigDecimal.valueOf(value(numerator))
                .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }

    private boolean positive(Long value) {
        return value(value) > 0L;
    }

    private String safe(String value) {
        String normalized = PaymentContextSupport.trimToNull(value);
        return normalized == null ? "-" : normalized;
    }
}
