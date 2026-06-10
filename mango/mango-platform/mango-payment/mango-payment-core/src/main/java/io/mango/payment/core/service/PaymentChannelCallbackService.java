package io.mango.payment.core.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.common.exception.BizException;
import io.mango.common.result.Require;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.command.PaymentChannelCallbackCommand;
import io.mango.payment.api.enums.PaymentBusinessOrderStatusEnum;
import io.mango.payment.api.enums.PaymentOrderStatusEnum;
import io.mango.payment.api.enums.PaymentRefundOrderStatusEnum;
import io.mango.payment.api.vo.PaymentChannelCallbackResultVO;
import io.mango.payment.api.vo.PaymentOrderVO;
import io.mango.payment.api.vo.PaymentRefundOrderVO;
import io.mango.payment.core.entity.PaymentApplication;
import io.mango.payment.core.entity.PaymentBusinessOrderEntity;
import io.mango.payment.core.entity.PaymentOrderEntity;
import io.mango.payment.core.entity.PaymentTransactionFlowEntity;
import io.mango.payment.core.mapper.PaymentApplicationMapper;
import io.mango.payment.core.mapper.PaymentBusinessOrderMapper;
import io.mango.payment.core.mapper.PaymentOrderMapper;
import io.mango.payment.core.mapper.PaymentRefundOrderMapper;
import io.mango.payment.core.mapper.PaymentTransactionFlowMapper;
import io.mango.payment.core.model.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PaymentChannelCallbackService {

    private final PaymentOrderMapper paymentOrderMapper;
    private final PaymentRefundOrderMapper refundOrderMapper;
    private final PaymentBusinessOrderMapper businessOrderMapper;
    private final PaymentApplicationMapper applicationMapper;
    private final PaymentTransactionFlowMapper transactionFlowMapper;
    private final PaymentOrderStateService orderStateService;
    private final PaymentNotificationService notificationService;
    private final PaymentOrderStatusFlowService statusFlowService;
    private final PaymentDuplicatePaymentService duplicatePaymentService;
    private final PaymentDuplicateRefundCompletionService duplicateRefundCompletionService;
    private final PaymentObservabilityService observabilityService;
    private final PaymentExceptionOrderService exceptionOrderService;
    private final PaymentNumberService numberService;

    @Transactional(rollbackFor = Exception.class)
    public PaymentChannelCallbackResultVO handle(PaymentChannelCallbackCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_ORDER_STATE_INVALID.getCode(), "通道回调命令不能为空");
        String callbackType = normalize(command.getCallbackType());
        if ("PAYMENT".equals(callbackType)) {
            return handlePayment(command);
        }
        if ("REFUND".equals(callbackType)) {
            return handleRefund(command);
        }
        throw new BizException(PaymentCode.PAYMENT_ORDER_STATE_INVALID.getCode(), "通道回调类型不支持");
    }

    private PaymentChannelCallbackResultVO handlePayment(PaymentChannelCallbackCommand command) {
        long startedAt = System.nanoTime();
        Long tenantId = PaymentContextSupport.currentTenantId();
        String channelCode = normalize(command.getChannelCode());
        PaymentOrderEntity order = selectPaymentOrder(tenantId, command, channelCode);
        Require.isTrue(channelCode.equals(normalize(order.getChannelCode())),
                PaymentCode.PAYMENT_ORDER_NOT_FOUND.getCode(), "支付订单通道不匹配");
        Require.isTrue(sameMerchant(command.getChannelMerchantNo(), order.getChannelMerchantNo()),
                PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "通道商户号不匹配");
        long callbackAmount = Money.cents(command.getAmount()).toPositiveCents("通道回调金额");
        Require.isTrue(callbackAmount == Money.cents(order.getAmount()).toPositiveCents("支付订单金额"),
                PaymentCode.PAYMENT_AMOUNT_INVALID.getCode(), "通道回调金额与支付订单金额不一致");

        String currentStatus = order.getStatus();
        String targetStatus = paymentTargetStatus(command.getChannelStatus());
        if (isPaymentTerminal(currentStatus)) {
            return result(false, order.getPayOrderNo(), currentStatus, paymentOrderMapper.selectLatestFlowNo(tenantId, order.getId()),
                    "本地支付订单已是终态，回调幂等返回");
        }
        orderStateService.requirePaymentTransition(currentStatus, targetStatus);
        LocalDateTime eventTime = command.getEventTime() == null ? LocalDateTime.now() : command.getEventTime();
        String channelTradeNo = PaymentContextSupport.trimToNull(command.getChannelTradeNo());
        int updated = updatePaymentCallbackResult(tenantId, order, targetStatus, eventTime, channelTradeNo);
        if (updated == -1) {
            PaymentDuplicatePaymentService.DuplicatePaymentResult duplicateResult = duplicatePaymentService.handleDuplicateSuccess(
                    tenantId,
                    order,
                    eventTime,
                    PaymentOrderStatusFlowService.SOURCE_CHANNEL_CALLBACK,
                    firstText(channelTradeNo, order.getPayOrderNo()),
                    channelTradeNo);
            String message = duplicateResult.exceptionNo() == null ? "重复成功支付已发起自动退款，等待通道结果" : "重复成功支付已挂起异常处理";
            return result(true, order.getPayOrderNo(), duplicateResult.status(),
                    paymentOrderMapper.selectLatestFlowNo(tenantId, order.getId()), message);
        }
        Require.isTrue(updated == 1, PaymentCode.PAYMENT_ORDER_STATE_INVALID.getCode(), "支付订单状态已变化，请刷新后重试");
        statusFlowService.record(
                tenantId,
                PaymentOrderStatusFlowService.ORDER_TYPE_PAYMENT,
                order.getId(),
                order.getPayOrderNo(),
                currentStatus,
                targetStatus,
                PaymentOrderStatusFlowService.SOURCE_CHANNEL_CALLBACK,
                firstText(channelTradeNo, order.getPayOrderNo()),
                eventTime,
                "通道支付回调推进支付订单状态");

        String flowNo = null;
        if (PaymentOrderStatusEnum.SUCCESS.getCode().equals(targetStatus)) {
            flowNo = numberService.next(PaymentNumberService.PAY_FLOW_NO);
            insertFlow(flowNo, order.getBusinessOrderId(), order.getId(), null, "PAY_SUCCESS", order.getAmount(), tenantId);
            int businessUpdated = businessOrderMapper.markCashierPaySuccess(tenantId, order.getBusinessOrderId(), order.getAmount());
            Require.isTrue(businessUpdated == 1, PaymentCode.PAYMENT_BUSINESS_ORDER_STATE_CHANGED);
            statusFlowService.record(
                    tenantId,
                    PaymentOrderStatusFlowService.ORDER_TYPE_BUSINESS,
                    order.getBusinessOrderId(),
                    selectBusinessOrderNo(tenantId, order.getBusinessOrderId()),
                    PaymentBusinessOrderStatusEnum.PAYING.getCode(),
                    PaymentBusinessOrderStatusEnum.PAID.getCode(),
                    PaymentOrderStatusFlowService.SOURCE_CHANNEL_CALLBACK,
                    firstText(channelTradeNo, order.getPayOrderNo()),
                    eventTime,
                    "通道支付回调确认支付成功");
        } else if (PaymentOrderStatusEnum.FAILED.getCode().equals(targetStatus)) {
            exceptionOrderService.createIfAbsent(
                    tenantId,
                    order.getPayOrderNo(),
                    PaymentExceptionOrderService.TYPE_CHANNEL_FAILED,
                    PaymentExceptionOrderService.SEVERITY_HIGH,
                    "通道支付回调返回失败状态，支付订单已失败并等待人工核对失败原因",
                    eventTime);
        }
        notifyPaymentTerminal(tenantId, order.getId(), order.getBusinessOrderId());
        observabilityService.logSummary("CHANNEL_PAYMENT_CALLBACK", order.getPayOrderNo(), targetStatus,
                order.getAmount(), channelCode, elapsedMillis(startedAt), "CHANGED");
        return result(true, order.getPayOrderNo(), targetStatus, flowNo, "已按通道回调推进支付订单状态");
    }

    private PaymentChannelCallbackResultVO handleRefund(PaymentChannelCallbackCommand command) {
        long startedAt = System.nanoTime();
        Long tenantId = PaymentContextSupport.currentTenantId();
        String channelCode = normalize(command.getChannelCode());
        PaymentRefundOrderVO refundOrder = selectRefundOrder(tenantId, command);
        Require.isTrue(channelCode.equals(normalize(refundOrder.getChannelCode())),
                PaymentCode.PAYMENT_REFUND_ORDER_NOT_FOUND.getCode(), "退款订单通道不匹配");
        String channelRefundNo = PaymentContextSupport.trimToNull(command.getChannelRefundNo());
        Require.notBlank(channelRefundNo, PaymentCode.PAYMENT_REFUND_ORDER_NOT_FOUND.getCode(), "通道退款单号不能为空");
        Require.isTrue(channelRefundNo.equals(PaymentContextSupport.trimToNull(refundOrder.getChannelRefundNo())),
                PaymentCode.PAYMENT_REFUND_ORDER_NOT_FOUND.getCode(), "通道退款单号不匹配");
        Require.isTrue(sameMerchant(command.getChannelMerchantNo(), refundOrder.getChannelMerchantNo()),
                PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "通道商户号不匹配");
        long callbackAmount = Money.cents(command.getAmount()).toPositiveCents("通道退款回调金额");
        Require.isTrue(callbackAmount == Money.cents(refundOrder.getRefundAmount()).toPositiveCents("退款订单金额"),
                PaymentCode.PAYMENT_AMOUNT_INVALID.getCode(), "通道退款回调金额与退款订单金额不一致");

        String currentStatus = normalizeRefundStatus(refundOrder.getStatus());
        String targetStatus = refundTargetStatus(command.getChannelStatus());
        if (isRefundTerminal(currentStatus)) {
            return result(false, refundOrder.getRefundOrderNo(), currentStatus,
                    refundOrderMapper.selectLatestFlowNo(tenantId, refundOrder.getId()), "本地退款订单已是终态，回调幂等返回");
        }
        Require.isTrue(PaymentRefundOrderStatusEnum.REFUNDING.getCode().equals(currentStatus),
                PaymentCode.PAYMENT_REFUND_ORDER_STATE_INVALID.getCode(), "只有退款中的订单允许回调推进");
        orderStateService.requireRefundTransition(currentStatus, targetStatus);
        LocalDateTime eventTime = command.getEventTime() == null ? LocalDateTime.now() : command.getEventTime();
        int updated = refundOrderMapper.updateRefundingQueryResult(
                tenantId,
                refundOrder.getId(),
                targetStatus,
                PaymentRefundOrderStatusEnum.SUCCESS.getCode().equals(targetStatus) ? eventTime : null);
        Require.isTrue(updated == 1, PaymentCode.PAYMENT_REFUND_ORDER_STATE_INVALID.getCode(), "退款订单状态已变化，请刷新后重试");
        statusFlowService.record(
                tenantId,
                PaymentOrderStatusFlowService.ORDER_TYPE_REFUND,
                refundOrder.getId(),
                refundOrder.getRefundOrderNo(),
                currentStatus,
                targetStatus,
                PaymentOrderStatusFlowService.SOURCE_CHANNEL_CALLBACK,
                channelRefundNo,
                eventTime,
                "通道退款回调推进退款订单状态");

        String flowNo = null;
        if (PaymentRefundOrderStatusEnum.SUCCESS.getCode().equals(targetStatus)) {
            boolean duplicateRefund = duplicateRefundCompletionService.completeIfDuplicateRefund(
                    tenantId,
                    refundOrder,
                    PaymentOrderStatusFlowService.SOURCE_CHANNEL_CALLBACK,
                    channelRefundNo,
                    eventTime);
            if (!duplicateRefund) {
                PaymentBusinessOrderEntity businessOrder = businessOrderMapper.selectCashierBusinessOrder(tenantId, refundOrder.getBusinessOrderId());
                Require.notNull(businessOrder, PaymentCode.PAYMENT_BUSINESS_ORDER_NOT_FOUND);
                int businessUpdated = businessOrderMapper.updateRefundProgress(
                        tenantId,
                        refundOrder.getBusinessOrderId(),
                        refundOrder.getRefundAmount());
                Require.isTrue(businessUpdated == 1, PaymentCode.PAYMENT_REFUND_AMOUNT_EXCEEDED);
                statusFlowService.record(
                        tenantId,
                        PaymentOrderStatusFlowService.ORDER_TYPE_BUSINESS,
                        refundOrder.getBusinessOrderId(),
                        businessOrder.getBizOrderNo(),
                        businessOrder.getStatus(),
                        nextBusinessRefundStatus(businessOrder, refundOrder.getRefundAmount()),
                        PaymentOrderStatusFlowService.SOURCE_CHANNEL_CALLBACK,
                        channelRefundNo,
                        eventTime,
                        "通道退款回调确认退款成功");
            }
            flowNo = numberService.next(PaymentNumberService.PAY_REFUND_FLOW_NO);
            insertFlow(flowNo, refundOrder.getBusinessOrderId(), refundOrder.getPaymentOrderId(), refundOrder.getId(),
                    "REFUND_SUCCESS", refundOrder.getRefundAmount(), tenantId);
            refundOrder.setFlowNo(flowNo);
            refundOrder.setRefundTime(eventTime);
        } else if (PaymentRefundOrderStatusEnum.FAILED.getCode().equals(targetStatus)) {
            exceptionOrderService.createIfAbsent(
                    tenantId,
                    refundOrder.getRefundOrderNo(),
                    PaymentExceptionOrderService.TYPE_REFUND_MISMATCH,
                    PaymentExceptionOrderService.SEVERITY_HIGH,
                    "通道退款回调返回失败状态，退款订单已失败并等待人工核对退款结果",
                    eventTime);
        }
        refundOrder.setStatus(targetStatus);
        notifyRefundTerminal(tenantId, refundOrder);
        observabilityService.logSummary("CHANNEL_REFUND_CALLBACK", refundOrder.getRefundOrderNo(), targetStatus,
                refundOrder.getRefundAmount(), channelCode, elapsedMillis(startedAt), "CHANGED");
        return result(true, refundOrder.getRefundOrderNo(), targetStatus, flowNo, "已按通道回调推进退款订单状态");
    }

    private PaymentOrderEntity selectPaymentOrder(Long tenantId, PaymentChannelCallbackCommand command, String channelCode) {
        String payOrderNo = PaymentContextSupport.trimToNull(command.getPayOrderNo());
        if (payOrderNo != null) {
            PaymentOrderEntity order = paymentOrderMapper.selectByTenantAndPayOrderNo(tenantId, payOrderNo);
            Require.notNull(order, PaymentCode.PAYMENT_ORDER_NOT_FOUND);
            return order;
        }
        String channelTradeNo = PaymentContextSupport.trimToNull(command.getChannelTradeNo());
        Require.notBlank(channelTradeNo, PaymentCode.PAYMENT_ORDER_NOT_FOUND.getCode(), "支付订单号或通道交易号不能为空");
        PaymentOrderEntity order = paymentOrderMapper.selectByTenantAndChannelTradeNo(tenantId, channelCode, channelTradeNo);
        Require.notNull(order, PaymentCode.PAYMENT_ORDER_NOT_FOUND);
        return order;
    }

    private PaymentRefundOrderVO selectRefundOrder(Long tenantId, PaymentChannelCallbackCommand command) {
        String refundOrderNo = PaymentContextSupport.trimToNull(command.getRefundOrderNo());
        if (refundOrderNo != null) {
            PaymentRefundOrderVO order = refundOrderMapper.selectByTenantAndRefundOrderNo(tenantId, refundOrderNo);
            Require.notNull(order, PaymentCode.PAYMENT_REFUND_ORDER_NOT_FOUND);
            return order;
        }
        String channelRefundNo = PaymentContextSupport.trimToNull(command.getChannelRefundNo());
        Require.notBlank(channelRefundNo, PaymentCode.PAYMENT_REFUND_ORDER_NOT_FOUND.getCode(), "退款订单号或通道退款单号不能为空");
        PaymentRefundOrderVO order = refundOrderMapper.selectByTenantAndChannelRefundNo(tenantId, channelRefundNo);
        Require.notNull(order, PaymentCode.PAYMENT_REFUND_ORDER_NOT_FOUND);
        return order;
    }

    private int updatePaymentCallbackResult(
            Long tenantId,
            PaymentOrderEntity order,
            String targetStatus,
            LocalDateTime eventTime,
            String channelTradeNo) {
        try {
            return paymentOrderMapper.updatePayingCallbackResult(
                    tenantId,
                    order.getId(),
                    targetStatus,
                    PaymentOrderStatusEnum.SUCCESS.getCode().equals(targetStatus) ? 1 : 0,
                    PaymentOrderStatusEnum.SUCCESS.getCode().equals(targetStatus) ? eventTime : null,
                    channelTradeNo);
        } catch (DuplicateKeyException ex) {
            return -1;
        }
    }

    private void notifyPaymentTerminal(Long tenantId, Long paymentOrderId, Long businessOrderId) {
        PaymentBusinessOrderEntity businessOrder = businessOrderMapper.selectCashierBusinessOrder(tenantId, businessOrderId);
        Require.notNull(businessOrder, PaymentCode.PAYMENT_BUSINESS_ORDER_NOT_FOUND);
        PaymentApplication application = selectApplication(tenantId, businessOrder.getAppCode());
        PaymentOrderVO paymentOrder = paymentOrderMapper.selectPaymentOrderById(tenantId, paymentOrderId);
        Require.notNull(paymentOrder, PaymentCode.PAYMENT_ORDER_NOT_FOUND);
        notificationService.notifyPaymentAfterCommit(application, businessOrder, paymentOrder);
    }

    private void notifyRefundTerminal(Long tenantId, PaymentRefundOrderVO refundOrder) {
        PaymentApplication application = selectApplication(tenantId, refundOrder.getAppId());
        PaymentBusinessOrderEntity businessOrder = businessOrderMapper.selectCashierBusinessOrder(tenantId, refundOrder.getBusinessOrderId());
        Require.notNull(businessOrder, PaymentCode.PAYMENT_BUSINESS_ORDER_NOT_FOUND);
        notificationService.notifyRefundAfterCommit(application, businessOrder, refundOrder);
    }

    private PaymentApplication selectApplication(Long tenantId, String appId) {
        PaymentApplication application = applicationMapper.selectOne(new LambdaQueryWrapper<PaymentApplication>()
                .eq(PaymentApplication::getTenantId, tenantId)
                .eq(PaymentApplication::getAppId, appId)
                .eq(PaymentApplication::getStatus, 1));
        Require.notNull(application, PaymentCode.PAYMENT_APPLICATION_NOT_FOUND);
        return application;
    }

    private String selectBusinessOrderNo(Long tenantId, Long businessOrderId) {
        PaymentBusinessOrderEntity businessOrder = businessOrderMapper.selectCashierBusinessOrder(tenantId, businessOrderId);
        Require.notNull(businessOrder, PaymentCode.PAYMENT_BUSINESS_ORDER_NOT_FOUND);
        return businessOrder.getBizOrderNo();
    }

    private String nextBusinessRefundStatus(PaymentBusinessOrderEntity businessOrder, Long refundAmount) {
        long paidAmount = businessOrder.getPaidAmount() == null ? 0L : businessOrder.getPaidAmount();
        long refundedAmount = businessOrder.getRefundedAmount() == null ? 0L : businessOrder.getRefundedAmount();
        long nextRefundedAmount = refundedAmount + (refundAmount == null ? 0L : refundAmount);
        if (nextRefundedAmount >= paidAmount) {
            return PaymentBusinessOrderStatusEnum.REFUNDED.getCode();
        }
        return PaymentBusinessOrderStatusEnum.PARTIAL_REFUNDED.getCode();
    }

    private String firstText(String first, String second) {
        String normalizedFirst = PaymentContextSupport.trimToNull(first);
        if (normalizedFirst != null) {
            return normalizedFirst;
        }
        return PaymentContextSupport.trimToNull(second);
    }

    private void insertFlow(String flowNo, Long businessOrderId, Long paymentOrderId, Long refundOrderId, String flowType, Long amount, Long tenantId) {
        PaymentTransactionFlowEntity flow = new PaymentTransactionFlowEntity();
        flow.setFlowNo(flowNo);
        flow.setBusinessOrderId(businessOrderId);
        flow.setPaymentOrderId(paymentOrderId);
        flow.setRefundOrderId(refundOrderId);
        flow.setFlowType(flowType);
        flow.setAmount(amount);
        flow.setTenantId(tenantId);
        transactionFlowMapper.insert(flow);
    }

    private String paymentTargetStatus(String channelStatus) {
        String normalized = normalize(channelStatus);
        if (PaymentOrderStatusEnum.SUCCESS.getCode().equals(normalized)) {
            return PaymentOrderStatusEnum.SUCCESS.getCode();
        }
        if (PaymentOrderStatusEnum.FAILED.getCode().equals(normalized)) {
            return PaymentOrderStatusEnum.FAILED.getCode();
        }
        if (PaymentOrderStatusEnum.CLOSED.getCode().equals(normalized)) {
            return PaymentOrderStatusEnum.CLOSED.getCode();
        }
        throw new BizException(PaymentCode.PAYMENT_ORDER_STATE_INVALID.getCode(), "通道支付回调状态不支持");
    }

    private String refundTargetStatus(String channelStatus) {
        String normalized = normalize(channelStatus);
        if (PaymentRefundOrderStatusEnum.SUCCESS.getCode().equals(normalized)) {
            return PaymentRefundOrderStatusEnum.SUCCESS.getCode();
        }
        if (PaymentRefundOrderStatusEnum.FAILED.getCode().equals(normalized)) {
            return PaymentRefundOrderStatusEnum.FAILED.getCode();
        }
        throw new BizException(PaymentCode.PAYMENT_REFUND_ORDER_STATE_INVALID.getCode(), "通道退款回调状态不支持");
    }

    private boolean isPaymentTerminal(String status) {
        return PaymentOrderStatusEnum.SUCCESS.getCode().equals(status)
                || PaymentOrderStatusEnum.FAILED.getCode().equals(status)
                || PaymentOrderStatusEnum.CLOSED.getCode().equals(status);
    }

    private boolean isRefundTerminal(String status) {
        return PaymentRefundOrderStatusEnum.SUCCESS.getCode().equals(status)
                || PaymentRefundOrderStatusEnum.FAILED.getCode().equals(status)
                || PaymentRefundOrderStatusEnum.CLOSED.getCode().equals(status);
    }

    private String normalizeRefundStatus(String status) {
        if ("PROCESSING".equals(status)) {
            return PaymentRefundOrderStatusEnum.REFUNDING.getCode();
        }
        return status;
    }

    private boolean sameMerchant(String callbackMerchantNo, String orderMerchantNo) {
        String callbackValue = PaymentContextSupport.trimToNull(callbackMerchantNo);
        String orderValue = PaymentContextSupport.trimToNull(orderMerchantNo);
        return callbackValue != null && callbackValue.equals(orderValue);
    }

    private PaymentChannelCallbackResultVO result(boolean changed, String orderNo, String status, String flowNo, String message) {
        PaymentChannelCallbackResultVO vo = new PaymentChannelCallbackResultVO();
        vo.setChanged(changed);
        vo.setOrderNo(orderNo);
        vo.setStatus(status);
        vo.setFlowNo(flowNo);
        vo.setMessage(message);
        return vo;
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    private String normalize(String value) {
        String normalized = PaymentContextSupport.trimToNull(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }
}
