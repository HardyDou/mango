package io.mango.payment.core.service;

import io.mango.common.result.Require;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.vo.PaymentTaskDispatchResultVO;
import io.mango.payment.core.entity.PaymentOrderEntity;
import io.mango.payment.core.mapper.PaymentOrderMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentTaskService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentTaskService.class);

    private final PaymentOrderMapper paymentOrderMapper;
    private final PaymentChannelSyncService channelSyncService;
    private final PaymentChannelOrderCloseService channelOrderCloseService;
    private final PaymentOperationAuditService auditService;

    public PaymentTaskDispatchResultVO expireOpenPaymentOrders(long limit) {
        long resolvedLimit = validateTaskLimit(limit);
        List<PaymentOrderEntity> orders = paymentOrderMapper.selectExpiredOpenPaymentOrders(
                PaymentContextSupport.currentTenantId(), LocalDateTime.now(), resolvedLimit);
        PaymentTaskDispatchResultVO result = new PaymentTaskDispatchResultVO();
        result.setScannedCount(orders.size());
        for (PaymentOrderEntity order : orders) {
            if (order.getPayOrderNo() == null) {
                result.setSkippedCount(result.getSkippedCount() + 1);
                continue;
            }
            try {
                PaymentChannelOrderCloseService.CloseResult closeResult =
                        channelOrderCloseService.closeExpiredPaymentOrder(order.getPayOrderNo());
                if (closeResult.changed()) {
                    result.setSuccessCount(result.getSuccessCount() + 1);
                } else {
                    result.setSkippedCount(result.getSkippedCount() + 1);
                }
            } catch (RuntimeException ex) {
                result.setFailedCount(result.getFailedCount() + 1);
                LOGGER.warn("Payment expired order close failed: payOrderNo={}", order.getPayOrderNo(), ex);
            }
        }
        auditService.record(
                PaymentOperationAuditService.ACTION_EXPIRE_OPEN_PAYMENT_ORDERS,
                PaymentOperationAuditService.RESOURCE_PAYMENT_ORDER,
                taskResultResourceId("EXPIRE_OPEN_PAYMENT_ORDERS", result),
                PaymentOperationAuditService.RESULT_SUCCESS);
        return result;
    }

    public PaymentTaskDispatchResultVO queryProcessingPaymentOrders(long limit) {
        long resolvedLimit = validateTaskLimit(limit);
        List<PaymentOrderEntity> orders = paymentOrderMapper.selectProcessingPaymentOrders(
                PaymentContextSupport.currentTenantId(), resolvedLimit);
        PaymentTaskDispatchResultVO result = new PaymentTaskDispatchResultVO();
        result.setScannedCount(orders.size());
        for (PaymentOrderEntity order : orders) {
            if (order.getPayOrderNo() == null) {
                result.setSkippedCount(result.getSkippedCount() + 1);
                continue;
            }
            try {
                PaymentChannelSyncService.PaymentSyncResult queryResult =
                        channelSyncService.syncPaymentStatus(order.getPayOrderNo());
                if (queryResult.changed()) {
                    result.setSuccessCount(result.getSuccessCount() + 1);
                } else {
                    result.setSkippedCount(result.getSkippedCount() + 1);
                }
            } catch (RuntimeException ex) {
                result.setFailedCount(result.getFailedCount() + 1);
                LOGGER.warn("Payment processing order query failed: payOrderNo={}", order.getPayOrderNo(), ex);
            }
        }
        auditService.record(
                PaymentOperationAuditService.ACTION_QUERY_PROCESSING_PAYMENT_ORDERS,
                PaymentOperationAuditService.RESOURCE_PAYMENT_ORDER,
                taskResultResourceId("QUERY_PROCESSING_PAYMENT_ORDERS", result),
                PaymentOperationAuditService.RESULT_SUCCESS);
        return result;
    }

    private long validateTaskLimit(long limit) {
        Require.isTrue(limit > 0 && limit <= 100, PaymentCode.PAYMENT_READONLY_RESOURCE_INVALID.getCode(),
                "任务批次大小必须在 1 到 100 之间");
        return limit;
    }

    private String taskResultResourceId(String taskCode, PaymentTaskDispatchResultVO result) {
        return taskCode
                + ":s=" + result.getScannedCount()
                + ",ok=" + result.getSuccessCount()
                + ",skip=" + result.getSkippedCount()
                + ",fail=" + result.getFailedCount();
    }
}
