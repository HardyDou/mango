package io.mango.payment.core.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.exception.BizException;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.command.PaymentOpenRequestCommand;
import io.mango.payment.api.vo.PaymentCashierPayResultVO;
import io.mango.payment.api.vo.PaymentOpenBusinessOrderVO;
import io.mango.payment.api.vo.PaymentOpenCashierVO;
import io.mango.payment.api.vo.PaymentOpenPaymentOrderVO;
import io.mango.payment.api.vo.PaymentOpenReceiptVO;
import io.mango.payment.api.vo.PaymentOpenRefundOrderVO;
import io.mango.payment.api.vo.PaymentOrderVO;
import io.mango.payment.api.vo.PaymentRefundOrderVO;
import io.mango.payment.core.entity.PaymentApplication;
import io.mango.payment.core.entity.PaymentBusinessOrderEntity;
import io.mango.payment.core.entity.PaymentCashierConfig;
import io.mango.payment.core.entity.PaymentOpenApiNonceEntity;
import io.mango.payment.core.entity.PaymentRefundOrderEntity;
import io.mango.payment.core.entity.PaymentTransactionFlowEntity;
import io.mango.payment.core.mapper.PaymentApplicationMapper;
import io.mango.payment.core.mapper.PaymentBusinessOrderMapper;
import io.mango.payment.core.mapper.PaymentCashierConfigMapper;
import io.mango.payment.core.mapper.PaymentChannelContractMapper;
import io.mango.payment.core.mapper.PaymentOpenApiNonceMapper;
import io.mango.payment.core.mapper.PaymentOrderMapper;
import io.mango.payment.core.mapper.PaymentRefundOrderMapper;
import io.mango.payment.core.mapper.PaymentTransactionFlowMapper;
import io.mango.payment.core.service.IPaymentCashierService;
import io.mango.payment.core.service.PaymentChannelAdapterRegistry;
import io.mango.payment.core.service.PaymentNotificationService;
import io.mango.payment.core.service.PaymentOrderStatusFlowService;
import io.mango.payment.core.service.PaymentOrderStateService;
import io.mango.payment.core.service.PaymentMangoPayChannelAdapter;
import io.mango.payment.core.service.PaymentMangoPayScenarioControlService;
import io.mango.payment.core.service.PaymentMangoPayResultMappingService;
import io.mango.payment.core.service.PaymentNumberService;
import io.mango.payment.core.service.PaymentRefundApplyService;
import io.mango.payment.core.service.PaymentSensitiveValueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentOpenApiServiceTest {

    private static final String APP_ID = "app_openapi";
    private static final String APP_SECRET = "openapi-secret";
    private static final String TENANT_ID = "1";

    private PaymentApplicationMapper applicationMapper;
    private PaymentBusinessOrderMapper businessOrderMapper;
    private PaymentCashierConfigMapper cashierConfigMapper;
    private PaymentChannelContractMapper channelContractMapper;
    private PaymentOpenApiNonceMapper nonceMapper;
    private PaymentOrderMapper paymentOrderMapper;
    private PaymentRefundOrderMapper refundOrderMapper;
    private PaymentTransactionFlowMapper transactionFlowMapper;
    private IPaymentCashierService cashierService;
    private PaymentNotificationService notificationService;
    private PaymentMangoPayScenarioControlService scenarioControlService;
    private PaymentOrderStatusFlowService statusFlowService;
    private PaymentSensitiveValueService sensitiveValueService;
    private PaymentNumberService numberService;
    private TestTransactionManager transactionManager;
    private PaymentOpenApiService service;

    @BeforeEach
    void setUp() {
        applicationMapper = mock(PaymentApplicationMapper.class);
        businessOrderMapper = mock(PaymentBusinessOrderMapper.class);
        cashierConfigMapper = mock(PaymentCashierConfigMapper.class);
        channelContractMapper = mock(PaymentChannelContractMapper.class);
        nonceMapper = mock(PaymentOpenApiNonceMapper.class);
        paymentOrderMapper = mock(PaymentOrderMapper.class);
        refundOrderMapper = mock(PaymentRefundOrderMapper.class);
        transactionFlowMapper = mock(PaymentTransactionFlowMapper.class);
        cashierService = mock(IPaymentCashierService.class);
        notificationService = mock(PaymentNotificationService.class);
        scenarioControlService = mock(PaymentMangoPayScenarioControlService.class);
        statusFlowService = mock(PaymentOrderStatusFlowService.class);
        sensitiveValueService = mock(PaymentSensitiveValueService.class);
        numberService = mock(PaymentNumberService.class);
        transactionManager = new TestTransactionManager();
        service = new PaymentOpenApiService(
                applicationMapper,
                businessOrderMapper,
                cashierConfigMapper,
                nonceMapper,
                paymentOrderMapper,
                refundOrderMapper,
                cashierService,
                new PaymentOrderStateService(),
                statusFlowService,
                new PaymentRefundApplyService(
                        businessOrderMapper,
                        paymentOrderMapper,
                        refundOrderMapper,
                        new PaymentOrderStateService(),
                        statusFlowService,
                        new PaymentChannelAdapterRegistry(java.util.List.of(new PaymentMangoPayChannelAdapter(
                                channelContractMapper,
                                mock(io.mango.payment.core.mapper.PaymentReconciliationMapper.class),
                                scenarioControlService,
                        new PaymentMangoPayResultMappingService()))),
                        numberService,
                        new TestTransactionManager()),
                sensitiveValueService,
                new ObjectMapper(),
                transactionManager);
        when(applicationMapper.selectOne(any())).thenReturn(application());
        when(sensitiveValueService.decrypt("enc:openapi-secret-ciphertext")).thenReturn(APP_SECRET);
        when(cashierConfigMapper.selectOne(any())).thenReturn(cashierConfig());
        when(paymentOrderMapper.selectOpenPaymentOrder(any(), any(), any())).thenReturn(paymentOrder());
        when(paymentOrderMapper.selectSuccessfulOpenPaymentOrder(any(), any(), any())).thenReturn(paymentOrder());
        when(paymentOrderMapper.lockSuccessfulOpenPaymentOrder(1L, 370001L)).thenReturn(370001L);
        when(paymentOrderMapper.selectLatestFlowNo(1L, 370001L)).thenReturn("FLOW202606060001");
        when(refundOrderMapper.sumOccupyingRefundAmount(1L, 370001L)).thenReturn(0L);
        when(refundOrderMapper.updateRefundApplyResult(any(), any(), any(), any())).thenReturn(1);
        when(refundOrderMapper.selectLatestFlowNo(1L, 380001L)).thenReturn("RFLOW202606060001");
        when(channelContractMapper.selectActiveConfigValuesJson(1L, 331001L)).thenReturn("{\"mangoPayRefundScenario\":\"SUCCESS\"}");
        when(numberService.next(PaymentNumberService.PAY_REFUND_ORDER_NO)).thenReturn("RO2026060600000001");
    }

    @Test
    @DisplayName("createOrder should authenticate, persist nonce, and create real business order")
    void createOrder_validSignature_createsBusinessOrder() {
        String body = createOrderBody(8800L);
        String timestamp = timestamp();
        String nonce = "nonce-create";
        ArgumentCaptor<PaymentBusinessOrderEntity> orderCaptor = ArgumentCaptor.forClass(PaymentBusinessOrderEntity.class);

        PaymentOpenBusinessOrderVO result = service.createOrder(openRequest(
                body, APP_ID, TENANT_ID, timestamp, nonce, signature("POST", "/openapi/pay/orders", body, timestamp, nonce),
                "/openapi/pay/orders", null, null, null, null)).getData();

        verify(nonceMapper).insert(any(PaymentOpenApiNonceEntity.class));
        verify(businessOrderMapper).insert(orderCaptor.capture());
        PaymentBusinessOrderEntity entity = orderCaptor.getValue();
        assertThat(entity.getTenantId()).isEqualTo(1L);
        assertThat(entity.getAppCode()).isEqualTo(APP_ID);
        assertThat(entity.getBizOrderNo()).isEqualTo("BIZ_OPENAPI_001");
        assertThat(entity.getSubjectId()).isEqualTo(320001L);
        assertThat(entity.getAmount()).isEqualTo(8800L);
        assertThat(entity.getStatus()).isEqualTo("TO_PAY");
        assertThat(result.getAppId()).isEqualTo(APP_ID);
        assertThat(result.getAmount()).isEqualTo(8800L);
    }

    @Test
    @DisplayName("createOrder should record nonce in independent transaction")
    void createOrder_nonceUsesRequiresNewTransaction() {
        String body = createOrderBody(8800L);
        String timestamp = timestamp();
        String nonce = "nonce-create-requires-new";

        service.createOrder(openRequest(
                body, APP_ID, TENANT_ID, timestamp, nonce, signature("POST", "/openapi/pay/orders", body, timestamp, nonce),
                "/openapi/pay/orders", null, null, null, null));

        assertThat(transactionManager.propagationBehaviors())
                .contains(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Test
    @DisplayName("createOrder should return existing order when idempotent fields are same")
    void createOrder_sameIdempotentFields_returnsExistingOrder() {
        PaymentBusinessOrderEntity existing = businessOrder(8800L);
        when(businessOrderMapper.selectOne(any())).thenReturn(existing);
        String body = createOrderBody(8800L);
        String timestamp = timestamp();
        String nonce = "nonce-idempotent";

        PaymentOpenBusinessOrderVO result = service.createOrder(openRequest(
                body, APP_ID, TENANT_ID, timestamp, nonce, signature("POST", "/openapi/pay/orders", body, timestamp, nonce),
                "/openapi/pay/orders", null, null, null, null)).getData();

        assertThat(result.getId()).isEqualTo(360001L);
        assertThat(result.getAmount()).isEqualTo(8800L);
    }

    @Test
    @DisplayName("createOrder should reject when idempotent key has conflicting amount")
    void createOrder_conflictingIdempotentFields_rejects() {
        when(businessOrderMapper.selectOne(any())).thenReturn(businessOrder(9900L));
        String body = createOrderBody(8800L);
        String timestamp = timestamp();
        String nonce = "nonce-conflict";

        assertThatThrownBy(() -> service.createOrder(openRequest(
                body, APP_ID, TENANT_ID, timestamp, nonce, signature("POST", "/openapi/pay/orders", body, timestamp, nonce),
                "/openapi/pay/orders", null, null, null, null)))
                .isInstanceOf(BizException.class)
                .hasMessage(PaymentCode.PAYMENT_OPENAPI_IDEMPOTENT_CONFLICT.getMessage());
    }

    @Test
    @DisplayName("detailOrder should reject replayed nonce")
    void detailOrder_replayedNonce_rejects() {
        when(nonceMapper.insert(any(PaymentOpenApiNonceEntity.class))).thenThrow(new DuplicateKeyException("duplicate nonce"));
        String timestamp = timestamp();
        String nonce = "nonce-replay";

        assertThatThrownBy(() -> service.detailOrder(openRequest(
                null, APP_ID, TENANT_ID, timestamp, nonce,
                signature("GET", "/openapi/pay/orders/BIZ_OPENAPI_001", "", timestamp, nonce),
                "/openapi/pay/orders/BIZ_OPENAPI_001",
                "192.0.2.20", "BIZ_OPENAPI_001", null, null)))
                .isInstanceOf(BizException.class)
                .hasMessage(PaymentCode.PAYMENT_OPENAPI_NONCE_REPLAY.getMessage());
    }

    @Test
    @DisplayName("cashier should return stable cashier route for unexpired paying order")
    void cashier_unexpiredPayingOrder_returnsCashierUrl() {
        when(businessOrderMapper.selectOne(any())).thenReturn(businessOrder(8800L));
        String timestamp = timestamp();
        String nonce = "nonce-cashier";

        PaymentOpenCashierVO result = service.cashier(openRequest(
                "", APP_ID, TENANT_ID, timestamp, nonce,
                signature("POST", "/openapi/pay/orders/BIZ_OPENAPI_001/cashier", "", timestamp, nonce),
                "/openapi/pay/orders/BIZ_OPENAPI_001/cashier", null, "BIZ_OPENAPI_001", null, null)).getData();

        assertThat(result.getCashierConfigId()).isEqualTo(350001L);
        assertThat(result.getBusinessOrderId()).isEqualTo(360001L);
        assertThat(result.getCashierUrl()).isEqualTo("/payment/cashier-configs/350001/cashier?businessOrderId=360001");
    }

    @Test
    @DisplayName("pay should authenticate and reuse cashier payment service")
    void pay_validSignature_createsPaymentOrderThroughCashierService() {
        when(businessOrderMapper.selectOne(any())).thenReturn(businessOrder(8800L));
        when(paymentOrderMapper.selectOpenPaymentOrder(any(), any(), any())).thenReturn(paymentOrder("PAYING"));
        when(cashierService.pay(any())).thenReturn(io.mango.common.result.R.ok(cashierPayResult()));
        String body = "{\"methodCode\":\"PERSONAL_WECHAT_QR\"}";
        String timestamp = timestamp();
        String nonce = "nonce-pay";

        PaymentOpenPaymentOrderVO result = service.pay(openRequest(
                body, APP_ID, TENANT_ID, timestamp, nonce,
                signature("POST", "/openapi/pay/orders/BIZ_OPENAPI_001/pay", body, timestamp, nonce),
                "/openapi/pay/orders/BIZ_OPENAPI_001/pay",
                "192.0.2.20", "BIZ_OPENAPI_001", null, null)).getData();

        assertThat(result.getPayOrderNo()).isEqualTo("PO202606060001");
        assertThat(result.getBizOrderNo()).isEqualTo("BIZ_OPENAPI_001");
        assertThat(result.getAppId()).isEqualTo(APP_ID);
        assertThat(result.getMethodCode()).isEqualTo("PERSONAL_WECHAT_QR");
        assertThat(result.getAmount()).isEqualTo(8800L);
        assertThat(result.getFlowNo()).isEqualTo("FLOW202606060001");
        assertThat(result.getStatus()).isEqualTo("PAYING");
        verify(notificationService, never()).notifyPaymentAfterCommit(any(), any(), any());
    }

    @Test
    @DisplayName("detailPaymentOrder should query payment order under signed app")
    void detailPaymentOrder_validSignature_returnsPaymentOrder() {
        String timestamp = timestamp();
        String nonce = "nonce-payment-detail";

        PaymentOpenPaymentOrderVO result = service.detailPaymentOrder(openRequest(
                null, APP_ID, TENANT_ID, timestamp, nonce,
                signature("GET", "/openapi/pay/payment-orders/PO202606060001", "", timestamp, nonce),
                "/openapi/pay/payment-orders/PO202606060001", null, null, "PO202606060001", null)).getData();

        assertThat(result.getPayOrderNo()).isEqualTo("PO202606060001");
        assertThat(result.getBizOrderNo()).isEqualTo("BIZ_OPENAPI_001");
        assertThat(result.getAppId()).isEqualTo(APP_ID);
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getFlowNo()).isEqualTo("FLOW202606060001");
    }

    @Test
    @DisplayName("receipt should query successful payment receipt under signed app")
    void receipt_validSignature_returnsPaymentReceipt() {
        String timestamp = timestamp();
        String nonce = "nonce-receipt";

        PaymentOpenReceiptVO result = service.receipt(openRequest(
                null, APP_ID, TENANT_ID, timestamp, nonce,
                signature("GET", "/openapi/pay/receipts/BIZ_OPENAPI_001", "", timestamp, nonce),
                "/openapi/pay/receipts/BIZ_OPENAPI_001", null, "BIZ_OPENAPI_001", null, null)).getData();

        assertThat(result.getReceiptNo()).isEqualTo("RCPT-BIZ_OPENAPI_001-PO202606060001");
        assertThat(result.getBizOrderNo()).isEqualTo("BIZ_OPENAPI_001");
        assertThat(result.getPayOrderNo()).isEqualTo("PO202606060001");
        assertThat(result.getAppId()).isEqualTo(APP_ID);
        assertThat(result.getAmount()).isEqualTo(8800L);
        assertThat(result.getCurrency()).isEqualTo("CNY");
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getMethodCode()).isEqualTo("PERSONAL_WECHAT_QR");
        assertThat(result.getChannelCode()).isEqualTo("MANGO_PAY");
        assertThat(result.getChannelTradeNo()).isEqualTo("CASHIER-PO202606060001");
        assertThat(result.getFlowNo()).isEqualTo("FLOW202606060001");
        assertThat(result.getPayTime()).isNotNull();
        assertThat(result.getIssuedTime()).isEqualTo(result.getPayTime());
    }

    @Test
    @DisplayName("refund should create refunding order without success side effects even when channel returns success")
    void refund_validSignature_createsRefundingOrderOnly() {
        when(businessOrderMapper.selectOne(any())).thenReturn(successBusinessOrder(8800L));
        when(refundOrderMapper.selectOpenRefundOrder(any(), any(), any())).thenReturn(null, refundOrder("REFUNDING"));
        String body = refundBody(3300L);
        String timestamp = timestamp();
        String nonce = "nonce-refund";
        ArgumentCaptor<PaymentRefundOrderEntity> refundCaptor = ArgumentCaptor.forClass(PaymentRefundOrderEntity.class);

        PaymentOpenRefundOrderVO result = service.refund(openRequest(
                body, APP_ID, TENANT_ID, timestamp, nonce,
                signature("POST", "/openapi/pay/refunds", body, timestamp, nonce),
                "/openapi/pay/refunds", null, null, null, null)).getData();

        verify(refundOrderMapper).insert(refundCaptor.capture());
        PaymentRefundOrderEntity refundEntity = refundCaptor.getValue();
        assertThat(refundEntity.getBizRefundNo()).isEqualTo("RF_OPENAPI_001");
        assertThat(refundEntity.getPaymentOrderId()).isEqualTo(370001L);
        assertThat(refundEntity.getRefundAmount()).isEqualTo(3300L);
        assertThat(refundEntity.getStatus()).isEqualTo("REFUNDING");
        assertThat(refundEntity.getRefundTime()).isNull();
        assertThat(refundEntity.getChannelRefundNo()).isNull();
        verify(refundOrderMapper).updateRefundApplyResult(1L, null, "MRRO2026060600000001", "REFUNDING");
        assertThat(result.getBizRefundNo()).isEqualTo("RF_OPENAPI_001");
        assertThat(result.getBizOrderNo()).isEqualTo("BIZ_OPENAPI_001");
        assertThat(result.getRefundAmount()).isEqualTo(3300L);
        assertThat(result.getStatus()).isEqualTo("REFUNDING");
        assertThat(result.getFlowNo()).isNull();
        InOrder refundConcurrencyOrder = inOrder(paymentOrderMapper, refundOrderMapper);
        refundConcurrencyOrder.verify(paymentOrderMapper).lockSuccessfulOpenPaymentOrder(1L, 370001L);
        refundConcurrencyOrder.verify(refundOrderMapper).sumOccupyingRefundAmount(1L, 370001L);
        verify(businessOrderMapper, never()).updateRefundProgress(any(), any(), any());
        verify(transactionFlowMapper, never()).insert(any(PaymentTransactionFlowEntity.class));
        verify(notificationService, never()).notifyRefundAfterCommit(any(), any(), any());
    }

    @Test
    @DisplayName("refund should create refunding order without success flow when channel is processing")
    void refund_processingScenario_createsRefundingOrderOnly() {
        when(channelContractMapper.selectActiveConfigValuesJson(1L, 331001L)).thenReturn("{\"mangoPayRefundScenario\":\"PROCESSING\"}");
        when(businessOrderMapper.selectOne(any())).thenReturn(successBusinessOrder(8800L));
        when(refundOrderMapper.selectOpenRefundOrder(any(), any(), any())).thenReturn(null, refundOrder("REFUNDING"));
        String body = refundBody(3300L);
        String timestamp = timestamp();
        String nonce = "nonce-refund-processing";
        ArgumentCaptor<PaymentRefundOrderEntity> refundCaptor = ArgumentCaptor.forClass(PaymentRefundOrderEntity.class);

        PaymentOpenRefundOrderVO result = service.refund(openRequest(
                body, APP_ID, TENANT_ID, timestamp, nonce,
                signature("POST", "/openapi/pay/refunds", body, timestamp, nonce),
                "/openapi/pay/refunds", null, null, null, null)).getData();

        verify(refundOrderMapper).insert(refundCaptor.capture());
        assertThat(refundCaptor.getValue().getStatus()).isEqualTo("REFUNDING");
        assertThat(refundCaptor.getValue().getRefundTime()).isNull();
        assertThat(result.getStatus()).isEqualTo("REFUNDING");
        assertThat(result.getFlowNo()).isNull();
        verify(businessOrderMapper, never()).updateRefundProgress(any(), any(), any());
        verify(transactionFlowMapper, never()).insert(any(PaymentTransactionFlowEntity.class));
        verify(notificationService, never()).notifyRefundAfterCommit(any(), any(), any());
    }

    @Test
    @DisplayName("refund should create failed order without success side effects when channel fails")
    void refund_failedScenario_createsFailedOrderOnly() {
        when(channelContractMapper.selectActiveConfigValuesJson(1L, 331001L)).thenReturn("{\"mangoPayRefundScenario\":\"FAILED\"}");
        when(businessOrderMapper.selectOne(any())).thenReturn(successBusinessOrder(8800L));
        when(refundOrderMapper.selectOpenRefundOrder(any(), any(), any())).thenReturn(null, refundOrder("FAILED"));
        String body = refundBody(3300L);
        String timestamp = timestamp();
        String nonce = "nonce-refund-failed";
        ArgumentCaptor<PaymentRefundOrderEntity> refundCaptor = ArgumentCaptor.forClass(PaymentRefundOrderEntity.class);

        PaymentOpenRefundOrderVO result = service.refund(openRequest(
                body, APP_ID, TENANT_ID, timestamp, nonce,
                signature("POST", "/openapi/pay/refunds", body, timestamp, nonce),
                "/openapi/pay/refunds", null, null, null, null)).getData();

        verify(refundOrderMapper).insert(refundCaptor.capture());
        assertThat(refundCaptor.getValue().getStatus()).isEqualTo("REFUNDING");
        assertThat(refundCaptor.getValue().getRefundTime()).isNull();
        verify(refundOrderMapper).updateRefundApplyResult(eq(1L), any(), eq("MRRO2026060600000001"), eq("FAILED"));
        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getFlowNo()).isNull();
        verify(businessOrderMapper, never()).updateRefundProgress(any(), any(), any());
        verify(transactionFlowMapper, never()).insert(any(PaymentTransactionFlowEntity.class));
        verify(notificationService, never()).notifyRefundAfterCommit(any(), any(), any());
    }

    @Test
    @DisplayName("refund should reject conflicting idempotent refund fields")
    void refund_conflictingIdempotentFields_rejects() {
        PaymentRefundOrderVO existing = refundOrder();
        existing.setRefundAmount(2200L);
        when(refundOrderMapper.selectOpenRefundOrder(any(), any(), any())).thenReturn(existing);
        String body = refundBody(3300L);
        String timestamp = timestamp();
        String nonce = "nonce-refund-conflict";

        assertThatThrownBy(() -> service.refund(openRequest(
                body, APP_ID, TENANT_ID, timestamp, nonce,
                signature("POST", "/openapi/pay/refunds", body, timestamp, nonce),
                "/openapi/pay/refunds", null, null, null, null)))
                .isInstanceOf(BizException.class)
                .hasMessage(PaymentCode.PAYMENT_OPENAPI_IDEMPOTENT_CONFLICT.getMessage());
    }

    @Test
    @DisplayName("refund should reject amount greater than refundable amount")
    void refund_amountExceeded_rejects() {
        when(businessOrderMapper.selectOne(any())).thenReturn(successBusinessOrder(8800L));
        when(refundOrderMapper.sumOccupyingRefundAmount(1L, 370001L)).thenReturn(7000L);
        String body = refundBody(3300L);
        String timestamp = timestamp();
        String nonce = "nonce-refund-exceeded";

        assertThatThrownBy(() -> service.refund(openRequest(
                body, APP_ID, TENANT_ID, timestamp, nonce,
                signature("POST", "/openapi/pay/refunds", body, timestamp, nonce),
                "/openapi/pay/refunds", null, null, null, null)))
                .isInstanceOf(BizException.class)
                .hasMessage(PaymentCode.PAYMENT_REFUND_AMOUNT_EXCEEDED.getMessage());
        verify(refundOrderMapper, never()).insert(any(PaymentRefundOrderEntity.class));
        verify(businessOrderMapper, never()).updateRefundProgress(any(), any(), any());
        verify(transactionFlowMapper, never()).insert(any(PaymentTransactionFlowEntity.class));
    }

    @Test
    @DisplayName("refund should reject when successful payment row lock is not acquired")
    void refund_paymentLockMissing_rejectsBeforeAmountCalculation() {
        when(businessOrderMapper.selectOne(any())).thenReturn(successBusinessOrder(8800L));
        when(paymentOrderMapper.lockSuccessfulOpenPaymentOrder(1L, 370001L)).thenReturn(null);
        String body = refundBody(3300L);
        String timestamp = timestamp();
        String nonce = "nonce-refund-lock-missing";

        assertThatThrownBy(() -> service.refund(openRequest(
                body, APP_ID, TENANT_ID, timestamp, nonce,
                signature("POST", "/openapi/pay/refunds", body, timestamp, nonce),
                "/openapi/pay/refunds", null, null, null, null)))
                .isInstanceOf(BizException.class)
                .hasMessage("原成功支付订单不存在");
        verify(refundOrderMapper, never()).sumOccupyingRefundAmount(any(), any());
        verify(refundOrderMapper, never()).insert(any(PaymentRefundOrderEntity.class));
        verify(transactionFlowMapper, never()).insert(any(PaymentTransactionFlowEntity.class));
    }

    @Test
    @DisplayName("refund should not update business refund progress during apply phase")
    void refund_successScenario_doesNotTouchBusinessRefundProgress() {
        when(businessOrderMapper.selectOne(any())).thenReturn(successBusinessOrder(8800L));
        when(refundOrderMapper.selectOpenRefundOrder(any(), any(), any())).thenReturn(null, refundOrder("REFUNDING"));
        when(businessOrderMapper.updateRefundProgress(1L, 360001L, 3300L)).thenReturn(0);
        String body = refundBody(3300L);
        String timestamp = timestamp();
        String nonce = "nonce-refund-progress-cas";

        PaymentOpenRefundOrderVO result = service.refund(openRequest(
                body, APP_ID, TENANT_ID, timestamp, nonce,
                signature("POST", "/openapi/pay/refunds", body, timestamp, nonce),
                "/openapi/pay/refunds", null, null, null, null)).getData();

        assertThat(result.getStatus()).isEqualTo("REFUNDING");
        verify(refundOrderMapper).insert(any(PaymentRefundOrderEntity.class));
        verify(businessOrderMapper, never()).updateRefundProgress(any(), any(), any());
        verify(transactionFlowMapper, never()).insert(any(PaymentTransactionFlowEntity.class));
        verify(notificationService, never()).notifyRefundAfterCommit(any(), any(), any());
    }

    @Test
    @DisplayName("refund should reject invalid occupied refund amount through money boundary")
    void refund_occupiedAmountGreaterThanPaid_rejects() {
        when(businessOrderMapper.selectOne(any())).thenReturn(successBusinessOrder(8800L));
        when(refundOrderMapper.sumOccupyingRefundAmount(1L, 370001L)).thenReturn(9000L);
        String body = refundBody(100L);
        String timestamp = timestamp();
        String nonce = "nonce-refund-occupied-invalid";

        assertThatThrownBy(() -> service.refund(openRequest(
                body, APP_ID, TENANT_ID, timestamp, nonce,
                signature("POST", "/openapi/pay/refunds", body, timestamp, nonce),
                "/openapi/pay/refunds", null, null, null, null)))
                .isInstanceOf(BizException.class)
                .hasMessage("金额不能小于 0 分");
    }

    @Test
    @DisplayName("detailRefund should query refund order under signed app")
    void detailRefund_validSignature_returnsRefundOrder() {
        when(refundOrderMapper.selectOpenRefundOrder(any(), any(), any())).thenReturn(refundOrder());
        String timestamp = timestamp();
        String nonce = "nonce-refund-detail";

        PaymentOpenRefundOrderVO result = service.detailRefund(openRequest(
                null, APP_ID, TENANT_ID, timestamp, nonce,
                signature("GET", "/openapi/pay/refunds/RF_OPENAPI_001", "", timestamp, nonce),
                "/openapi/pay/refunds/RF_OPENAPI_001", null, null, null, "RF_OPENAPI_001")).getData();

        assertThat(result.getRefundOrderNo()).isEqualTo("RO202606060001");
        assertThat(result.getBizRefundNo()).isEqualTo("RF_OPENAPI_001");
        assertThat(result.getPayOrderNo()).isEqualTo("PO202606060001");
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getFlowNo()).isEqualTo("RFLOW202606060001");
    }

    private PaymentApplication application() {
        PaymentApplication application = new PaymentApplication();
        application.setId(310001L);
        application.setTenantId(1L);
        application.setAppId(APP_ID);
        application.setAppSecret("enc:openapi-secret-ciphertext");
        application.setSecretConfigured(1);
        application.setSignAlgorithm("HMAC_SHA256");
        application.setStatus(1);
        return application;
    }

    private PaymentCashierConfig cashierConfig() {
        PaymentCashierConfig config = new PaymentCashierConfig();
        config.setId(350001L);
        config.setTenantId(1L);
        config.setApplicationId(310001L);
        config.setDefaultCashier(1);
        config.setEnterpriseSubjectIds("320001,320002");
        config.setStatus(1);
        return config;
    }

    private PaymentBusinessOrderEntity businessOrder(Long amount) {
        PaymentBusinessOrderEntity order = new PaymentBusinessOrderEntity();
        order.setId(360001L);
        order.setTenantId(1L);
        order.setAppCode(APP_ID);
        order.setBizOrderNo("BIZ_OPENAPI_001");
        order.setTitle("开放接口订单");
        order.setSubjectId(320001L);
        order.setAmount(amount);
        order.setPaidAmount(0L);
        order.setRefundedAmount(0L);
        order.setCurrency("CNY");
        order.setStatus("PAYING");
        order.setExpireTime(LocalDateTime.now().plusMinutes(30));
        order.setNotifyUrl("https://business.example.test/payment/notify");
        order.setReturnUrl("https://business.example.test/payment/result");
        return order;
    }

    private PaymentBusinessOrderEntity successBusinessOrder(Long amount) {
        PaymentBusinessOrderEntity order = businessOrder(amount);
        order.setPaidAmount(amount);
        order.setStatus("SUCCESS");
        return order;
    }

    private PaymentCashierPayResultVO cashierPayResult() {
        PaymentCashierPayResultVO result = new PaymentCashierPayResultVO();
        result.setPayOrderNo("PO202606060001");
        result.setFlowNo("FLOW202606060001");
        result.setStatus("PAYING");
        result.setMethodCode("PERSONAL_WECHAT_QR");
        result.setMethodName("微信扫码");
        result.setAmount(8800L);
        return result;
    }

    private PaymentOrderVO paymentOrder() {
        return paymentOrder("SUCCESS");
    }

    private PaymentOrderVO paymentOrder(String status) {
        PaymentOrderVO order = new PaymentOrderVO();
        order.setId(370001L);
        order.setPayOrderNo("PO202606060001");
        order.setBusinessOrderId(360001L);
        order.setBizOrderNo("BIZ_OPENAPI_001");
        order.setAppId(APP_ID);
        order.setTitle("开放接口订单");
        order.setAmount(8800L);
        order.setCurrency("CNY");
        order.setStatus(status);
        order.setMethodCode("PERSONAL_WECHAT_QR");
        order.setMethodName("微信扫码");
        order.setChannelCode("MANGO_PAY");
        order.setChannelName("芒果支付");
        order.setChannelMerchantNo("MANGO_PAY_MERCHANT_001");
        order.setContractId(331001L);
        order.setContractCapabilityId(333001L);
        order.setRouteRuleId(334001L);
        order.setChannelTradeNo("CASHIER-PO202606060001");
        if ("SUCCESS".equals(status)) {
            order.setSuccessFlag(1);
            order.setPayTime(LocalDateTime.now());
        } else {
            order.setSuccessFlag(0);
        }
        order.setCreateTime(LocalDateTime.now().minusMinutes(1));
        order.setUpdateTime(LocalDateTime.now());
        return order;
    }

    private PaymentRefundOrderVO refundOrder() {
        return refundOrder("SUCCESS");
    }

    private PaymentRefundOrderVO refundOrder(String status) {
        PaymentRefundOrderVO order = new PaymentRefundOrderVO();
        order.setId(380001L);
        order.setRefundOrderNo("RO202606060001");
        order.setBizRefundNo("RF_OPENAPI_001");
        order.setPaymentOrderId(370001L);
        order.setBusinessOrderId(360001L);
        order.setPayOrderNo("PO202606060001");
        order.setBizOrderNo("BIZ_OPENAPI_001");
        order.setAppId(APP_ID);
        order.setRefundAmount(3300L);
        order.setCurrency("CNY");
        order.setReason("开放接口退款");
        order.setStatus(status);
        order.setMethodCode("PERSONAL_WECHAT_QR");
        order.setChannelCode("MANGO_PAY");
        order.setChannelTradeNo("CASHIER-PO202606060001");
        order.setChannelRefundNo("MRRO202606060001");
        if ("SUCCESS".equals(status)) {
            order.setRefundTime(LocalDateTime.now());
        }
        return order;
    }

    private String createOrderBody(Long amount) {
        return "{"
                + "\"tenantId\":1,"
                + "\"appId\":\"" + APP_ID + "\","
                + "\"bizOrderNo\":\"BIZ_OPENAPI_001\","
                + "\"title\":\"开放接口订单\","
                + "\"amount\":" + amount + ","
                + "\"currency\":\"CNY\","
                + "\"expireMinutes\":30,"
                + "\"notifyUrl\":\"https://business.example.test/payment/notify\","
                + "\"returnUrl\":\"https://business.example.test/payment/result\","
                + "\"extendInfo\":{\"businessRefNo\":\"BIZ_OPENAPI_001\"}"
                + "}";
    }

    private String refundBody(Long amount) {
        return "{"
                + "\"tenantId\":1,"
                + "\"appId\":\"" + APP_ID + "\","
                + "\"bizOrderNo\":\"BIZ_OPENAPI_001\","
                + "\"bizRefundNo\":\"RF_OPENAPI_001\","
                + "\"refundAmount\":" + amount + ","
                + "\"reason\":\"开放接口退款\""
                + "}";
    }

    private String timestamp() {
        return String.valueOf(Instant.now().getEpochSecond());
    }

    private PaymentOpenRequestCommand openRequest(
            String body,
            String appId,
            String tenantId,
            String timestamp,
            String nonce,
            String signature,
            String requestPath,
            String clientIp,
            String bizOrderNo,
            String payOrderNo,
            String bizRefundNo) {
        PaymentOpenRequestCommand command = new PaymentOpenRequestCommand();
        command.setBody(body);
        command.setAppId(appId);
        command.setTenantId(tenantId);
        command.setTimestamp(timestamp);
        command.setNonce(nonce);
        command.setSignature(signature);
        command.setRequestPath(requestPath);
        command.setClientIp(clientIp);
        command.setBizOrderNo(bizOrderNo);
        command.setPayOrderNo(payOrderNo);
        command.setBizRefundNo(bizRefundNo);
        return command;
    }

    private String signature(String method, String path, String body, String timestamp, String nonce) {
        try {
            String canonical = method + "\n" + path + "\n" + sha256Hex(body) + "\n" + timestamp + "\n" + nonce;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(APP_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private String sha256Hex(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder(digest.length * 2);
        for (byte item : digest) {
            builder.append(String.format("%02x", item));
        }
        return builder.toString();
    }

    private static class TestTransactionManager extends AbstractPlatformTransactionManager {

        private final List<Integer> propagationBehaviors = new ArrayList<>();

        private List<Integer> propagationBehaviors() {
            return propagationBehaviors;
        }

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            propagationBehaviors.add(definition.getPropagationBehavior());
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }
}
