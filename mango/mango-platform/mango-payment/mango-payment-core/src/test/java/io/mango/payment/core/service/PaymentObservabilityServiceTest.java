package io.mango.payment.core.service;

import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import io.mango.payment.api.vo.PaymentChannelCertificateExpiryVO;
import io.mango.payment.api.vo.PaymentObservabilitySnapshotVO;
import io.mango.payment.core.mapper.PaymentChannelContractCapabilityMapper;
import io.mango.payment.core.mapper.PaymentDifferenceMapper;
import io.mango.payment.core.mapper.PaymentExceptionOrderMapper;
import io.mango.payment.core.mapper.PaymentNotificationRecordMapper;
import io.mango.payment.core.mapper.PaymentOrderMapper;
import io.mango.payment.core.mapper.PaymentRefundOrderMapper;
import io.mango.payment.core.model.PaymentChannelFailureMetric;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentObservabilityServiceTest {

    private PaymentOrderMapper paymentOrderMapper;
    private PaymentRefundOrderMapper refundOrderMapper;
    private PaymentNotificationRecordMapper notificationRecordMapper;
    private PaymentDifferenceMapper differenceMapper;
    private PaymentExceptionOrderMapper exceptionOrderMapper;
    private PaymentChannelContractCapabilityMapper contractCapabilityMapper;
    private PaymentObservabilityService service;

    @BeforeEach
    void setUp() {
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1001L, "1", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L, "internal-admin"));
        paymentOrderMapper = mock(PaymentOrderMapper.class);
        refundOrderMapper = mock(PaymentRefundOrderMapper.class);
        notificationRecordMapper = mock(PaymentNotificationRecordMapper.class);
        differenceMapper = mock(PaymentDifferenceMapper.class);
        exceptionOrderMapper = mock(PaymentExceptionOrderMapper.class);
        contractCapabilityMapper = mock(PaymentChannelContractCapabilityMapper.class);
        PaymentObservabilityProperties properties = new PaymentObservabilityProperties();
        properties.setPaymentBacklogThreshold(2L);
        properties.setCallbackFailureThreshold(1L);
        properties.setNotificationFailureThreshold(1L);
        properties.setRefundFailureThreshold(1L);
        properties.setDifferenceThreshold(1L);
        properties.setUnhandledExceptionThreshold(1L);
        properties.setExpiringCertificateThreshold(1L);
        properties.setChannelFailureRateThreshold(new BigDecimal("0.2000"));
        service = new PaymentObservabilityService(
                paymentOrderMapper,
                refundOrderMapper,
                notificationRecordMapper,
                differenceMapper,
                exceptionOrderMapper,
                contractCapabilityMapper,
                properties);
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    @DisplayName("currentSnapshot should calculate minimum metrics from real mapper counts")
    void currentSnapshot_calculatesMinimumMetricsFromMapperCounts() {
        when(paymentOrderMapper.countPaymentOrdersByStatus(1L, null)).thenReturn(10L);
        when(paymentOrderMapper.countPaymentOrdersByStatus(1L, "SUCCESS")).thenReturn(8L);
        when(paymentOrderMapper.countPaymentOrdersByStatus(1L, "FAILED")).thenReturn(2L);
        when(paymentOrderMapper.countProcessingPaymentBacklog(1L)).thenReturn(3L);
        when(paymentOrderMapper.selectChannelFailureMetrics(1L)).thenReturn(List.of(channelMetric("MANGO_PAY", 10L, 3L)));
        when(refundOrderMapper.countRefundOrdersByStatus(1L, null)).thenReturn(4L);
        when(refundOrderMapper.countRefundOrdersByStatus(1L, "SUCCESS")).thenReturn(3L);
        when(refundOrderMapper.countRefundOrdersByStatus(1L, "FAILED")).thenReturn(1L);
        when(exceptionOrderMapper.countCallbackFailureExceptionOrders(1L)).thenReturn(1L);
        when(notificationRecordMapper.countFailedNotificationRecords(1L)).thenReturn(2L);
        when(differenceMapper.countDifferences(1L, null, null)).thenReturn(1L);
        when(exceptionOrderMapper.countExceptionOrders(1L, null, "PENDING")).thenReturn(1L);
        when(exceptionOrderMapper.countExceptionOrders(1L, null, "PROCESSING")).thenReturn(1L);
        when(contractCapabilityMapper.selectExpiringCertificates(eq(1L), any(), any())).thenReturn(List.of(expiringCertificate()));

        PaymentObservabilitySnapshotVO snapshot = service.currentSnapshot();

        assertThat(snapshot.getPaymentSuccessRate()).isEqualByComparingTo("0.8000");
        assertThat(snapshot.getChannelFailureRate()).isEqualByComparingTo("0.3000");
        assertThat(snapshot.getRefundSuccessRate()).isEqualByComparingTo("0.7500");
        assertThat(snapshot.getPaymentBacklogCount()).isEqualTo(3L);
        assertThat(snapshot.getCallbackFailureCount()).isEqualTo(1L);
        assertThat(snapshot.getNotificationFailureCount()).isEqualTo(2L);
        assertThat(snapshot.getDifferenceCount()).isEqualTo(1L);
        assertThat(snapshot.getUnhandledExceptionCount()).isEqualTo(2L);
        assertThat(snapshot.getExpiringCertificateCount()).isEqualTo(1L);
        assertThat(snapshot.getAlerts())
                .extracting("alertType")
                .contains(
                        "PAYMENT_SUCCESS_RATE",
                        "REFUND_SUCCESS_RATE",
                        "ORDER_BACKLOG",
                        "CALLBACK_FAILURE",
                        "NOTIFICATION_FAILURE",
                        "REFUND_FAILURE",
                        "RECONCILIATION_DIFFERENCE",
                        "UNHANDLED_EXCEPTION",
                        "CERTIFICATE_EXPIRING",
                        "CHANNEL_FAILURE_RATE");
    }

    @Test
    @DisplayName("currentSnapshot should not alert success rate when there is no denominator")
    void currentSnapshot_noDenominator_hasFullRateAndNoSuccessRateAlert() {
        when(paymentOrderMapper.countPaymentOrdersByStatus(1L, null)).thenReturn(0L);
        when(paymentOrderMapper.countPaymentOrdersByStatus(1L, "SUCCESS")).thenReturn(0L);
        when(paymentOrderMapper.countPaymentOrdersByStatus(1L, "FAILED")).thenReturn(0L);
        when(refundOrderMapper.countRefundOrdersByStatus(1L, null)).thenReturn(0L);
        when(refundOrderMapper.countRefundOrdersByStatus(1L, "SUCCESS")).thenReturn(0L);
        when(refundOrderMapper.countRefundOrdersByStatus(1L, "FAILED")).thenReturn(0L);
        when(paymentOrderMapper.selectChannelFailureMetrics(1L)).thenReturn(List.of());
        when(contractCapabilityMapper.selectExpiringCertificates(eq(1L), any(), any())).thenReturn(List.of());

        PaymentObservabilitySnapshotVO snapshot = service.currentSnapshot();

        assertThat(snapshot.getPaymentSuccessRate()).isEqualByComparingTo("1.0000");
        assertThat(snapshot.getRefundSuccessRate()).isEqualByComparingTo("1.0000");
        assertThat(snapshot.getAlerts()).isEmpty();
    }

    private PaymentChannelFailureMetric channelMetric(String channelCode, Long totalCount, Long failedCount) {
        PaymentChannelFailureMetric metric = new PaymentChannelFailureMetric();
        metric.setChannelCode(channelCode);
        metric.setTotalCount(totalCount);
        metric.setFailedCount(failedCount);
        return metric;
    }

    private PaymentChannelCertificateExpiryVO expiringCertificate() {
        PaymentChannelCertificateExpiryVO vo = new PaymentChannelCertificateExpiryVO();
        vo.setChannelCode("MANGO_PAY");
        return vo;
    }
}
