package io.mango.payment.core.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import io.mango.common.exception.BizException;
import io.mango.common.result.Require;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.enums.PaymentOrderStatusEnum;
import io.mango.payment.api.enums.PaymentRefundOrderStatusEnum;
import io.mango.payment.core.entity.PaymentOrderEntity;
import io.mango.payment.core.entity.PaymentRefundOrderEntity;
import io.mango.payment.core.entity.PaymentTransactionFlowEntity;
import io.mango.payment.core.mapper.PaymentOrderMapper;
import io.mango.payment.core.mapper.PaymentRefundOrderMapper;
import io.mango.payment.core.mapper.PaymentTransactionFlowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentDuplicatePaymentService {

    private static final String MANGO_PAY_CHANNEL_CODE = "MANGO_PAY";

    private final PaymentOrderMapper paymentOrderMapper;
    private final PaymentRefundOrderMapper refundOrderMapper;
    private final PaymentTransactionFlowMapper transactionFlowMapper;
    private final PaymentExceptionOrderRecordService exceptionOrderRecordService;
    private final PaymentOrderStateService orderStateService;
    private final PaymentOrderStatusFlowService statusFlowService;
    private final PaymentNumberService numberService;

    public DuplicatePaymentResult handleDuplicateSuccess(
            Long tenantId,
            PaymentOrderEntity order,
            LocalDateTime eventTime,
            String triggerSource,
            String triggerNo,
            String channelTradeNo) {
        Require.notNull(tenantId, PaymentCode.PAYMENT_ORDER_STATE_INVALID.getCode(), "租户 ID 不能为空");
        Require.notNull(order, PaymentCode.PAYMENT_ORDER_NOT_FOUND);
        Require.notNull(order.getId(), PaymentCode.PAYMENT_ORDER_NOT_FOUND.getCode(), "支付订单 ID 不能为空");
        Require.notBlank(order.getPayOrderNo(), PaymentCode.PAYMENT_ORDER_NOT_FOUND.getCode(), "支付订单号不能为空");
        Require.notNull(order.getBusinessOrderId(), PaymentCode.PAYMENT_BUSINESS_ORDER_NOT_FOUND.getCode(), "业务订单 ID 不能为空");
        Require.notNull(eventTime, PaymentCode.PAYMENT_ORDER_STATE_INVALID.getCode(), "支付事件时间不能为空");
        Require.notBlank(triggerSource, PaymentCode.PAYMENT_ORDER_STATE_INVALID.getCode(), "重复支付触发来源不能为空");

        int successUpdated = paymentOrderMapper.markDuplicatePaymentSuccess(
                tenantId,
                order.getId(),
                eventTime,
                PaymentContextSupport.trimToNull(channelTradeNo));
        Require.isTrue(successUpdated == 1, PaymentCode.PAYMENT_ORDER_STATE_INVALID.getCode(), "重复支付订单状态已变化，请刷新后重试");
        statusFlowService.record(
                tenantId,
                PaymentOrderStatusFlowService.ORDER_TYPE_PAYMENT,
                order.getId(),
                order.getPayOrderNo(),
                PaymentOrderStatusEnum.PAYING.getCode(),
                PaymentOrderStatusEnum.SUCCESS.getCode(),
                triggerSource,
                effectiveTriggerNo(triggerNo, order),
                eventTime,
                "重复成功支付先记录为非有效成功支付");
        insertFlow(numberService.next(PaymentNumberService.PAY_FLOW_NO), order, null, "PAY_SUCCESS", eventTime, tenantId);

        if (!MANGO_PAY_CHANNEL_CODE.equalsIgnoreCase(PaymentContextSupport.trimToNull(order.getChannelCode()))) {
            String exceptionNo = exceptionOrderRecordService.createIfAbsent(
                    tenantId,
                    order.getPayOrderNo(),
                    PaymentExceptionOrderRecordService.TYPE_DUPLICATE_PAYMENT,
                    PaymentExceptionOrderRecordService.SEVERITY_HIGH,
                    "重复成功支付已落库，当前通道未具备自动退款适配器，已挂起异常处理",
                    eventTime).getExceptionNo();
            return new DuplicatePaymentResult(
                    order.getPayOrderNo(),
                    PaymentOrderStatusEnum.SUCCESS.getCode(),
                    false,
                    null,
                    exceptionNo);
        }

        orderStateService.requirePaymentTransition(
                PaymentOrderStatusEnum.SUCCESS.getCode(),
                PaymentOrderStatusEnum.DUPLICATE_REFUNDING.getCode());
        int refundingUpdated = paymentOrderMapper.markDuplicatePaymentRefunding(tenantId, order.getId());
        Require.isTrue(refundingUpdated == 1, PaymentCode.PAYMENT_ORDER_STATE_INVALID.getCode(), "重复支付订单退款状态已变化，请刷新后重试");
        statusFlowService.record(
                tenantId,
                PaymentOrderStatusFlowService.ORDER_TYPE_PAYMENT,
                order.getId(),
                order.getPayOrderNo(),
                PaymentOrderStatusEnum.SUCCESS.getCode(),
                PaymentOrderStatusEnum.DUPLICATE_REFUNDING.getCode(),
                triggerSource,
                effectiveTriggerNo(triggerNo, order),
                eventTime,
                "重复成功支付进入自动退款");

        PaymentRefundOrderEntity refundOrder = createDuplicateRefundOrder(tenantId, order, eventTime, triggerSource);
        return new DuplicatePaymentResult(
                order.getPayOrderNo(),
                PaymentOrderStatusEnum.DUPLICATE_REFUNDING.getCode(),
                false,
                refundOrder.getRefundOrderNo(),
                null);
    }

    private PaymentRefundOrderEntity createDuplicateRefundOrder(
            Long tenantId,
            PaymentOrderEntity order,
            LocalDateTime eventTime,
            String triggerSource) {
        PaymentRefundOrderEntity existing = refundOrderMapper.selectOne(new LambdaQueryWrapper<PaymentRefundOrderEntity>()
                .eq(PaymentRefundOrderEntity::getTenantId, tenantId)
                .eq(PaymentRefundOrderEntity::getPaymentOrderId, order.getId())
                .eq(PaymentRefundOrderEntity::getBizRefundNo, duplicateBizRefundNo(order)));
        if (existing != null) {
            return existing;
        }
        PaymentRefundOrderEntity refundOrder = new PaymentRefundOrderEntity();
        refundOrder.setId(IdWorker.getId());
        refundOrder.setRefundOrderNo(numberService.next(PaymentNumberService.PAY_REFUND_ORDER_NO));
        refundOrder.setBizRefundNo(duplicateBizRefundNo(order));
        refundOrder.setPaymentOrderId(order.getId());
        refundOrder.setChannelRefundNo("DUP-REFUND-" + order.getPayOrderNo());
        refundOrder.setRefundAmount(order.getAmount());
        refundOrder.setReason("重复成功支付自动退款");
        refundOrder.setStatus(PaymentRefundOrderStatusEnum.REFUNDING.getCode());
        refundOrder.setRefundTime(null);
        refundOrder.setTenantId(tenantId);
        refundOrder.setCreatedBy(PaymentContextSupport.currentUserId());
        refundOrder.setCreatedAt(eventTime);
        refundOrder.setUpdatedBy(PaymentContextSupport.currentUserId());
        refundOrder.setUpdatedAt(eventTime);
        refundOrderMapper.insert(refundOrder);
        statusFlowService.record(
                tenantId,
                PaymentOrderStatusFlowService.ORDER_TYPE_REFUND,
                refundOrder.getId(),
                refundOrder.getRefundOrderNo(),
                PaymentRefundOrderStatusEnum.CREATED.getCode(),
                PaymentRefundOrderStatusEnum.REFUNDING.getCode(),
                triggerSource,
                order.getPayOrderNo(),
                eventTime,
                "重复成功支付自动退款单创建并等待通道结果");
        return refundOrder;
    }

    private void insertFlow(
            String flowNo,
            PaymentOrderEntity order,
            Long refundOrderId,
            String flowType,
            LocalDateTime eventTime,
            Long tenantId) {
        PaymentTransactionFlowEntity flow = new PaymentTransactionFlowEntity();
        flow.setId(IdWorker.getId());
        flow.setFlowNo(flowNo);
        flow.setBusinessOrderId(order.getBusinessOrderId());
        flow.setPaymentOrderId(order.getId());
        flow.setRefundOrderId(refundOrderId);
        flow.setFlowType(flowType);
        flow.setAmount(order.getAmount());
        flow.setTenantId(tenantId);
        flow.setCreatedBy(PaymentContextSupport.currentUserId());
        flow.setCreatedAt(eventTime);
        flow.setUpdatedBy(PaymentContextSupport.currentUserId());
        flow.setUpdatedAt(eventTime);
        transactionFlowMapper.insert(flow);
    }

    private String duplicateBizRefundNo(PaymentOrderEntity order) {
        return "DUP-" + order.getPayOrderNo();
    }

    private String effectiveTriggerNo(String triggerNo, PaymentOrderEntity order) {
        String normalized = PaymentContextSupport.trimToNull(triggerNo);
        if (normalized != null) {
            return normalized;
        }
        return order.getPayOrderNo();
    }

    public record DuplicatePaymentResult(
            String payOrderNo,
            String status,
            boolean refunded,
            String refundOrderNo,
            String exceptionNo) {
    }
}
