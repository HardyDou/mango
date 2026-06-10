package io.mango.payment.core.service;

import io.mango.common.exception.BizException;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.vo.PaymentRefundOrderVO;
import io.mango.payment.core.entity.PaymentOrderEntity;
import io.mango.payment.core.entity.PaymentMangoPayScenarioControl;
import io.mango.payment.core.mapper.PaymentChannelContractMapper;
import io.mango.payment.core.mapper.PaymentReconciliationMapper;
import io.mango.payment.core.model.PaymentChannelBillItemRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentMangoPayChannelAdapterTest {

    private PaymentChannelContractMapper channelContractMapper;
    private PaymentReconciliationMapper reconciliationMapper;
    private PaymentMangoPayScenarioControlService scenarioControlService;
    private PaymentMangoPayChannelAdapter adapter;

    @BeforeEach
    void setUp() {
        channelContractMapper = mock(PaymentChannelContractMapper.class);
        reconciliationMapper = mock(PaymentReconciliationMapper.class);
        scenarioControlService = mock(PaymentMangoPayScenarioControlService.class);
        adapter = new PaymentMangoPayChannelAdapter(
                channelContractMapper,
                reconciliationMapper,
                scenarioControlService,
                new PaymentMangoPayResultMappingService());
    }

    @Test
    @DisplayName("generateBill should read real MANGO_PAY bill rows")
    void generateBill_realRows_returnsRows() {
        LocalDate billDate = LocalDate.of(2026, 6, 6);
        when(reconciliationMapper.selectMangoPayBillItems(1L, "MANGO_PAY", billDate, billDate.plusDays(1)))
                .thenReturn(List.of(billRow("CASHIER-PO001", 9900L)));

        IPaymentChannelAdapter.ChannelBillResult result = adapter.generateBill(
                new IPaymentChannelAdapter.ChannelBillCommand(1L, "MANGO_PAY", 331001L, billDate));

        assertThat(result.rows()).hasSize(1);
        assertThat(result.rows().get(0).getChannelTradeNo()).isEqualTo("CASHIER-PO001");
        assertThat(result.rows().get(0).getAmount()).isEqualTo(9900L);
    }

    @Test
    @DisplayName("generateBill should apply MANGO_PAY bill difference control")
    void generateBill_billDifferenceControl_adjustsFirstRow() {
        LocalDate billDate = LocalDate.of(2026, 6, 6);
        when(reconciliationMapper.selectMangoPayBillItems(1L, "MANGO_PAY", billDate, billDate.plusDays(1)))
                .thenReturn(List.of(billRow("CASHIER-PO001", 9900L)));
        PaymentMangoPayScenarioControl scenario = new PaymentMangoPayScenarioControl();
        scenario.setBillDifferenceType("AMOUNT_MINUS");
        scenario.setDifferenceAmount(500L);
        when(scenarioControlService.consumeBillScenario(331001L)).thenReturn(scenario);

        IPaymentChannelAdapter.ChannelBillResult result = adapter.generateBill(
                new IPaymentChannelAdapter.ChannelBillCommand(1L, "MANGO_PAY", 331001L, billDate));

        assertThat(result.rows()).hasSize(1);
        assertThat(result.rows().get(0).getAmount()).isEqualTo(9400L);
    }

    @Test
    @DisplayName("applyPayment should not consume MANGO_PAY payment scenario when generating material")
    void applyPayment_controlledScenario_returnsPayingMaterialWithoutConsumingScenario() {
        when(channelContractMapper.selectActiveConfigValuesJson(1L, 331001L))
                .thenReturn("{\"mangoPayScenario\":\"SUCCESS\"}");
        when(scenarioControlService.consumePaymentScenario(331001L, "PAYMENT"))
                .thenReturn(new PaymentMangoPayResultMappingService.PaymentChannelResult(
                        "FAIL", "MANGO_PAY_PAY_FAILED", "FAIL", "FAILED"));

        IPaymentChannelAdapter.PaymentApplyResult result = adapter.applyPayment(
                paymentApplyCommand("QR"));

        assertThat(result.scenario()).isNull();
        assertThat(result.returnCode()).isEqualTo("PROCESSING");
        assertThat(result.resultType()).isEqualTo("PROCESSING");
        assertThat(result.status()).isEqualTo("PAYING");
        assertThat(result.channelTradeNo()).isEqualTo("MPPO202606060001");
        assertThat(result.material().getQrContent()).isEqualTo("mango-pay:PO202606060001:PERSONAL_WECHAT_QR");
        verify(scenarioControlService, never()).consumePaymentScenario(331001L, "PAYMENT");
    }

    @Test
    @DisplayName("applyPayment should validate MANGO_PAY contract config and return paying material")
    void applyPayment_contractConfig_returnsPayingMaterial() {
        when(channelContractMapper.selectActiveConfigValuesJson(1L, 331001L))
                .thenReturn("{\"mangoPayScenario\":\"SUCCESS\"}");

        IPaymentChannelAdapter.PaymentApplyResult result = adapter.applyPayment(
                paymentApplyCommand("QR"));

        assertThat(result.scenario()).isNull();
        assertThat(result.returnCode()).isEqualTo("PROCESSING");
        assertThat(result.resultType()).isEqualTo("PROCESSING");
        assertThat(result.status()).isEqualTo("PAYING");
        assertThat(result.material().getQrContent()).isEqualTo("mango-pay:PO202606060001:PERSONAL_WECHAT_QR");
        verify(channelContractMapper).selectActiveConfigValuesJson(1L, 331001L);
        verify(scenarioControlService, never()).consumePaymentScenario(331001L, "PAYMENT");
    }

    @Test
    @DisplayName("applyPayment should reject offline transfer material")
    void applyPayment_transferAccount_rejectsOfflineTransfer() {
        assertThatThrownBy(() -> adapter.applyPayment(paymentApplyCommand("TRANSFER_ACCOUNT")))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(PaymentCode.PAYMENT_CHANNEL_INVALID.getCode());
    }

    @Test
    @DisplayName("applyPayment should return html form material from command context")
    void applyPayment_htmlForm_returnsFormMaterial() {
        when(channelContractMapper.selectActiveConfigValuesJson(1L, 331001L))
                .thenReturn("{\"mangoPayScenario\":\"SUCCESS\"}");

        IPaymentChannelAdapter.PaymentApplyResult result = adapter.applyPayment(
                paymentApplyCommand("HTML_FORM"));

        assertThat(result.material().getMaterialType()).isEqualTo("HTML_FORM");
        assertThat(result.material().getHtmlForm()).contains("/payment/mango-pay/virtual/pay");
        assertThat(result.material().getHtmlForm()).contains("payOrderNo");
        assertThat(result.material().getHtmlForm()).contains("PO202606060001");
    }

    @Test
    @DisplayName("applyPayment should return h5 redirect material with existing mango pay endpoint")
    void applyPayment_h5Param_returnsExistingVirtualPayEndpoint() {
        when(channelContractMapper.selectActiveConfigValuesJson(1L, 331001L))
                .thenReturn("{\"mangoPayScenario\":\"SUCCESS\"}");

        IPaymentChannelAdapter.PaymentApplyResult result = adapter.applyPayment(
                paymentApplyCommand("H5_PARAM"));

        assertThat(result.material().getMaterialType()).isEqualTo("H5_PARAM");
        assertThat(result.material().getRedirectUrl()).isEqualTo("/payment/mango-pay/virtual/pay?payOrderNo=PO202606060001");
    }

    @Test
    @DisplayName("applyRefund should map MANGO_PAY refund contract config")
    void applyRefund_contractConfig_returnsMappedResult() {
        when(channelContractMapper.selectActiveConfigValuesJson(1L, 331001L))
                .thenReturn("{\"mangoPayRefundScenario\":\"FAILED\"}");

        IPaymentChannelAdapter.RefundApplyResult result = adapter.applyRefund(
                refundApplyCommand());

        assertThat(result.scenario()).isEqualTo("FAILED");
        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.channelRefundNo()).isEqualTo("MRRO202606060001");
        verify(channelContractMapper).selectActiveConfigValuesJson(1L, 331001L);
    }

    @Test
    @DisplayName("queryPayment should consume controlled MANGO_PAY payment query scenario first")
    void queryPayment_controlledScenario_returnsControlledResult() {
        PaymentOrderEntity order = paymentOrder();
        when(scenarioControlService.consumePaymentScenario(331001L, "PAYMENT_QUERY"))
                .thenReturn(new PaymentMangoPayResultMappingService.PaymentChannelResult(
                        "FAILED", "MANGO_PAY_PAY_FAILED", "FAIL", "FAILED"));

        IPaymentChannelAdapter.PaymentQueryResult result = adapter.queryPayment(
                new IPaymentChannelAdapter.PaymentQueryCommand(1L, order));

        assertThat(result.scenario()).isEqualTo("FAILED");
        assertThat(result.returnCode()).isEqualTo("MANGO_PAY_PAY_FAILED");
        assertThat(result.resultType()).isEqualTo("FAIL");
        assertThat(result.status()).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("queryPayment should map MANGO_PAY contract config when no controlled scenario exists")
    void queryPayment_contractConfig_returnsMappedResult() {
        PaymentOrderEntity order = paymentOrder();
        when(channelContractMapper.selectActiveConfigValuesJson(1L, 331001L))
                .thenReturn("{\"mangoPayScenario\":\"SUCCESS\"}");

        IPaymentChannelAdapter.PaymentQueryResult result = adapter.queryPayment(
                new IPaymentChannelAdapter.PaymentQueryCommand(1L, order));

        assertThat(result.scenario()).isEqualTo("SUCCESS");
        assertThat(result.status()).isEqualTo("SUCCESS");
        verify(channelContractMapper).selectActiveConfigValuesJson(1L, 331001L);
    }

    @Test
    @DisplayName("queryRefund should map MANGO_PAY refund contract config")
    void queryRefund_contractConfig_returnsMappedResult() {
        PaymentRefundOrderVO refundOrder = refundOrder();
        when(channelContractMapper.selectActiveConfigValuesJson(1L, 331001L))
                .thenReturn("{\"mangoPayRefundScenario\":\"PROCESSING\"}");

        IPaymentChannelAdapter.RefundQueryResult result = adapter.queryRefund(
                new IPaymentChannelAdapter.RefundQueryCommand(1L, refundOrder));

        assertThat(result.scenario()).isEqualTo("PROCESSING");
        assertThat(result.status()).isEqualTo("REFUNDING");
        verify(channelContractMapper).selectActiveConfigValuesJson(1L, 331001L);
    }

    @Test
    @DisplayName("queryPayment should reject invalid MANGO_PAY config JSON")
    void queryPayment_invalidJson_rejects() {
        PaymentOrderEntity order = paymentOrder();
        when(channelContractMapper.selectActiveConfigValuesJson(1L, 331001L)).thenReturn("{invalid");

        assertThatThrownBy(() -> adapter.queryPayment(new IPaymentChannelAdapter.PaymentQueryCommand(1L, order)))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(PaymentCode.PAYMENT_CHANNEL_CONTRACT_VALUE_INVALID.getCode());
    }

    private PaymentOrderEntity paymentOrder() {
        PaymentOrderEntity order = new PaymentOrderEntity();
        order.setId(370001L);
        order.setContractId(331001L);
        order.setPayOrderNo("PO202606060001");
        return order;
    }

    private IPaymentChannelAdapter.RefundApplyCommand refundApplyCommand() {
        return new IPaymentChannelAdapter.RefundApplyCommand(
                1L,
                "MANGO_PAY",
                331001L,
                "RO202606060001",
                "RF202606060001",
                "PO202606060001",
                "BIZ202606060001",
                "MPPO202606060001",
                3900L,
                "CNY",
                "用户申请退款");
    }

    private IPaymentChannelAdapter.PaymentApplyCommand paymentApplyCommand(String materialType) {
        return new IPaymentChannelAdapter.PaymentApplyCommand(
                1L,
                "MANGO_PAY",
                331001L,
                "{}",
                "PO202606060001",
                "BO202606060001",
                "PERSONAL_WECHAT_QR",
                "微信扫码",
                materialType,
                9900L,
                "CNY",
                "测试订单",
                LocalDateTime.of(2026, 6, 6, 10, 30),
                320001L,
                "芒果科技有限公司",
                null,
                null,
                null,
                null);
    }

    private PaymentRefundOrderVO refundOrder() {
        PaymentRefundOrderVO order = new PaymentRefundOrderVO();
        order.setId(380001L);
        order.setContractId(331001L);
        order.setRefundOrderNo("RO202606060001");
        return order;
    }

    private PaymentChannelBillItemRow billRow(String channelTradeNo, Long amount) {
        PaymentChannelBillItemRow row = new PaymentChannelBillItemRow();
        row.setChannelTradeNo(channelTradeNo);
        row.setTradeType("PAYMENT");
        row.setAmount(amount);
        row.setFee(0L);
        row.setTradeTime(LocalDateTime.of(2026, 6, 6, 10, 0));
        return row;
    }
}
