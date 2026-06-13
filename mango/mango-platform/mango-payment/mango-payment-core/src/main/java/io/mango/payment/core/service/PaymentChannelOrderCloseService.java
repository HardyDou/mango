package io.mango.payment.core.service;

import io.mango.common.result.Require;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.enums.PaymentBusinessOrderStatusEnum;
import io.mango.payment.api.enums.PaymentOrderStatusEnum;
import io.mango.payment.api.vo.PaymentOrderVO;
import io.mango.payment.core.entity.PaymentApplication;
import io.mango.payment.core.entity.PaymentBusinessOrderEntity;
import io.mango.payment.core.entity.PaymentOrderEntity;
import io.mango.payment.core.mapper.PaymentApplicationMapper;
import io.mango.payment.core.mapper.PaymentBusinessOrderMapper;
import io.mango.payment.core.mapper.PaymentOrderMapper;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentChannelOrderCloseService {

    private final PaymentOrderMapper paymentOrderMapper;
    private final PaymentBusinessOrderMapper businessOrderMapper;
    private final PaymentApplicationMapper applicationMapper;
    private final PaymentOrderStateService orderStateService;
    private final PaymentOperationAuditService auditService;
    private final PaymentNotificationService notificationService;
    private final PaymentOrderStatusFlowService statusFlowService;
    private final PaymentExceptionOrderService exceptionOrderService;

    @Transactional(rollbackFor = Exception.class)
    public CloseResult closePaymentOrder(String payOrderNo) {
        return closePayment(payOrderNo, false);
    }

    @Transactional(rollbackFor = Exception.class)
    public CloseResult closeExpiredPaymentOrder(String payOrderNo) {
        return closePayment(payOrderNo, true);
    }

    private CloseResult closePayment(String payOrderNo, boolean expiredClose) {
        Require.notBlank(payOrderNo, PaymentCode.PAYMENT_CASHIER_PAY_INVALID.getCode(), "支付订单号不能为空");
        Long tenantId = PaymentContextSupport.currentTenantId();
        PaymentOrderEntity paymentOrder = paymentOrderMapper.selectByTenantAndPayOrderNo(tenantId, payOrderNo);
        Require.notNull(paymentOrder, PaymentCode.PAYMENT_ORDER_NOT_FOUND);
        String currentStatus = paymentOrder.getStatus();
        if (PaymentOrderStatusEnum.CLOSED.getCode().equals(currentStatus)) {
            return new CloseResult(payOrderNo, currentStatus, false);
        }
        Require.isTrue(isClosablePayment(currentStatus),
                PaymentCode.PAYMENT_ORDER_STATE_INVALID.getCode(), "只有未支付或支付中的订单允许关单");
        Require.isTrue(!Integer.valueOf(1).equals(paymentOrder.getSuccessFlag()),
                PaymentCode.PAYMENT_ORDER_STATE_INVALID.getCode(), "支付成功订单不允许关单");
        orderStateService.requirePaymentTransition(currentStatus, PaymentOrderStatusEnum.CLOSED.getCode());

        PaymentBusinessOrderEntity businessOrder = businessOrderMapper.selectCashierBusinessOrder(tenantId, paymentOrder.getBusinessOrderId());
        Require.notNull(businessOrder, PaymentCode.PAYMENT_BUSINESS_ORDER_NOT_FOUND);
        Require.isTrue(isClosableBusiness(businessOrder.getStatus()),
                PaymentCode.PAYMENT_BUSINESS_ORDER_STATE_CHANGED.getCode(), "业务订单当前状态不允许关单");
        Require.isTrue((businessOrder.getPaidAmount() == null ? 0L : businessOrder.getPaidAmount()) == 0L,
                PaymentCode.PAYMENT_BUSINESS_ORDER_STATE_CHANGED.getCode(), "业务订单已有支付金额，不允许关单");
        orderStateService.requireBusinessTransition(businessOrder.getStatus(), PaymentBusinessOrderStatusEnum.CLOSED.getCode());

        int paymentUpdated = paymentOrderMapper.closeOpenPaymentOrder(tenantId, paymentOrder.getId());
        Require.isTrue(paymentUpdated == 1, PaymentCode.PAYMENT_ORDER_STATE_INVALID.getCode(), "支付订单状态已变化，请刷新后重试");
        int businessUpdated = businessOrderMapper.closeOpenBusinessOrder(tenantId, businessOrder.getId());
        Require.isTrue(businessUpdated == 1, PaymentCode.PAYMENT_BUSINESS_ORDER_STATE_CHANGED.getCode(), "业务订单状态已变化，请刷新后重试");
        statusFlowService.record(
                tenantId,
                PaymentOrderStatusFlowService.ORDER_TYPE_PAYMENT,
                paymentOrder.getId(),
                payOrderNo,
                currentStatus,
                PaymentOrderStatusEnum.CLOSED.getCode(),
                PaymentOrderStatusFlowService.SOURCE_CHANNEL_CLOSE,
                payOrderNo,
                null,
                expiredClose ? "订单超时关闭支付订单" : "受控关单关闭支付订单");
        statusFlowService.record(
                tenantId,
                PaymentOrderStatusFlowService.ORDER_TYPE_BUSINESS,
                businessOrder.getId(),
                businessOrder.getBizOrderNo(),
                businessOrder.getStatus(),
                PaymentBusinessOrderStatusEnum.CLOSED.getCode(),
                PaymentOrderStatusFlowService.SOURCE_CHANNEL_CLOSE,
                payOrderNo,
                null,
                expiredClose ? "订单超时关闭业务订单" : "受控关单关闭业务订单");
        if (expiredClose) {
            exceptionOrderService.createIfAbsent(
                    tenantId,
                    payOrderNo,
                    PaymentExceptionOrderService.TYPE_PAY_TIMEOUT,
                    PaymentExceptionOrderService.SEVERITY_MEDIUM,
                    "支付订单超过有效支付时间未收到通道成功结果，已关闭并等待人工核对",
                    null);
        }
        auditService.record(
                PaymentOperationAuditService.ACTION_CLOSE_PAYMENT_ORDER,
                PaymentOperationAuditService.RESOURCE_PAYMENT_ORDER,
                payOrderNo,
                PaymentOperationAuditService.RESULT_SUCCESS);
        notifyPaymentClosed(tenantId, paymentOrder.getId(), businessOrder);
        return new CloseResult(payOrderNo, PaymentOrderStatusEnum.CLOSED.getCode(), true);
    }

    private void notifyPaymentClosed(Long tenantId, Long paymentOrderId, PaymentBusinessOrderEntity businessOrder) {
        PaymentApplication application = applicationMapper.selectOne(new LambdaQueryWrapper<PaymentApplication>()
                .eq(PaymentApplication::getTenantId, tenantId)
                .eq(PaymentApplication::getAppId, businessOrder.getAppCode()));
        Require.notNull(application, PaymentCode.PAYMENT_APPLICATION_NOT_FOUND);
        PaymentOrderVO closedOrder = paymentOrderMapper.selectPaymentOrderById(tenantId, paymentOrderId);
        Require.notNull(closedOrder, PaymentCode.PAYMENT_ORDER_NOT_FOUND);
        notificationService.notifyPaymentAfterCommit(application, businessOrder, closedOrder);
    }

    private boolean isClosablePayment(String status) {
        return PaymentOrderStatusEnum.CREATED.getCode().equals(status)
                || PaymentOrderStatusEnum.PAYING.getCode().equals(status);
    }

    private boolean isClosableBusiness(String status) {
        return PaymentBusinessOrderStatusEnum.TO_PAY.getCode().equals(status)
                || PaymentBusinessOrderStatusEnum.PAYING.getCode().equals(status);
    }

    public record CloseResult(String payOrderNo, String status, boolean changed) {
    }
}
