package io.mango.payment.core.service;

import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.command.MangoPayVirtualPaymentCommand;
import io.mango.payment.api.command.PaymentChannelCallbackCommand;
import io.mango.payment.api.vo.MangoPayVirtualPaymentResultVO;
import io.mango.payment.api.vo.PaymentChannelCallbackResultVO;
import io.mango.payment.core.entity.PaymentMethod;
import io.mango.payment.core.entity.PaymentOrderEntity;
import io.mango.payment.core.entity.PaymentVirtualChannelPayment;
import io.mango.payment.core.mapper.PaymentChannelContractMapper;
import io.mango.payment.core.mapper.PaymentMethodMapper;
import io.mango.payment.core.mapper.PaymentOrderMapper;
import io.mango.payment.core.mapper.PaymentVirtualChannelPaymentMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MangoPayVirtualPaymentServiceTest {

    private PaymentVirtualChannelPaymentMapper virtualPaymentMapper;
    private PaymentOrderMapper paymentOrderMapper;
    private PaymentMethodMapper paymentMethodMapper;
    private PaymentChannelCallbackService callbackService;
    private PaymentMangoPayScenarioControlService scenarioControlService;
    private PaymentChannelContractMapper channelContractMapper;
    private PaymentNumberService numberService;
    private MangoPayVirtualPaymentService service;

    @BeforeEach
    void setUp() {
        virtualPaymentMapper = mock(PaymentVirtualChannelPaymentMapper.class);
        paymentOrderMapper = mock(PaymentOrderMapper.class);
        paymentMethodMapper = mock(PaymentMethodMapper.class);
        callbackService = mock(PaymentChannelCallbackService.class);
        scenarioControlService = mock(PaymentMangoPayScenarioControlService.class);
        channelContractMapper = mock(PaymentChannelContractMapper.class);
        numberService = mock(PaymentNumberService.class);
        when(numberService.next(PaymentNumberService.PAY_MANGO_VIRTUAL_NO)).thenReturn("MP2026060600000001");
        service = new MangoPayVirtualPaymentService(
                virtualPaymentMapper,
                paymentOrderMapper,
                paymentMethodMapper,
                callbackService,
                scenarioControlService,
                channelContractMapper,
                new PaymentMangoPayResultMappingService(),
                numberService);
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1001L, "1", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L, "internal-admin"));
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    @DisplayName("pay should persist mango pay virtual payment through mapper")
    void pay_validCommand_insertsEntityAndReturnsResult() {
        MangoPayVirtualPaymentCommand command = new MangoPayVirtualPaymentCommand();
        command.setCashierConfigId(350001L);
        command.setTitle("订单支付");
        command.setAmount(9900L);
        command.setPaymentMethodCode("PERSONAL_WECHAT_QR");
        command.setPayerName("张三");
        ArgumentCaptor<PaymentVirtualChannelPayment> captor = ArgumentCaptor.forClass(PaymentVirtualChannelPayment.class);
        ArgumentCaptor<PaymentChannelCallbackCommand> callbackCaptor = ArgumentCaptor.forClass(PaymentChannelCallbackCommand.class);
        when(paymentOrderMapper.selectByTenantAndPayOrderNo(1L, "PO202606060001")).thenReturn(paymentOrder());
        when(paymentMethodMapper.selectById(340001L)).thenReturn(paymentMethod("PERSONAL_WECHAT_QR"));
        when(virtualPaymentMapper.selectByTenantAndPayOrderNo(1L, "PO202606060001")).thenReturn(null);
        when(channelContractMapper.selectActiveConfigValuesJson(1L, 331001L)).thenReturn("{\"mangoPayScenario\":\"SUCCESS\"}");
        PaymentChannelCallbackResultVO callbackResult = new PaymentChannelCallbackResultVO();
        callbackResult.setStatus("SUCCESS");
        when(callbackService.handle(any(PaymentChannelCallbackCommand.class))).thenReturn(callbackResult);
        command.setPayOrderNo("PO202606060001");

        MangoPayVirtualPaymentResultVO result = service.pay(command);

        verify(virtualPaymentMapper).insert(captor.capture());
        PaymentVirtualChannelPayment entity = captor.getValue();
        assertThat(entity.getTenantId()).isEqualTo(1L);
        assertThat(entity.getPayOrderNo()).isEqualTo("PO202606060001");
        assertThat(entity.getChannelTradeNo()).startsWith("MP");
        assertThat(entity.getCashierConfigId()).isEqualTo(350001L);
        assertThat(entity.getPaymentMethodId()).isEqualTo(340001L);
        assertThat(entity.getPaymentMethodCode()).isEqualTo("PERSONAL_WECHAT_QR");
        assertThat(entity.getAmount()).isEqualTo(9900L);
        assertThat(entity.getStatus()).isEqualTo("SUCCESS");
        verify(callbackService).handle(callbackCaptor.capture());
        PaymentChannelCallbackCommand callback = callbackCaptor.getValue();
        assertThat(callback.getCallbackType()).isEqualTo("PAYMENT");
        assertThat(callback.getChannelCode()).isEqualTo("MANGO_PAY");
        assertThat(callback.getPayOrderNo()).isEqualTo("PO202606060001");
        assertThat(callback.getChannelTradeNo()).isEqualTo(entity.getChannelTradeNo());
        assertThat(callback.getChannelMerchantNo()).isEqualTo("MANGO_PAY_MERCHANT_001");
        assertThat(callback.getChannelStatus()).isEqualTo("SUCCESS");
        assertThat(callback.getChannelReturnCode()).isEqualTo("SUCCESS");
        assertThat(callback.getAmount()).isEqualTo(9900L);
        assertThat(result.getVirtualPaymentNo()).startsWith("MP");
        assertThat(result.getPayOrderNo()).isEqualTo("PO202606060001");
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getAmount()).isEqualTo(9900L);
    }

    @Test
    @DisplayName("pay should keep paying without callback when mango pay config returns processing")
    void pay_processingConfig_keepsPayingWithoutCallback() {
        MangoPayVirtualPaymentCommand command = command();
        ArgumentCaptor<PaymentVirtualChannelPayment> captor = ArgumentCaptor.forClass(PaymentVirtualChannelPayment.class);
        when(paymentOrderMapper.selectByTenantAndPayOrderNo(1L, "PO202606060001")).thenReturn(paymentOrder());
        when(paymentMethodMapper.selectById(340001L)).thenReturn(paymentMethod("PERSONAL_WECHAT_QR"));
        when(virtualPaymentMapper.selectByTenantAndPayOrderNo(1L, "PO202606060001")).thenReturn(null);
        when(channelContractMapper.selectActiveConfigValuesJson(1L, 331001L)).thenReturn("{\"mangoPayScenario\":\"PROCESSING\"}");

        MangoPayVirtualPaymentResultVO result = service.pay(command);

        verify(virtualPaymentMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("PAYING");
        assertThat(result.getStatus()).isEqualTo("PAYING");
        verify(callbackService, never()).handle(any(PaymentChannelCallbackCommand.class));
    }

    @Test
    @DisplayName("pay should call standard callback with failed status when mango pay config returns failed")
    void pay_failedConfig_callsFailedCallback() {
        MangoPayVirtualPaymentCommand command = command();
        ArgumentCaptor<PaymentChannelCallbackCommand> callbackCaptor = ArgumentCaptor.forClass(PaymentChannelCallbackCommand.class);
        when(paymentOrderMapper.selectByTenantAndPayOrderNo(1L, "PO202606060001")).thenReturn(paymentOrder());
        when(paymentMethodMapper.selectById(340001L)).thenReturn(paymentMethod("PERSONAL_WECHAT_QR"));
        when(virtualPaymentMapper.selectByTenantAndPayOrderNo(1L, "PO202606060001")).thenReturn(null);
        when(channelContractMapper.selectActiveConfigValuesJson(1L, 331001L)).thenReturn("{\"mangoPayScenario\":\"FAILED\"}");
        PaymentChannelCallbackResultVO callbackResult = new PaymentChannelCallbackResultVO();
        callbackResult.setStatus("FAILED");
        when(callbackService.handle(any(PaymentChannelCallbackCommand.class))).thenReturn(callbackResult);

        MangoPayVirtualPaymentResultVO result = service.pay(command);

        verify(callbackService).handle(callbackCaptor.capture());
        PaymentChannelCallbackCommand callback = callbackCaptor.getValue();
        assertThat(callback.getChannelStatus()).isEqualTo("FAILED");
        assertThat(callback.getChannelReturnCode()).isEqualTo("FAILED");
        assertThat(result.getStatus()).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("pay should consume controlled MANGO_PAY payment scenario during payer action")
    void pay_controlledPaymentScenario_consumesScenarioAndCallsCallback() {
        MangoPayVirtualPaymentCommand command = command();
        ArgumentCaptor<PaymentChannelCallbackCommand> callbackCaptor = ArgumentCaptor.forClass(PaymentChannelCallbackCommand.class);
        when(paymentOrderMapper.selectByTenantAndPayOrderNo(1L, "PO202606060001")).thenReturn(paymentOrder());
        when(paymentMethodMapper.selectById(340001L)).thenReturn(paymentMethod("PERSONAL_WECHAT_QR"));
        when(virtualPaymentMapper.selectByTenantAndPayOrderNo(1L, "PO202606060001")).thenReturn(null);
        when(scenarioControlService.consumePaymentScenario(331001L, "PAYMENT"))
                .thenReturn(new PaymentMangoPayResultMappingService.PaymentChannelResult(
                        "FAIL", "MANGO_PAY_PAY_FAILED", "FAIL", "FAILED"));
        PaymentChannelCallbackResultVO callbackResult = new PaymentChannelCallbackResultVO();
        callbackResult.setStatus("FAILED");
        when(callbackService.handle(any(PaymentChannelCallbackCommand.class))).thenReturn(callbackResult);

        MangoPayVirtualPaymentResultVO result = service.pay(command);

        verify(scenarioControlService).consumePaymentScenario(331001L, "PAYMENT");
        verify(channelContractMapper, never()).selectActiveConfigValuesJson(any(), any());
        verify(callbackService).handle(callbackCaptor.capture());
        PaymentChannelCallbackCommand callback = callbackCaptor.getValue();
        assertThat(callback.getChannelStatus()).isEqualTo("FAILED");
        assertThat(callback.getChannelReturnCode()).isEqualTo("MANGO_PAY_PAY_FAILED");
        assertThat(result.getStatus()).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("pay should call standard callback with closed status when mango pay config returns closed")
    void pay_closedConfig_callsClosedCallback() {
        MangoPayVirtualPaymentCommand command = command();
        ArgumentCaptor<PaymentChannelCallbackCommand> callbackCaptor = ArgumentCaptor.forClass(PaymentChannelCallbackCommand.class);
        when(paymentOrderMapper.selectByTenantAndPayOrderNo(1L, "PO202606060001")).thenReturn(paymentOrder());
        when(paymentMethodMapper.selectById(340001L)).thenReturn(paymentMethod("PERSONAL_WECHAT_QR"));
        when(virtualPaymentMapper.selectByTenantAndPayOrderNo(1L, "PO202606060001")).thenReturn(null);
        when(channelContractMapper.selectActiveConfigValuesJson(1L, 331001L)).thenReturn("{\"mangoPayScenario\":\"CLOSED\"}");
        PaymentChannelCallbackResultVO callbackResult = new PaymentChannelCallbackResultVO();
        callbackResult.setStatus("CLOSED");
        when(callbackService.handle(any(PaymentChannelCallbackCommand.class))).thenReturn(callbackResult);

        MangoPayVirtualPaymentResultVO result = service.pay(command);

        verify(callbackService).handle(callbackCaptor.capture());
        PaymentChannelCallbackCommand callback = callbackCaptor.getValue();
        assertThat(callback.getChannelStatus()).isEqualTo("CLOSED");
        assertThat(callback.getChannelReturnCode()).isEqualTo("CLOSED");
        assertThat(result.getStatus()).isEqualTo("CLOSED");
    }

    @Test
    @DisplayName("pay should reject duplicate mango pay submission for same payment order")
    void pay_duplicateVirtualPayment_rejectsWithoutCallback() {
        MangoPayVirtualPaymentCommand command = command();
        when(paymentOrderMapper.selectByTenantAndPayOrderNo(1L, "PO202606060001")).thenReturn(paymentOrder());
        when(paymentMethodMapper.selectById(340001L)).thenReturn(paymentMethod("PERSONAL_WECHAT_QR"));
        PaymentVirtualChannelPayment existing = new PaymentVirtualChannelPayment();
        existing.setPayOrderNo("PO202606060001");
        existing.setChannelTradeNo("MP202606060001000001");
        when(virtualPaymentMapper.selectByTenantAndPayOrderNo(1L, "PO202606060001")).thenReturn(existing);

        assertThatThrownBy(() -> service.pay(command))
                .extracting("code")
                .isEqualTo(PaymentCode.PAYMENT_ORDER_STATE_INVALID.getCode());
        verify(virtualPaymentMapper, never()).insert(any(PaymentVirtualChannelPayment.class));
        verify(callbackService, never()).handle(any(PaymentChannelCallbackCommand.class));
    }

    @Test
    @DisplayName("pay should reject payment method code inconsistent with payment order")
    void pay_methodCodeMismatch_rejectsWithoutInsert() {
        MangoPayVirtualPaymentCommand command = command();
        command.setPaymentMethodCode("PERSONAL_ALIPAY_QR");
        when(paymentOrderMapper.selectByTenantAndPayOrderNo(1L, "PO202606060001")).thenReturn(paymentOrder());
        when(paymentMethodMapper.selectById(340001L)).thenReturn(paymentMethod("PERSONAL_WECHAT_QR"));

        assertThatThrownBy(() -> service.pay(command))
                .extracting("code")
                .isEqualTo(PaymentCode.PAYMENT_CASHIER_PAY_INVALID.getCode());
        verify(virtualPaymentMapper, never()).insert(any(PaymentVirtualChannelPayment.class));
        verify(callbackService, never()).handle(any(PaymentChannelCallbackCommand.class));
    }

    @Test
    @DisplayName("pay should convert concurrent duplicate virtual payment insert to business error")
    void pay_concurrentDuplicateInsert_rejectsWithoutCallback() {
        MangoPayVirtualPaymentCommand command = command();
        when(paymentOrderMapper.selectByTenantAndPayOrderNo(1L, "PO202606060001")).thenReturn(paymentOrder());
        when(paymentMethodMapper.selectById(340001L)).thenReturn(paymentMethod("PERSONAL_WECHAT_QR"));
        when(virtualPaymentMapper.selectByTenantAndPayOrderNo(1L, "PO202606060001"))
                .thenReturn(null)
                .thenReturn(existingVirtualPayment());
        when(channelContractMapper.selectActiveConfigValuesJson(1L, 331001L)).thenReturn("{\"mangoPayScenario\":\"SUCCESS\"}");
        when(virtualPaymentMapper.insert(any(PaymentVirtualChannelPayment.class)))
                .thenThrow(new DuplicateKeyException("duplicate virtual payment pay order"));

        assertThatThrownBy(() -> service.pay(command))
                .extracting("code")
                .isEqualTo(PaymentCode.PAYMENT_ORDER_STATE_INVALID.getCode());
        verify(callbackService, never()).handle(any(PaymentChannelCallbackCommand.class));
    }

    private MangoPayVirtualPaymentCommand command() {
        MangoPayVirtualPaymentCommand command = new MangoPayVirtualPaymentCommand();
        command.setCashierConfigId(350001L);
        command.setPayOrderNo("PO202606060001");
        command.setTitle("订单支付");
        command.setAmount(9900L);
        command.setPaymentMethodCode("PERSONAL_WECHAT_QR");
        command.setPayerName("张三");
        return command;
    }

    private PaymentOrderEntity paymentOrder() {
        PaymentOrderEntity order = new PaymentOrderEntity();
        order.setId(370001L);
        order.setPayOrderNo("PO202606060001");
        order.setContractId(331001L);
        order.setTenantId(1L);
        order.setCashierConfigId(350001L);
        order.setChannelCode("MANGO_PAY");
        order.setChannelMerchantNo("MANGO_PAY_MERCHANT_001");
        order.setMethodId(340001L);
        order.setAmount(9900L);
        order.setStatus("PAYING");
        return order;
    }

    private PaymentMethod paymentMethod(String methodCode) {
        PaymentMethod method = new PaymentMethod();
        method.setId(340001L);
        method.setTenantId(1L);
        method.setMethodCode(methodCode);
        return method;
    }

    private PaymentVirtualChannelPayment existingVirtualPayment() {
        PaymentVirtualChannelPayment payment = new PaymentVirtualChannelPayment();
        payment.setTenantId(1L);
        payment.setPayOrderNo("PO202606060001");
        payment.setChannelTradeNo("MP202606060001000001");
        return payment;
    }
}
