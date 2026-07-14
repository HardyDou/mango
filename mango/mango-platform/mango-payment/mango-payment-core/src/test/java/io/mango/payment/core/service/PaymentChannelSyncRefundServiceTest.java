package io.mango.payment.core.service;

import io.mango.payment.core.service.impl.PaymentMangoPayScenarioControlService;
import io.mango.payment.core.service.impl.PaymentObservabilityService;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.exception.BizException;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.payment.api.enums.PaymentCode;
import io.mango.payment.api.vo.PaymentRefundOrderVO;
import io.mango.payment.core.model.projection.PaymentRefundOrderProjection;
import io.mango.payment.core.entity.PaymentApplicationEntity;
import io.mango.payment.core.entity.PaymentBusinessOrderEntity;
import io.mango.payment.core.entity.PaymentRefundQueryRecordEntity;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentChannelSyncRefundServiceTest {

    private PaymentOrderMapper paymentOrderMapper;
    private PaymentRefundOrderMapper refundOrderMapper;
    private PaymentRefundQueryRecordMapper refundQueryRecordMapper;
    private PaymentBusinessOrderMapper businessOrderMapper;
    private PaymentTransactionFlowMapper transactionFlowMapper;
    private PaymentChannelContractMapper channelContractMapper;
    private PaymentChannelQueryRecordMapper channelQueryRecordMapper;
    private PaymentApplicationMapper applicationMapper;
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
        refundQueryRecordMapper = mock(PaymentRefundQueryRecordMapper.class);
        businessOrderMapper = mock(PaymentBusinessOrderMapper.class);
        transactionFlowMapper = mock(PaymentTransactionFlowMapper.class);
        channelContractMapper = mock(PaymentChannelContractMapper.class);
        channelQueryRecordMapper = mock(PaymentChannelQueryRecordMapper.class);
        applicationMapper = mock(PaymentApplicationMapper.class);
        notificationService = mock(PaymentNotificationDispatcher.class);
        scenarioControlService = mock(PaymentMangoPayScenarioControlService.class);
        statusFlowService = mock(PaymentOrderStatusFlowRecorder.class);
        duplicatePaymentService = mock(PaymentDuplicatePaymentGuard.class);
        duplicateRefundCompletionService = mock(PaymentRefundCompletionDeduplicator.class);
        observabilityService = mock(PaymentObservabilityService.class);
        exceptionOrderRecordService = mock(PaymentExceptionOrderRecorder.class);
        numberService = mock(PaymentNumberGenerator.class);
        when(numberService.next(PaymentNumberGenerator.PAY_REFUND_FLOW_NO)).thenReturn("RF2026060600000001");
        when(numberService.next(PaymentNumberGenerator.PAY_REFUND_QUERY_NO)).thenReturn("RQ2026060600000001");
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
                new NoopTransactionManager());
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1001L, "1", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L, "internal-admin"));
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    @DisplayName("syncRefundStatus should advance refunding order to success and create refund flow")
    void syncRefundStatus_success_advancesRefundAndCreatesFlow() {
        PaymentRefundOrderProjection refundOrder = refundOrder("REFUNDING");
        when(refundOrderMapper.selectByTenantAndRefundOrderNo("1", "RO202606060001")).thenReturn(refundOrder);
        when(channelContractMapper.selectActiveConfigValuesJson("1", 331001L)).thenReturn("{\"mangoPayRefundScenario\":\"SUCCESS\"}");
        when(refundOrderMapper.updateRefundingQueryResult(eq("1"), eq(380001L), eq("SUCCESS"), any(LocalDateTime.class))).thenReturn(1);
        when(businessOrderMapper.updateRefundProgress("1", 360001L, 3300L)).thenReturn(1);
        when(applicationMapper.selectOne(any())).thenReturn(application());
        when(businessOrderMapper.selectCashierBusinessOrder("1", 360001L)).thenReturn(businessOrder());
        when(refundQueryRecordMapper.countByTenantAndRefundOrderNo("1", "RO202606060001")).thenReturn(2L);
        when(refundQueryRecordMapper.selectLastByTenantAndRefundOrderNo("1", "RO202606060001")).thenReturn(queryRecord("UPDATED"));
        ArgumentCaptor<PaymentTransactionFlowEntity> flowCaptor = ArgumentCaptor.forClass(PaymentTransactionFlowEntity.class);
        ArgumentCaptor<PaymentRefundQueryRecordEntity> queryRecordCaptor = ArgumentCaptor.forClass(PaymentRefundQueryRecordEntity.class);

        PaymentChannelSynchronizer.RefundSyncResult result = service.syncRefundStatus("RO202606060001");

        assertThat(result.refundOrderNo()).isEqualTo("RO202606060001");
        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.changed()).isTrue();
        assertThat(result.flowNo()).startsWith("RF");
        assertThat(result.queryCount()).isEqualTo(2L);
        verify(businessOrderMapper).updateRefundProgress("1", 360001L, 3300L);
        verify(transactionFlowMapper).insert(flowCaptor.capture());
        assertThat(flowCaptor.getValue().getFlowType()).isEqualTo("REFUND_SUCCESS");
        assertThat(flowCaptor.getValue().getAmount()).isEqualTo(3300L);
        verify(notificationService).notifyRefundAfterCommit(any(PaymentApplicationEntity.class), any(PaymentBusinessOrderEntity.class), any(PaymentRefundOrderVO.class));
        verify(refundQueryRecordMapper).insert(queryRecordCaptor.capture());
        PaymentRefundQueryRecordEntity record = queryRecordCaptor.getValue();
        assertThat(record.getRefundOrderNo()).isEqualTo("RO202606060001");
        assertThat(record.getBusinessOrderId()).isEqualTo(360001L);
        assertThat(record.getBeforeStatus()).isEqualTo("REFUNDING");
        assertThat(record.getChannelStatus()).isEqualTo("SUCCESS");
        assertThat(record.getResultStatus()).isEqualTo("SUCCESS");
        assertThat(record.getProcessResult()).isEqualTo("UPDATED");
        assertThat(record.getTenantId()).isEqualTo("1");
        assertThat(record.getCreatedBy()).isEqualTo(1001L);
        verify(observabilityService).logSummary(argThat(summary ->
                "CHANNEL_REFUND_QUERY".equals(summary.event())
                        && "RO202606060001".equals(summary.orderNo())
                        && "SUCCESS".equals(summary.status())
                        && Long.valueOf(3300L).equals(summary.amount())
                        && "MANGO_PAY".equals(summary.channelCode())
                        && summary.durationMillis() >= 0
                        && "UPDATED".equals(summary.result())));
    }

    @Test
    @DisplayName("syncRefundStatus should reject success when business refund progress CAS fails")
    void syncRefundStatus_successBusinessCasFailed_rejectsWithoutFlowOrNotification() {
        PaymentRefundOrderProjection refundOrder = refundOrder("REFUNDING");
        when(refundOrderMapper.selectByTenantAndRefundOrderNo("1", "RO202606060001")).thenReturn(refundOrder);
        when(channelContractMapper.selectActiveConfigValuesJson("1", 331001L)).thenReturn("{\"mangoPayRefundScenario\":\"SUCCESS\"}");
        when(refundOrderMapper.updateRefundingQueryResult(eq("1"), eq(380001L), eq("SUCCESS"), any(LocalDateTime.class))).thenReturn(1);
        when(businessOrderMapper.selectCashierBusinessOrder("1", 360001L)).thenReturn(businessOrder());
        when(businessOrderMapper.updateRefundProgress("1", 360001L, 3300L)).thenReturn(0);

        assertThatThrownBy(() -> service.syncRefundStatus("RO202606060001"))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(PaymentCode.PAYMENT_REFUND_AMOUNT_EXCEEDED.getCode());
        verify(transactionFlowMapper, never()).insert(any(PaymentTransactionFlowEntity.class));
        verify(notificationService, never()).notifyRefundAfterCommit(any(), any(), any());
        verify(refundQueryRecordMapper, never()).insert(any(PaymentRefundQueryRecordEntity.class));
    }

    @Test
    @DisplayName("syncRefundStatus should allow only one success side effect when refund query races")
    void syncRefundStatus_successRace_allowsOnlyOneSuccessSideEffect() {
        PaymentRefundOrderProjection successOrder = refundOrder("SUCCESS");
        successOrder.setFlowNo("RF2026060600000001");
        when(refundOrderMapper.selectByTenantAndRefundOrderNo("1", "RO202606060001"))
                .thenReturn(refundOrder("REFUNDING"), refundOrder("REFUNDING"), successOrder);
        when(channelContractMapper.selectActiveConfigValuesJson("1", 331001L))
                .thenReturn("{\"mangoPayRefundScenario\":\"SUCCESS\"}");
        when(refundOrderMapper.updateRefundingQueryResult(eq("1"), eq(380001L), eq("SUCCESS"), any(LocalDateTime.class)))
                .thenReturn(1, 0);
        when(businessOrderMapper.updateRefundProgress("1", 360001L, 3300L)).thenReturn(1);
        when(applicationMapper.selectOne(any())).thenReturn(application());
        when(businessOrderMapper.selectCashierBusinessOrder("1", 360001L)).thenReturn(businessOrder());
        when(refundQueryRecordMapper.countByTenantAndRefundOrderNo("1", "RO202606060001")).thenReturn(1L);
        when(refundQueryRecordMapper.selectLastByTenantAndRefundOrderNo("1", "RO202606060001"))
                .thenReturn(queryRecord("UPDATED"), queryRecord("IDEMPOTENT_TERMINAL"));

        PaymentChannelSynchronizer.RefundSyncResult result = service.syncRefundStatus("RO202606060001");

        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.changed()).isTrue();
        PaymentChannelSynchronizer.RefundSyncResult idempotentResult = service.syncRefundStatus("RO202606060001");
        assertThat(idempotentResult.status()).isEqualTo("SUCCESS");
        assertThat(idempotentResult.changed()).isFalse();
        assertThat(idempotentResult.flowNo()).isEqualTo("RF2026060600000001");
        verify(refundOrderMapper, times(2)).updateRefundingQueryResult(
                eq("1"),
                eq(380001L),
                eq("SUCCESS"),
                any(LocalDateTime.class));
        verify(businessOrderMapper, times(1)).updateRefundProgress("1", 360001L, 3300L);
        verify(transactionFlowMapper, times(1)).insert(any(PaymentTransactionFlowEntity.class));
        verify(notificationService, times(1)).notifyRefundAfterCommit(
                any(PaymentApplicationEntity.class),
                any(PaymentBusinessOrderEntity.class),
                any(PaymentRefundOrderVO.class));
        verify(refundQueryRecordMapper, times(2)).insert(any(PaymentRefundQueryRecordEntity.class));
    }

    @Test
    @DisplayName("syncRefundStatus should keep refunding order unchanged when channel still processing")
    void syncRefundStatus_processing_keepsRefundUnchanged() {
        when(refundOrderMapper.selectByTenantAndRefundOrderNo("1", "RO202606060001")).thenReturn(refundOrder("REFUNDING"));
        when(channelContractMapper.selectActiveConfigValuesJson("1", 331001L)).thenReturn("{\"mangoPayRefundScenario\":\"PROCESSING\"}");
        when(refundQueryRecordMapper.countByTenantAndRefundOrderNo("1", "RO202606060001")).thenReturn(1L);
        when(refundQueryRecordMapper.selectLastByTenantAndRefundOrderNo("1", "RO202606060001")).thenReturn(queryRecord("NO_CHANGE_PROCESSING"));

        PaymentChannelSynchronizer.RefundSyncResult result = service.syncRefundStatus("RO202606060001");

        assertThat(result.status()).isEqualTo("REFUNDING");
        assertThat(result.changed()).isFalse();
        assertThat(result.flowNo()).isNull();
        verify(refundOrderMapper, never()).updateRefundingQueryResult(any(), any(), any(), any());
        verify(businessOrderMapper, never()).updateRefundProgress(any(), any(), any());
        verify(transactionFlowMapper, never()).insert(any(PaymentTransactionFlowEntity.class));
        verify(notificationService, never()).notifyRefundAfterCommit(any(), any(), any());
        verify(refundQueryRecordMapper).insert(any(PaymentRefundQueryRecordEntity.class));
    }

    @Test
    @DisplayName("syncRefundStatus should advance refunding order to failed without success side effects")
    void syncRefundStatus_failed_advancesRefundOnly() {
        when(refundOrderMapper.selectByTenantAndRefundOrderNo("1", "RO202606060001")).thenReturn(refundOrder("REFUNDING"));
        when(channelContractMapper.selectActiveConfigValuesJson("1", 331001L)).thenReturn("{\"mangoPayRefundScenario\":\"FAIL\"}");
        when(refundOrderMapper.updateRefundingQueryResult("1", 380001L, "FAILED", null)).thenReturn(1);
        when(applicationMapper.selectOne(any())).thenReturn(application());
        when(businessOrderMapper.selectCashierBusinessOrder("1", 360001L)).thenReturn(businessOrder());
        when(refundQueryRecordMapper.countByTenantAndRefundOrderNo("1", "RO202606060001")).thenReturn(3L);
        when(refundQueryRecordMapper.selectLastByTenantAndRefundOrderNo("1", "RO202606060001")).thenReturn(queryRecord("UPDATED"));

        PaymentChannelSynchronizer.RefundSyncResult result = service.syncRefundStatus("RO202606060001");

        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.changed()).isTrue();
        assertThat(result.flowNo()).isNull();
        verify(refundOrderMapper).updateRefundingQueryResult(eq("1"), eq(380001L), eq("FAILED"), isNull());
        verify(businessOrderMapper, never()).updateRefundProgress(any(), any(), any());
        verify(transactionFlowMapper, never()).insert(any(PaymentTransactionFlowEntity.class));
        verify(exceptionOrderRecordService).createIfAbsent(
                eq("1"),
                eq("RO202606060001"),
                eq(PaymentExceptionOrderRecorder.TYPE_REFUND_MISMATCH),
                eq(PaymentExceptionOrderRecorder.SEVERITY_HIGH),
                eq("主动查退款发现通道退款失败，退款订单已失败并等待人工核对退款结果"),
                any(LocalDateTime.class));
        verify(notificationService).notifyRefundAfterCommit(any(PaymentApplicationEntity.class), any(PaymentBusinessOrderEntity.class), any(PaymentRefundOrderVO.class));
        verify(refundQueryRecordMapper).insert(any(PaymentRefundQueryRecordEntity.class));
    }

    @Test
    @DisplayName("syncRefundStatus should record terminal refund query without mutating it")
    void syncRefundStatus_terminal_recordsOnly() {
        PaymentRefundOrderProjection refundOrder = refundOrder("SUCCESS");
        refundOrder.setFlowNo("RFLOW202606060001");
        when(refundOrderMapper.selectByTenantAndRefundOrderNo("1", "RO202606060001")).thenReturn(refundOrder);
        when(refundQueryRecordMapper.countByTenantAndRefundOrderNo("1", "RO202606060001")).thenReturn(4L);
        when(refundQueryRecordMapper.selectLastByTenantAndRefundOrderNo("1", "RO202606060001")).thenReturn(queryRecord("NO_QUERY_TERMINAL"));

        PaymentChannelSynchronizer.RefundSyncResult result = service.syncRefundStatus("RO202606060001");

        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.changed()).isFalse();
        assertThat(result.flowNo()).isEqualTo("RFLOW202606060001");
        assertThat(result.lastQueryResult()).isEqualTo("NO_QUERY_TERMINAL");
        verify(channelContractMapper, never()).selectActiveConfigValuesJson(any(), any());
        verify(refundOrderMapper, never()).updateRefundingQueryResult(any(), any(), any(), any());
        verify(transactionFlowMapper, never()).insert(any(PaymentTransactionFlowEntity.class));
        verify(refundQueryRecordMapper).insert(any(PaymentRefundQueryRecordEntity.class));
    }

    private PaymentRefundOrderProjection refundOrder(String status) {
        PaymentRefundOrderProjection order = new PaymentRefundOrderProjection();
        order.setId(380001L);
        order.setContractId(331001L);
        order.setRefundOrderNo("RO202606060001");
        order.setBizRefundNo("RF_OPENAPI_001");
        order.setPaymentOrderId(370001L);
        order.setBusinessOrderId(360001L);
        order.setPayOrderNo("PO202606060001");
        order.setBizOrderNo("BIZ_OPENAPI_001");
        order.setAppId("app_openapi");
        order.setRefundAmount(3300L);
        order.setCurrency("CNY");
        order.setReason("开放接口退款");
        order.setStatus(status);
        order.setMethodCode("PERSONAL_WECHAT_QR");
        order.setChannelCode("MANGO_PAY");
        order.setChannelTradeNo("CASHIER-PO202606060001");
        order.setChannelRefundNo("CASHIER-REFUND-RO202606060001");
        return order;
    }

    private PaymentApplicationEntity application() {
        PaymentApplicationEntity application = new PaymentApplicationEntity();
        application.setId(310001L);
        application.setTenantId("1");
        application.setAppId("app_openapi");
        application.setAppSecret("openapi-secret");
        application.setSecretConfigured(1);
        application.setSignAlgorithm("HMAC_SHA256");
        application.setStatus(1);
        return application;
    }

    private PaymentBusinessOrderEntity businessOrder() {
        PaymentBusinessOrderEntity order = new PaymentBusinessOrderEntity();
        order.setId(360001L);
        order.setTenantId("1");
        order.setAppCode("app_openapi");
        order.setBizOrderNo("BIZ_OPENAPI_001");
        order.setStatus("SUCCESS");
        order.setNotifyUrl("https://business.example.test/payment/notify");
        return order;
    }

    private PaymentRefundQueryRecordEntity queryRecord(String processResult) {
        PaymentRefundQueryRecordEntity record = new PaymentRefundQueryRecordEntity();
        record.setProcessResult(processResult);
        return record;
    }

    private static final class NoopTransactionManager implements PlatformTransactionManager {

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
        }

        @Override
        public void rollback(TransactionStatus status) {
        }
    }
}
