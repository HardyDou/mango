package io.mango.payment.core.service;

import io.mango.common.exception.BizException;
import io.mango.common.result.Require;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.command.MangoPayVirtualPaymentCommand;
import io.mango.payment.api.command.PaymentChannelCallbackCommand;
import io.mango.payment.api.enums.PaymentOrderStatusEnum;
import io.mango.payment.api.vo.MangoPayVirtualPaymentResultVO;
import io.mango.payment.api.vo.PaymentChannelCallbackResultVO;
import io.mango.payment.core.entity.PaymentMethod;
import io.mango.payment.core.entity.PaymentOrderEntity;
import io.mango.payment.core.entity.PaymentVirtualChannelPayment;
import io.mango.payment.core.mapper.PaymentChannelContractMapper;
import io.mango.payment.core.mapper.PaymentMethodMapper;
import io.mango.payment.core.mapper.PaymentOrderMapper;
import io.mango.payment.core.mapper.PaymentVirtualChannelPaymentMapper;
import io.mango.payment.core.model.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MangoPayVirtualPaymentService {

    private static final String CHANNEL_CODE = "MANGO_PAY";

    private final PaymentVirtualChannelPaymentMapper virtualPaymentMapper;
    private final PaymentOrderMapper paymentOrderMapper;
    private final PaymentMethodMapper paymentMethodMapper;
    private final PaymentChannelCallbackService callbackService;
    private final PaymentMangoPayScenarioControlService scenarioControlService;
    private final PaymentChannelContractMapper channelContractMapper;
    private final PaymentMangoPayResultMappingService resultMappingService;
    private final PaymentNumberService numberService;

    @Transactional(rollbackFor = Exception.class)
    public MangoPayVirtualPaymentResultVO pay(MangoPayVirtualPaymentCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_CASHIER_PAY_INVALID.getCode(), "芒果支付命令不能为空");
        Require.notNull(command.getCashierConfigId(), PaymentCode.PAYMENT_CASHIER_CONFIG_INVALID.getCode(), "收银台配置 ID 不能为空");
        Require.notBlank(command.getPayOrderNo(), PaymentCode.PAYMENT_ORDER_NOT_FOUND.getCode(), "支付订单号不能为空");
        Require.notBlank(command.getTitle(), PaymentCode.PAYMENT_CASHIER_PAY_INVALID.getCode(), "付款标题不能为空");
        Long tenantId = PaymentContextSupport.currentTenantId();
        LocalDateTime paidTime = LocalDateTime.now();
        String virtualPaymentNo = numberService.next(PaymentNumberService.PAY_MANGO_VIRTUAL_NO);
        long amount = Money.cents(command.getAmount()).toPositiveCents("付款金额");
        PaymentOrderEntity order = paymentOrderMapper.selectByTenantAndPayOrderNo(tenantId, command.getPayOrderNo().trim());
        Require.notNull(order, PaymentCode.PAYMENT_ORDER_NOT_FOUND);
        Require.isTrue(command.getCashierConfigId().equals(order.getCashierConfigId()),
                PaymentCode.PAYMENT_CASHIER_PAY_INVALID.getCode(), "收银台配置与支付订单不匹配");
        Require.isTrue(CHANNEL_CODE.equalsIgnoreCase(PaymentContextSupport.trimToNull(order.getChannelCode())),
                PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "当前支付订单不是芒果支付通道");
        Require.isTrue(PaymentOrderStatusEnum.PAYING.getCode().equals(order.getStatus()),
                PaymentCode.PAYMENT_ORDER_STATE_INVALID.getCode(), "支付订单当前不可付款");
        Require.isTrue(amount == Money.cents(order.getAmount()).toPositiveCents("支付订单金额"),
                PaymentCode.PAYMENT_AMOUNT_INVALID.getCode(), "付款金额与支付订单金额不一致");
        requirePaymentMethodMatched(order, command);
        PaymentVirtualChannelPayment existing = virtualPaymentMapper.selectByTenantAndPayOrderNo(tenantId, order.getPayOrderNo());
        Require.isTrue(existing == null, PaymentCode.PAYMENT_ORDER_STATE_INVALID.getCode(), "芒果支付付款已提交，请查询支付结果");
        PaymentVirtualChannelPayment entity = new PaymentVirtualChannelPayment();
        entity.setVirtualPaymentNo(virtualPaymentNo);
        entity.setPayOrderNo(order.getPayOrderNo());
        entity.setChannelTradeNo(virtualPaymentNo);
        entity.setCashierConfigId(command.getCashierConfigId());
        entity.setPaymentMethodId(order.getMethodId());
        entity.setPaymentMethodCode(PaymentContextSupport.trimToNull(command.getPaymentMethodCode()));
        entity.setTitle(command.getTitle().trim());
        entity.setAmount(amount);
        entity.setPayerName(PaymentContextSupport.trimToNull(command.getPayerName()));
        PaymentMangoPayResultMappingService.PaymentChannelResult channelResult = resolvePaymentResult(tenantId, order);
        entity.setStatus(channelResult.status());
        entity.setPaidTime(paidTime);
        entity.setTenantId(tenantId);
        insertVirtualPayment(entity);
        String resultStatus = channelResult.status();
        if (terminalStatus(resultStatus)) {
            PaymentChannelCallbackResultVO callback = callbackService.handle(callbackCommand(
                    order,
                    virtualPaymentNo,
                    amount,
                    paidTime,
                    channelResult));
            resultStatus = callback.getStatus();
        }
        MangoPayVirtualPaymentResultVO result = new MangoPayVirtualPaymentResultVO();
        result.setVirtualPaymentNo(virtualPaymentNo);
        result.setPayOrderNo(order.getPayOrderNo());
        result.setStatus(resultStatus);
        result.setTitle(entity.getTitle());
        result.setAmount(amount);
        result.setPaidTime(paidTime);
        return result;
    }

    private void insertVirtualPayment(PaymentVirtualChannelPayment entity) {
        try {
            virtualPaymentMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            PaymentVirtualChannelPayment existing = virtualPaymentMapper.selectByTenantAndPayOrderNo(
                    entity.getTenantId(),
                    entity.getPayOrderNo());
            if (existing != null) {
                throw new BizException(
                        PaymentCode.PAYMENT_ORDER_STATE_INVALID.getCode(),
                        "芒果支付付款已提交，请查询支付结果",
                        ex);
            }
            throw new BizException(
                    PaymentCode.PAYMENT_CASHIER_PAY_INVALID.getCode(),
                    "芒果支付付款单号生成冲突，请重试",
                    ex);
        }
    }

    private PaymentMangoPayResultMappingService.PaymentChannelResult resolvePaymentResult(
            Long tenantId,
            PaymentOrderEntity order) {
        Require.notNull(order.getContractId(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "支付通道签约 ID 不能为空");
        PaymentMangoPayResultMappingService.PaymentChannelResult controlled =
                scenarioControlService.consumePaymentScenario(order.getContractId(), "PAYMENT");
        if (controlled != null) {
            return controlled;
        }
        String configValuesJson = channelContractMapper.selectActiveConfigValuesJson(tenantId, order.getContractId());
        Require.notNull(configValuesJson, PaymentCode.PAYMENT_CHANNEL_CONTRACT_NOT_FOUND);
        return resultMappingService.mapPayment(PaymentMangoPayConfigParser.parse(configValuesJson));
    }

    private void requirePaymentMethodMatched(PaymentOrderEntity order, MangoPayVirtualPaymentCommand command) {
        String paymentMethodCode = PaymentContextSupport.trimToNull(command.getPaymentMethodCode());
        if (paymentMethodCode == null) {
            return;
        }
        Require.notNull(order.getMethodId(), PaymentCode.PAYMENT_METHOD_NOT_FOUND);
        PaymentMethod method = paymentMethodMapper.selectById(order.getMethodId());
        Require.notNull(method, PaymentCode.PAYMENT_METHOD_NOT_FOUND);
        Require.isTrue(order.getTenantId().equals(method.getTenantId()), PaymentCode.PAYMENT_METHOD_NOT_FOUND);
        Require.isTrue(paymentMethodCode.equals(method.getMethodCode()),
                PaymentCode.PAYMENT_CASHIER_PAY_INVALID.getCode(), "付款方式与支付订单不匹配");
    }

    private PaymentChannelCallbackCommand callbackCommand(
            PaymentOrderEntity order,
            String virtualPaymentNo,
            long amount,
            LocalDateTime paidTime,
            PaymentMangoPayResultMappingService.PaymentChannelResult channelResult) {
        PaymentChannelCallbackCommand command = new PaymentChannelCallbackCommand();
        command.setCallbackType("PAYMENT");
        command.setChannelCode(CHANNEL_CODE);
        command.setPayOrderNo(order.getPayOrderNo());
        command.setChannelTradeNo(virtualPaymentNo);
        command.setChannelMerchantNo(order.getChannelMerchantNo());
        command.setChannelStatus(channelResult.status());
        command.setAmount(amount);
        command.setEventTime(paidTime);
        command.setChannelReturnCode(channelResult.returnCode());
        command.setChannelMessage("芒果支付内置虚拟通道付款结果：" + channelResult.resultType());
        return command;
    }

    private boolean terminalStatus(String status) {
        return PaymentOrderStatusEnum.SUCCESS.getCode().equals(status)
                || PaymentOrderStatusEnum.FAILED.getCode().equals(status)
                || PaymentOrderStatusEnum.CLOSED.getCode().equals(status);
    }

}
