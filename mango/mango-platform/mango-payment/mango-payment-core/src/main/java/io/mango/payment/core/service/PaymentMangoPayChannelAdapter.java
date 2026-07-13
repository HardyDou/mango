package io.mango.payment.core.service;

import io.mango.common.result.Require;
import io.mango.payment.api.enums.PaymentCode;
import io.mango.payment.api.enums.PaymentOrderStatusEnum;
import io.mango.payment.api.vo.PaymentCashierPayMaterialVO;
import io.mango.payment.core.entity.PaymentMangoPayScenarioControlEntity;
import io.mango.payment.core.mapper.PaymentChannelContractMapper;
import io.mango.payment.core.mapper.PaymentReconciliationMapper;
import io.mango.payment.core.model.Money;
import io.mango.payment.core.model.PaymentChannelBillItemRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentMangoPayChannelAdapter implements IPaymentChannelAdapter {

    private static final String CHANNEL_CODE = "MANGO_PAY";

    private final PaymentChannelContractMapper channelContractMapper;
    private final PaymentReconciliationMapper reconciliationMapper;
    private final PaymentMangoPayScenarioControlService scenarioControlService;
    private final PaymentMangoPayResultTranslator resultMappingService;

    @Override
    public String channelCode() {
        return CHANNEL_CODE;
    }

    @Override
    public PaymentApplyResult applyPayment(PaymentApplyInput command) {
        Require.notNull(command, PaymentCode.PAYMENT_CASHIER_PAY_INVALID, "支付命令不能为空");
        Require.notNull(command.contractId(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID, "支付通道签约 ID 不能为空");
        Require.notBlank(command.payOrderNo(), PaymentCode.PAYMENT_CASHIER_PAY_INVALID, "支付订单号不能为空");
        Require.notBlank(command.paymentMaterialType(), PaymentCode.PAYMENT_CASHIER_PAY_INVALID, "支付物料类型不能为空");
        Require.isTrue(!"CORPORATE_OFFLINE_ACCOUNT".equals(command.methodCode())
                        && !"TRANSFER_ACCOUNT".equals(command.paymentMaterialType()),
                PaymentCode.PAYMENT_CHANNEL_INVALID, "线下转账必须路由到线下收款通道");
        String configValuesJson = channelContractMapper.selectActiveConfigValuesJson(command.tenantId(), command.contractId());
        Require.notNull(configValuesJson, PaymentCode.PAYMENT_CHANNEL_CONTRACT_NOT_FOUND);
        PaymentMangoPayConfigParser.parse(configValuesJson);
        return new PaymentApplyResult(
                null,
                "PROCESSING",
                "PROCESSING",
                PaymentOrderStatusEnum.PAYING.getCode(),
                mangoPayChannelTradeNo(command),
                material(command));
    }

    @Override
    public RefundApplyResult applyRefund(RefundApplyInput command) {
        Require.notNull(command, PaymentCode.PAYMENT_REFUND_ORDER_INVALID, "退款命令不能为空");
        Require.notNull(command.contractId(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID, "支付通道签约 ID 不能为空");
        Require.notBlank(command.refundOrderNo(), PaymentCode.PAYMENT_REFUND_ORDER_INVALID, "退款订单号不能为空");
        Require.notBlank(command.payOrderNo(), PaymentCode.PAYMENT_ORDER_NOT_FOUND, "原支付订单号不能为空");
        Require.notBlank(command.channelTradeNo(), PaymentCode.PAYMENT_ORDER_NOT_FOUND, "原通道交易号不能为空");
        Require.notNull(command.amount(), PaymentCode.PAYMENT_REFUND_ORDER_INVALID, "退款金额不能为空");
        PaymentMangoPayResultTranslator.RefundChannelResult controlled =
                scenarioControlService.consumeRefundScenario(command.contractId(), "REFUND");
        if (controlled != null) {
            return new RefundApplyResult(
                    controlled.scenario(),
                    controlled.returnCode(),
                    controlled.resultType(),
                    controlled.status(),
                    mangoPayChannelRefundNo(command));
        }
        String configValuesJson = channelContractMapper.selectActiveConfigValuesJson(command.tenantId(), command.contractId());
        Require.notNull(configValuesJson, PaymentCode.PAYMENT_CHANNEL_CONTRACT_NOT_FOUND);
        PaymentMangoPayResultTranslator.RefundChannelResult mapped =
                resultMappingService.mapRefund(PaymentMangoPayConfigParser.parse(configValuesJson));
        return new RefundApplyResult(
                mapped.scenario(),
                mapped.returnCode(),
                mapped.resultType(),
                mapped.status(),
                mangoPayChannelRefundNo(command));
    }

    @Override
    public ChannelBillResult generateBill(ChannelBillInput command) {
        Require.notNull(command, PaymentCode.PAYMENT_RECONCILIATION_INVALID, "通道账单生成命令不能为空");
        Require.notNull(command.billDate(), PaymentCode.PAYMENT_RECONCILIATION_INVALID, "账单日期不能为空");
        List<PaymentChannelBillItemRow> rows = reconciliationMapper.selectMangoPayBillItems(
                command.tenantId(),
                CHANNEL_CODE,
                command.billDate(),
                command.billDate().plusDays(1));
        Require.notEmpty(rows, PaymentCode.PAYMENT_RECONCILIATION_INVALID, "账单日期内没有可生成的芒果支付成功支付或退款订单");
        PaymentMangoPayScenarioControlEntity billScenario = scenarioControlService.consumeBillScenario(command.contractId());
        if (billScenario != null) {
            rows = applyBillDifference(rows, billScenario);
        }
        return new ChannelBillResult(rows);
    }

    @Override
    public PaymentQueryResult queryPayment(PaymentQueryInput command) {
        Require.notNull(command, PaymentCode.PAYMENT_ORDER_STATE_INVALID, "支付查单命令不能为空");
        Require.notNull(command.order(), PaymentCode.PAYMENT_ORDER_NOT_FOUND);
        Require.notNull(command.order().getContractId(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID, "支付通道签约 ID 不能为空");
        PaymentMangoPayResultTranslator.PaymentChannelResult controlled =
                scenarioControlService.consumePaymentScenario(command.order().getContractId(), "PAYMENT_QUERY");
        if (controlled != null) {
            return new PaymentQueryResult(controlled.scenario(), controlled.returnCode(), controlled.resultType(), controlled.status());
        }
        String configValuesJson = channelContractMapper.selectActiveConfigValuesJson(command.tenantId(), command.order().getContractId());
        Require.notNull(configValuesJson, PaymentCode.PAYMENT_CHANNEL_CONTRACT_NOT_FOUND);
        PaymentMangoPayResultTranslator.PaymentChannelResult mapped =
                resultMappingService.mapPayment(PaymentMangoPayConfigParser.parse(configValuesJson));
        return new PaymentQueryResult(mapped.scenario(), mapped.returnCode(), mapped.resultType(), mapped.status());
    }

    @Override
    public RefundQueryResult queryRefund(RefundQueryInput command) {
        Require.notNull(command, PaymentCode.PAYMENT_REFUND_ORDER_INVALID, "退款查单命令不能为空");
        Require.notNull(command.refundOrder(), PaymentCode.PAYMENT_REFUND_ORDER_NOT_FOUND);
        Require.notNull(command.refundOrder().getContractId(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID, "支付通道签约 ID 不能为空");
        PaymentMangoPayResultTranslator.RefundChannelResult controlled =
                scenarioControlService.consumeRefundScenario(command.refundOrder().getContractId(), "REFUND_QUERY");
        if (controlled != null) {
            return new RefundQueryResult(controlled.scenario(), controlled.returnCode(), controlled.resultType(), controlled.status());
        }
        String configValuesJson = channelContractMapper.selectActiveConfigValuesJson(command.tenantId(), command.refundOrder().getContractId());
        Require.notNull(configValuesJson, PaymentCode.PAYMENT_CHANNEL_CONTRACT_NOT_FOUND);
        PaymentMangoPayResultTranslator.RefundChannelResult mapped =
                resultMappingService.mapRefund(PaymentMangoPayConfigParser.parse(configValuesJson));
        return new RefundQueryResult(mapped.scenario(), mapped.returnCode(), mapped.resultType(), mapped.status());
    }

    private String mangoPayChannelTradeNo(PaymentApplyInput command) {
        return "MP" + command.payOrderNo();
    }

    private String mangoPayChannelRefundNo(RefundApplyInput command) {
        return "MR" + command.refundOrderNo();
    }

    private PaymentCashierPayMaterialVO material(PaymentApplyInput command) {
        PaymentCashierPayMaterialVO material = new PaymentCashierPayMaterialVO();
        material.setMaterialType(command.paymentMaterialType());
        if ("HTML_FORM".equals(command.paymentMaterialType())) {
            material.setHtmlForm("<form method=\"post\" action=\"/payment/mango-pay/virtual/pay\"><input name=\"payOrderNo\" value=\""
                    + command.payOrderNo()
                    + "\"></form>");
            return material;
        }
        if ("H5_PARAM".equals(command.paymentMaterialType())) {
            material.setRedirectUrl("/payment/mango-pay/virtual/pay?payOrderNo=" + command.payOrderNo());
            return material;
        }
        material.setQrContent("mango-pay:" + command.payOrderNo() + ":" + command.methodCode());
        return material;
    }

    private List<PaymentChannelBillItemRow> applyBillDifference(
            List<PaymentChannelBillItemRow> rows,
            PaymentMangoPayScenarioControlEntity scenario) {
        List<PaymentChannelBillItemRow> adjusted = new ArrayList<>(rows);
        PaymentChannelBillItemRow first = adjusted.get(0);
        PaymentChannelBillItemRow changed = new PaymentChannelBillItemRow();
        changed.setChannelTradeNo(first.getChannelTradeNo());
        changed.setTradeType(first.getTradeType());
        changed.setFee(first.getFee());
        changed.setTradeTime(first.getTradeTime());
        long differenceAmount = scenario.getDifferenceAmount() == null ? 0L : scenario.getDifferenceAmount();
        long amount = first.getAmount() == null ? 0L : first.getAmount();
        if ("AMOUNT_MINUS".equals(scenario.getBillDifferenceType())) {
            Money adjustedAmount = Money.cents(amount).subtract(Money.cents(differenceAmount));
            changed.setAmount(adjustedAmount.isNegative() ? 0L : adjustedAmount.toNonNegativeCents());
        } else {
            changed.setAmount(Money.cents(amount).add(Money.cents(differenceAmount)).toNonNegativeCents());
        }
        adjusted.set(0, changed);
        return adjusted;
    }
}
