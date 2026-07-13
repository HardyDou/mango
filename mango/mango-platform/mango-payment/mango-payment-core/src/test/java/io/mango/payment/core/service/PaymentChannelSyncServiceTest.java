package io.mango.payment.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.exception.BizException;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.payment.api.enums.PaymentCode;
import io.mango.payment.api.vo.PaymentOrderVO;
import io.mango.payment.core.model.projection.PaymentOrderProjection;
import io.mango.payment.core.entity.PaymentApplicationEntity;
import io.mango.payment.core.entity.PaymentBusinessOrderEntity;
import io.mango.payment.core.entity.PaymentChannelQueryRecordEntity;
import io.mango.payment.core.entity.PaymentOrderEntity;
import io.mango.payment.core.entity.PaymentTransactionFlowEntity;
import io.mango.payment.core.mapper.PaymentApplicationMapper;
import io.mango.payment.core.mapper.PaymentBusinessOrderMapper;
import io.mango.payment.core.mapper.PaymentChannelContractMapper;
import io.mango.payment.core.mapper.PaymentChannelQueryRecordMapper;
import io.mango.payment.core.mapper.PaymentOrderMapper;
import io.mango.payment.core.mapper.PaymentRefundOrderMapper;
import io.mango.payment.core.mapper.PaymentRefundQueryRecordMapper;
import io.mango.payment.core.mapper.PaymentTransactionFlowMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentChannelSynchronizerTest {

    private PaymentOrderMapper paymentOrderMapper;
    private PaymentRefundOrderMapper refundOrderMapper;
    private PaymentBusinessOrderMapper businessOrderMapper;
    private PaymentApplicationMapper applicationMapper;
    private PaymentTransactionFlowMapper transactionFlowMapper;
    private PaymentChannelContractMapper channelContractMapper;
    private PaymentChannelQueryRecordMapper channelQueryRecordMapper;
    private PaymentRefundQueryRecordMapper refundQueryRecordMapper;
    private PaymentNotificationDispatcher notificationService;
    private PaymentMangoPayScenarioControlService scenarioControlService;
    private PaymentChannelAdapterRegistry channelAdapterRegistry;
    private PaymentOrderStatusFlowRecorder statusFlowService;
    private PaymentDuplicatePaymentGuard duplicatePaymentService;
    private PaymentRefundCompletionDeduplicator duplicateRefundCompletionService;
    private PaymentObservabilityService observabilityService;
    private PaymentExceptionOrderRecorder exceptionOrderRecordService;
    private PaymentNumberGenerator numberService;
    private PaymentChannelSynchronizer service;

    @BeforeEach
    void setUp() {
        paymentOrderMapper = mock(PaymentOrderMapper.class);
        refundOrderMapper = mock(PaymentRefundOrderMapper.class);
        businessOrderMapper = mock(PaymentBusinessOrderMapper.class);
        applicationMapper = mock(PaymentApplicationMapper.class);
        transactionFlowMapper = mock(PaymentTransactionFlowMapper.class);
        channelContractMapper = mock(PaymentChannelContractMapper.class);
        channelQueryRecordMapper = mock(PaymentChannelQueryRecordMapper.class);
        refundQueryRecordMapper = mock(PaymentRefundQueryRecordMapper.class);
        notificationService = mock(PaymentNotificationDispatcher.class);
        scenarioControlService = mock(PaymentMangoPayScenarioControlService.class);
        statusFlowService = mock(PaymentOrderStatusFlowRecorder.class);
        duplicatePaymentService = mock(PaymentDuplicatePaymentGuard.class);
        duplicateRefundCompletionService = mock(PaymentRefundCompletionDeduplicator.class);
        observabilityService = mock(PaymentObservabilityService.class);
        exceptionOrderRecordService = mock(PaymentExceptionOrderRecorder.class);
        numberService = mock(PaymentNumberGenerator.class);
        when(numberService.next(PaymentNumberGenerator.PAY_FLOW_NO)).thenReturn("PF2026060600000001");
        when(numberService.next(PaymentNumberGenerator.PAY_QUERY_NO)).thenReturn("PQ2026060600000001");
        channelAdapterRegistry = new PaymentChannelAdapterRegistry(java.util.List.of(new PaymentMangoPayChannelAdapter(
                channelContractMapper,
                mock(io.mango.payment.core.mapper.PaymentReconciliationMapper.class),
                scenarioControlService,
                new PaymentMangoPayResultTranslator())));
        service = new PaymentChannelSynchronizer(
                paymentOrderMapper,
                refundOrderMapper,
                businessOrderMapper,
                applicationMapper,
                transactionFlowMapper,
                channelQueryRecordMapper,
                refundQueryRecordMapper,
                new PaymentOrderStatePolicy(),
                notificationService,
                statusFlowService,
                duplicatePaymentService,
                duplicateRefundCompletionService,
                channelAdapterRegistry,
                new ObjectMapper(),
                observabilityService,
                exceptionOrderRecordService,
                numberService,
                new TestTransactionManager());
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1001L, "1", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L, "internal-admin"));
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    @DisplayName("syncPaymentStatus should advance PAYING order to SUCCESS and create payment flow")
    void syncPaymentStatus_success_advancesOrderAndCreatesFlow() {
        PaymentOrderEntity order = paymentOrder("PAYING");
        when(paymentOrderMapper.selectByTenantAndPayOrderNo("1", "PO202606060001")).thenReturn(order);
        when(channelContractMapper.selectActiveConfigValuesJson("1", 331001L)).thenReturn("{\"mangoPayScenario\":\"SUCCESS\"}");
        when(paymentOrderMapper.updatePayingQueryResult(eq("1"), eq(370001L), eq("SUCCESS"), eq(1), any(LocalDateTime.class)))
                .thenReturn(1);
        when(businessOrderMapper.markCashierPaySuccess("1", 360001L, 9900L)).thenReturn(1);
        when(businessOrderMapper.selectCashierBusinessOrder("1", 360001L)).thenReturn(businessOrder());
        when(applicationMapper.selectOne(any())).thenReturn(application());
        when(paymentOrderMapper.selectPaymentOrderById("1", 370001L)).thenReturn(paymentOrderVO("SUCCESS"));
        when(channelQueryRecordMapper.countByTenantAndPayOrderNo("1", "PO202606060001")).thenReturn(3L);
        when(channelQueryRecordMapper.selectLastByTenantAndPayOrderNo("1", "PO202606060001"))
                .thenReturn(queryRecord("UPDATED"));
        ArgumentCaptor<PaymentTransactionFlowEntity> flowCaptor = ArgumentCaptor.forClass(PaymentTransactionFlowEntity.class);
        ArgumentCaptor<PaymentChannelQueryRecordEntity> queryRecordCaptor = ArgumentCaptor.forClass(PaymentChannelQueryRecordEntity.class);

        PaymentChannelSynchronizer.PaymentSyncResult result = service.syncPaymentStatus("PO202606060001");

        assertThat(result.payOrderNo()).isEqualTo("PO202606060001");
        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.changed()).isTrue();
        assertThat(result.flowNo()).startsWith("PF");
        assertThat(result.queryCount()).isEqualTo(3L);
        assertThat(result.lastQueryResult()).isEqualTo("UPDATED");
        verify(transactionFlowMapper).insert(flowCaptor.capture());
        PaymentTransactionFlowEntity flow = flowCaptor.getValue();
        assertThat(flow.getTenantId()).isEqualTo("1");
        assertThat(flow.getBusinessOrderId()).isEqualTo(360001L);
        assertThat(flow.getPaymentOrderId()).isEqualTo(370001L);
        assertThat(flow.getFlowType()).isEqualTo("PAY_SUCCESS");
        assertThat(flow.getAmount()).isEqualTo(9900L);
        verify(businessOrderMapper).markCashierPaySuccess("1", 360001L, 9900L);
        assertPaymentNotification("SUCCESS");
        verify(channelQueryRecordMapper).insert(queryRecordCaptor.capture());
        PaymentChannelQueryRecordEntity record = queryRecordCaptor.getValue();
        assertThat(record.getPayOrderNo()).isEqualTo("PO202606060001");
        assertThat(record.getPaymentOrderId()).isEqualTo(370001L);
        assertThat(record.getBusinessOrderId()).isEqualTo(360001L);
        assertThat(record.getContractId()).isEqualTo(331001L);
        assertThat(record.getQueryType()).isEqualTo("ACTIVE_QUERY");
        assertThat(record.getBeforeStatus()).isEqualTo("PAYING");
        assertThat(record.getChannelStatus()).isEqualTo("SUCCESS");
        assertThat(record.getResultStatus()).isEqualTo("SUCCESS");
        assertThat(record.getProcessResult()).isEqualTo("UPDATED");
        assertThat(record.getRequestPayload()).contains("payOrderNo");
        assertThat(record.getResponsePayload()).contains("SUCCESS");
        assertThat(record.getTenantId()).isEqualTo("1");
        assertThat(record.getCreatedBy()).isEqualTo(1001L);
        verify(statusFlowService).record(
                eq("1"),
                eq(PaymentOrderStatusFlowRecorder.ORDER_TYPE_PAYMENT),
                eq(370001L),
                eq("PO202606060001"),
                eq("PAYING"),
                eq("SUCCESS"),
                eq(PaymentOrderStatusFlowRecorder.SOURCE_CHANNEL_QUERY),
                eq("PO202606060001"),
                any(LocalDateTime.class),
                eq("主动查单推进支付订单状态"));
        verify(statusFlowService).record(
                eq("1"),
                eq(PaymentOrderStatusFlowRecorder.ORDER_TYPE_BUSINESS),
                eq(360001L),
                eq("BO202606060001"),
                eq("PAYING"),
                eq("PAID"),
                eq(PaymentOrderStatusFlowRecorder.SOURCE_CHANNEL_QUERY),
                eq("PO202606060001"),
                any(LocalDateTime.class),
                eq("主动查单确认支付成功"));
        verify(observabilityService).logSummary(
                eq("CHANNEL_PAYMENT_QUERY"),
                eq("PO202606060001"),
                eq("SUCCESS"),
                eq(9900L),
                eq("MANGO_PAY"),
                any(Long.class),
                eq("UPDATED"));
    }

    @Test
    @DisplayName("syncPaymentStatus should keep PAYING order unchanged when channel still processing")
    void syncPaymentStatus_processing_keepsOrderUnchanged() {
        PaymentOrderEntity order = paymentOrder("PAYING");
        when(paymentOrderMapper.selectByTenantAndPayOrderNo("1", "PO202606060001")).thenReturn(order);
        when(channelContractMapper.selectActiveConfigValuesJson("1", 331001L)).thenReturn("{\"mangoPayScenario\":\"PROCESSING\"}");
        when(paymentOrderMapper.selectLatestFlowNo("1", 370001L)).thenReturn(null);
        when(channelQueryRecordMapper.countByTenantAndPayOrderNo("1", "PO202606060001")).thenReturn(1L);
        when(channelQueryRecordMapper.selectLastByTenantAndPayOrderNo("1", "PO202606060001"))
                .thenReturn(queryRecord("NO_CHANGE_PROCESSING"));

        PaymentChannelSynchronizer.PaymentSyncResult result = service.syncPaymentStatus("PO202606060001");

        assertThat(result.status()).isEqualTo("PAYING");
        assertThat(result.changed()).isFalse();
        assertThat(result.flowNo()).isNull();
        assertThat(result.queryCount()).isEqualTo(1L);
        assertThat(result.lastQueryResult()).isEqualTo("NO_CHANGE_PROCESSING");
        verify(paymentOrderMapper, never()).updatePayingQueryResult(any(), any(), any(), any(), any());
        verify(transactionFlowMapper, never()).insert(any(PaymentTransactionFlowEntity.class));
        verify(businessOrderMapper, never()).markCashierPaySuccess(any(), any(), any());
        verify(notificationService, never()).notifyPaymentAfterCommit(any(), any(), any());
        verify(channelQueryRecordMapper).insert(any(PaymentChannelQueryRecordEntity.class));
    }

    @Test
    @DisplayName("syncPaymentStatus should advance PAYING order to FAILED without creating success flow")
    void syncPaymentStatus_failed_advancesPaymentOrderOnly() {
        PaymentOrderEntity order = paymentOrder("PAYING");
        when(paymentOrderMapper.selectByTenantAndPayOrderNo("1", "PO202606060001")).thenReturn(order);
        when(channelContractMapper.selectActiveConfigValuesJson("1", 331001L)).thenReturn("{\"mangoPayScenario\":\"FAIL\"}");
        when(paymentOrderMapper.updatePayingQueryResult("1", 370001L, "FAILED", 0, null)).thenReturn(1);
        when(businessOrderMapper.selectCashierBusinessOrder("1", 360001L)).thenReturn(businessOrder());
        when(applicationMapper.selectOne(any())).thenReturn(application());
        when(paymentOrderMapper.selectPaymentOrderById("1", 370001L)).thenReturn(paymentOrderVO("FAILED"));
        when(channelQueryRecordMapper.countByTenantAndPayOrderNo("1", "PO202606060001")).thenReturn(2L);
        when(channelQueryRecordMapper.selectLastByTenantAndPayOrderNo("1", "PO202606060001"))
                .thenReturn(queryRecord("UPDATED"));

        PaymentChannelSynchronizer.PaymentSyncResult result = service.syncPaymentStatus("PO202606060001");

        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.changed()).isTrue();
        assertThat(result.flowNo()).isNull();
        assertThat(result.queryCount()).isEqualTo(2L);
        assertThat(result.lastQueryResult()).isEqualTo("UPDATED");
        verify(paymentOrderMapper).updatePayingQueryResult(eq("1"), eq(370001L), eq("FAILED"), eq(0), isNull());
        verify(transactionFlowMapper, never()).insert(any(PaymentTransactionFlowEntity.class));
        verify(businessOrderMapper, never()).markCashierPaySuccess(any(), any(), any());
        verify(exceptionOrderRecordService).createIfAbsent(
                eq("1"),
                eq("PO202606060001"),
                eq(PaymentExceptionOrderRecorder.TYPE_CHANNEL_FAILED),
                eq(PaymentExceptionOrderRecorder.SEVERITY_HIGH),
                eq("主动查单发现通道支付失败，支付订单已失败并等待人工核对失败原因"),
                any(LocalDateTime.class));
        assertPaymentNotification("FAILED");
        verify(channelQueryRecordMapper).insert(any(PaymentChannelQueryRecordEntity.class));
    }

    @Test
    @DisplayName("syncPaymentStatus should handle duplicate effective success through duplicate payment service")
    void syncPaymentStatus_duplicateSuccess_handlesDuplicatePayment() {
        PaymentOrderEntity order = paymentOrder("PAYING");
        when(paymentOrderMapper.selectByTenantAndPayOrderNo("1", "PO202606060001")).thenReturn(order);
        when(channelContractMapper.selectActiveConfigValuesJson("1", 331001L)).thenReturn("{\"mangoPayScenario\":\"SUCCESS\"}");
        when(paymentOrderMapper.updatePayingQueryResult(eq("1"), eq(370001L), eq("SUCCESS"), eq(1), any(LocalDateTime.class)))
                .thenThrow(new DuplicateKeyException("duplicate success"));
        when(duplicatePaymentService.handleDuplicateSuccess(
                eq("1"),
                eq(order),
                any(LocalDateTime.class),
                eq(PaymentOrderStatusFlowRecorder.SOURCE_CHANNEL_QUERY),
                eq("PO202606060001"),
                isNull()))
                .thenReturn(new PaymentDuplicatePaymentGuard.DuplicatePaymentResult(
                        "PO202606060001", "DUPLICATE_REFUNDED", true, "RO202606060001", null));
        when(paymentOrderMapper.selectLatestFlowNo("1", 370001L)).thenReturn("RFLOW202606060001");
        when(channelQueryRecordMapper.countByTenantAndPayOrderNo("1", "PO202606060001")).thenReturn(1L);
        when(channelQueryRecordMapper.selectLastByTenantAndPayOrderNo("1", "PO202606060001"))
                .thenReturn(queryRecord("DUPLICATE_REFUNDED"));

        PaymentChannelSynchronizer.PaymentSyncResult result = service.syncPaymentStatus("PO202606060001");

        assertThat(result.status()).isEqualTo("DUPLICATE_REFUNDED");
        assertThat(result.flowNo()).isEqualTo("RFLOW202606060001");
        assertThat(result.changed()).isTrue();
        verify(transactionFlowMapper, never()).insert(any(PaymentTransactionFlowEntity.class));
        verify(businessOrderMapper, never()).markCashierPaySuccess(any(), any(), any());
        verify(notificationService, never()).notifyPaymentAfterCommit(any(), any(), any());
        verify(channelQueryRecordMapper).insert(any(PaymentChannelQueryRecordEntity.class));
    }

    @Test
    @DisplayName("syncPaymentStatus should return terminal order result without mutating it")
    void syncPaymentStatus_terminal_returnsCurrentResult() {
        PaymentOrderEntity order = paymentOrder("SUCCESS");
        when(paymentOrderMapper.selectByTenantAndPayOrderNo("1", "PO202606060001")).thenReturn(order);
        when(paymentOrderMapper.selectLatestFlowNo("1", 370001L)).thenReturn("FLOW202606060001");
        when(channelQueryRecordMapper.countByTenantAndPayOrderNo("1", "PO202606060001")).thenReturn(4L);
        when(channelQueryRecordMapper.selectLastByTenantAndPayOrderNo("1", "PO202606060001"))
                .thenReturn(queryRecord("NO_QUERY_TERMINAL"));

        PaymentChannelSynchronizer.PaymentSyncResult result = service.syncPaymentStatus("PO202606060001");

        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.flowNo()).isEqualTo("FLOW202606060001");
        assertThat(result.changed()).isFalse();
        assertThat(result.queryCount()).isEqualTo(4L);
        assertThat(result.lastQueryResult()).isEqualTo("NO_QUERY_TERMINAL");
        verify(channelContractMapper, never()).selectActiveConfigValuesJson(any(), any());
        verify(paymentOrderMapper, never()).updatePayingQueryResult(any(), any(), any(), any(), any());
        verify(transactionFlowMapper, never()).insert(any(PaymentTransactionFlowEntity.class));
        verify(notificationService, never()).notifyPaymentAfterCommit(any(), any(), any());
        verify(channelQueryRecordMapper).insert(any(PaymentChannelQueryRecordEntity.class));
    }

    private PaymentOrderEntity paymentOrder(String status) {
        PaymentOrderEntity order = new PaymentOrderEntity();
        order.setId(370001L);
        order.setTenantId("1");
        order.setPayOrderNo("PO202606060001");
        order.setBusinessOrderId(360001L);
        order.setContractId(331001L);
        order.setChannelCode("MANGO_PAY");
        order.setAmount(9900L);
        order.setStatus(status);
        order.setSuccessFlag("SUCCESS".equals(status) ? 1 : 0);
        return order;
    }

    private void assertPaymentNotification(String status) {
        ArgumentCaptor<PaymentApplicationEntity> applicationCaptor = ArgumentCaptor.forClass(PaymentApplicationEntity.class);
        ArgumentCaptor<PaymentBusinessOrderEntity> businessOrderCaptor = ArgumentCaptor.forClass(PaymentBusinessOrderEntity.class);
        ArgumentCaptor<PaymentOrderVO> paymentOrderCaptor = ArgumentCaptor.forClass(PaymentOrderVO.class);
        verify(notificationService).notifyPaymentAfterCommit(
                applicationCaptor.capture(), businessOrderCaptor.capture(), paymentOrderCaptor.capture());
        assertThat(applicationCaptor.getValue().getAppId()).isEqualTo("app_openapi");
        assertThat(businessOrderCaptor.getValue().getBizOrderNo()).isEqualTo("BO202606060001");
        assertThat(paymentOrderCaptor.getValue().getPayOrderNo()).isEqualTo("PO202606060001");
        assertThat(paymentOrderCaptor.getValue().getStatus()).isEqualTo(status);
    }

    private PaymentApplicationEntity application() {
        PaymentApplicationEntity application = new PaymentApplicationEntity();
        application.setTenantId("1");
        application.setAppId("app_openapi");
        application.setAppSecret("openapi-secret");
        return application;
    }

    private PaymentBusinessOrderEntity businessOrder() {
        PaymentBusinessOrderEntity order = new PaymentBusinessOrderEntity();
        order.setId(360001L);
        order.setTenantId("1");
        order.setAppCode("app_openapi");
        order.setBizOrderNo("BO202606060001");
        order.setNotifyUrl("https://business.example.test/payment/notify");
        return order;
    }

    private PaymentOrderProjection paymentOrderVO(String status) {
        PaymentOrderProjection order = new PaymentOrderProjection();
        order.setId(370001L);
        order.setPayOrderNo("PO202606060001");
        order.setBusinessOrderId(360001L);
        order.setBizOrderNo("BO202606060001");
        order.setAppId("app_openapi");
        order.setAmount(9900L);
        order.setCurrency("CNY");
        order.setStatus(status);
        order.setChannelCode("MANGO_PAY");
        order.setFlowNo("SUCCESS".equals(status) ? "FLOW202606060001" : null);
        return order;
    }

    private PaymentChannelQueryRecordEntity queryRecord(String processResult) {
        PaymentChannelQueryRecordEntity record = new PaymentChannelQueryRecordEntity();
        record.setProcessResult(processResult);
        return record;
    }

    private static class TestTransactionManager extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }
}
