package io.mango.payment.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.common.exception.BizException;
import io.mango.common.result.Require;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.enums.PaymentBusinessOrderStatusEnum;
import io.mango.payment.api.enums.PaymentOrderStatusEnum;
import io.mango.payment.api.enums.PaymentRefundOrderStatusEnum;
import io.mango.payment.api.vo.PaymentOrderVO;
import io.mango.payment.api.vo.PaymentRefundOrderVO;
import io.mango.payment.core.entity.PaymentApplication;
import io.mango.payment.core.entity.PaymentBusinessOrderEntity;
import io.mango.payment.core.entity.PaymentChannelQueryRecordEntity;
import io.mango.payment.core.entity.PaymentOrderEntity;
import io.mango.payment.core.entity.PaymentRefundQueryRecordEntity;
import io.mango.payment.core.entity.PaymentTransactionFlowEntity;
import io.mango.payment.core.mapper.PaymentApplicationMapper;
import io.mango.payment.core.mapper.PaymentBusinessOrderMapper;
import io.mango.payment.core.mapper.PaymentChannelQueryRecordMapper;
import io.mango.payment.core.mapper.PaymentOrderMapper;
import io.mango.payment.core.mapper.PaymentRefundOrderMapper;
import io.mango.payment.core.mapper.PaymentRefundQueryRecordMapper;
import io.mango.payment.core.mapper.PaymentTransactionFlowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class PaymentChannelSyncService {

    private final PaymentOrderMapper paymentOrderMapper;
    private final PaymentRefundOrderMapper refundOrderMapper;
    private final PaymentBusinessOrderMapper businessOrderMapper;
    private final PaymentApplicationMapper applicationMapper;
    private final PaymentTransactionFlowMapper transactionFlowMapper;
    private final PaymentChannelQueryRecordMapper channelQueryRecordMapper;
    private final PaymentRefundQueryRecordMapper refundQueryRecordMapper;
    private final PaymentOrderStateService orderStateService;
    private final PaymentNotificationService notificationService;
    private final PaymentOrderStatusFlowService statusFlowService;
    private final PaymentDuplicatePaymentService duplicatePaymentService;
    private final PaymentDuplicateRefundCompletionService duplicateRefundCompletionService;
    private final PaymentChannelAdapterRegistry channelAdapterRegistry;
    private final ObjectMapper objectMapper;
    private final PaymentObservabilityService observabilityService;
    private final PaymentExceptionOrderService exceptionOrderService;
    private final PaymentNumberService numberService;
    private final PlatformTransactionManager transactionManager;

    public PaymentSyncResult syncPaymentStatus(String payOrderNo) {
        long startedAt = System.nanoTime();
        Require.notBlank(payOrderNo, PaymentCode.PAYMENT_CASHIER_PAY_INVALID.getCode(), "支付订单号不能为空");
        Long tenantId = PaymentContextSupport.currentTenantId();
        PaymentOrderEntity order = paymentOrderMapper.selectByTenantAndPayOrderNo(tenantId, payOrderNo);
        Require.notNull(order, PaymentCode.PAYMENT_ORDER_NOT_FOUND);
        String currentStatus = order.getStatus();
        LocalDateTime queryTime = LocalDateTime.now();
        if (isPaymentTerminal(currentStatus)) {
            QueryRecordSummary summary = inTransaction(() -> recordQuery(
                    tenantId,
                    order,
                    currentStatus,
                    currentStatus,
                    "NO_QUERY_TERMINAL",
                    "本地支付订单已是终态，主动查单不再覆盖本地终态",
                    null,
                    queryTime));
            logSummary(order, currentStatus, startedAt, "NO_QUERY_TERMINAL");
            return paymentResult(order.getPayOrderNo(), currentStatus, selectPaymentFlowNo(tenantId, order.getId()), false, summary);
        }
        Require.isTrue(PaymentOrderStatusEnum.PAYING.getCode().equals(currentStatus),
                PaymentCode.PAYMENT_ORDER_STATE_INVALID.getCode(), "只有支付中的订单允许主动查单推进");

        IPaymentChannelAdapter.PaymentQueryResult channelResponse = queryTargetStatus(tenantId, order);
        String targetStatus = channelResponse.status();
        if (PaymentOrderStatusEnum.PAYING.getCode().equals(targetStatus)) {
            QueryRecordSummary summary = inTransaction(() -> recordQuery(
                    tenantId,
                    order,
                    currentStatus,
                    targetStatus,
                    "NO_CHANGE_PROCESSING",
                    "通道返回处理中，本地支付订单保持支付中",
                    channelResponse,
                    queryTime));
            logSummary(order, currentStatus, startedAt, "NO_CHANGE_PROCESSING");
            return paymentResult(order.getPayOrderNo(), currentStatus, selectPaymentFlowNo(tenantId, order.getId()), false, summary);
        }

        PaymentSyncResult result = inTransaction(() -> applyPaymentChannelResult(
                tenantId,
                order,
                currentStatus,
                channelResponse,
                targetStatus,
                queryTime));
        logSummary(order, result.status(), startedAt, result.lastQueryResult());
        return result;
    }

    private PaymentSyncResult applyPaymentChannelResult(
            Long tenantId,
            PaymentOrderEntity order,
            String currentStatus,
            IPaymentChannelAdapter.PaymentQueryResult channelResponse,
            String targetStatus,
            LocalDateTime queryTime) {
        orderStateService.requirePaymentTransition(currentStatus, targetStatus);
        LocalDateTime payTime = PaymentOrderStatusEnum.SUCCESS.getCode().equals(targetStatus) ? queryTime : null;
        int updated = updatePayingQueryResult(tenantId, order, targetStatus, payTime);
        if (updated == -1) {
            PaymentDuplicatePaymentService.DuplicatePaymentResult duplicateResult = duplicatePaymentService.handleDuplicateSuccess(
                    tenantId,
                    order,
                    queryTime,
                    PaymentOrderStatusFlowService.SOURCE_CHANNEL_QUERY,
                    order.getPayOrderNo(),
                    order.getChannelTradeNo());
            QueryRecordSummary summary = recordQuery(
                    tenantId,
                    order,
                    currentStatus,
                    duplicateResult.status(),
                    duplicateResult.refunded() ? "DUPLICATE_REFUNDED" : "DUPLICATE_EXCEPTION",
                    duplicateResult.refunded() ? "重复成功支付已自动退款" : "重复成功支付已挂起异常处理",
                    channelResponse,
                    queryTime);
            return paymentResult(order.getPayOrderNo(), duplicateResult.status(), selectPaymentFlowNo(tenantId, order.getId()), true, summary);
        }
        Require.isTrue(updated == 1, PaymentCode.PAYMENT_ORDER_STATE_INVALID.getCode(), "支付订单状态已变化，请刷新后重试");
        statusFlowService.record(
                tenantId,
                PaymentOrderStatusFlowService.ORDER_TYPE_PAYMENT,
                order.getId(),
                order.getPayOrderNo(),
                currentStatus,
                targetStatus,
                PaymentOrderStatusFlowService.SOURCE_CHANNEL_QUERY,
                order.getPayOrderNo(),
                queryTime,
                "主动查单推进支付订单状态");

        String flowNo = null;
        if (PaymentOrderStatusEnum.SUCCESS.getCode().equals(targetStatus)) {
            flowNo = numberService.next(PaymentNumberService.PAY_FLOW_NO);
            PaymentTransactionFlowEntity flow = new PaymentTransactionFlowEntity();
            flow.setFlowNo(flowNo);
            flow.setBusinessOrderId(order.getBusinessOrderId());
            flow.setPaymentOrderId(order.getId());
            flow.setRefundOrderId(null);
            flow.setFlowType("PAY_SUCCESS");
            flow.setAmount(order.getAmount());
            flow.setTenantId(tenantId);
            transactionFlowMapper.insert(flow);
            int businessUpdated = businessOrderMapper.markCashierPaySuccess(tenantId, order.getBusinessOrderId(), order.getAmount());
            Require.isTrue(businessUpdated == 1, PaymentCode.PAYMENT_BUSINESS_ORDER_STATE_CHANGED);
            statusFlowService.record(
                    tenantId,
                    PaymentOrderStatusFlowService.ORDER_TYPE_BUSINESS,
                    order.getBusinessOrderId(),
                    selectBusinessOrderNo(tenantId, order.getBusinessOrderId()),
                    PaymentBusinessOrderStatusEnum.PAYING.getCode(),
                    PaymentBusinessOrderStatusEnum.PAID.getCode(),
                    PaymentOrderStatusFlowService.SOURCE_CHANNEL_QUERY,
                    order.getPayOrderNo(),
                    queryTime,
                    "主动查单确认支付成功");
        } else if (PaymentOrderStatusEnum.FAILED.getCode().equals(targetStatus)) {
            exceptionOrderService.createIfAbsent(
                    tenantId,
                    order.getPayOrderNo(),
                    PaymentExceptionOrderService.TYPE_CHANNEL_FAILED,
                    PaymentExceptionOrderService.SEVERITY_HIGH,
                    "主动查单发现通道支付失败，支付订单已失败并等待人工核对失败原因",
                    queryTime);
        }
        notifyPaymentTerminal(tenantId, order.getId(), order.getBusinessOrderId());
        QueryRecordSummary summary = recordQuery(
                tenantId,
                order,
                currentStatus,
                targetStatus,
                "UPDATED",
                "已按通道查单结果推进支付订单状态",
                channelResponse,
                queryTime);
        return paymentResult(order.getPayOrderNo(), targetStatus, flowNo, true, summary);
    }

    public RefundSyncResult syncRefundStatus(String refundOrderNo) {
        Require.notBlank(refundOrderNo, PaymentCode.PAYMENT_REFUND_ORDER_INVALID.getCode(), "退款订单号不能为空");
        Long tenantId = PaymentContextSupport.currentTenantId();
        PaymentRefundOrderVO refundOrder = refundOrderMapper.selectByTenantAndRefundOrderNo(tenantId, refundOrderNo);
        Require.notNull(refundOrder, PaymentCode.PAYMENT_REFUND_ORDER_NOT_FOUND);
        return syncRefundStatus(tenantId, refundOrder);
    }

    public RefundSyncResult syncRefundStatusByChannelRefundNo(String channelRefundNo) {
        Require.notBlank(channelRefundNo, PaymentCode.PAYMENT_REFUND_ORDER_INVALID.getCode(), "通道退款单号不能为空");
        Long tenantId = PaymentContextSupport.currentTenantId();
        PaymentRefundOrderVO refundOrder = refundOrderMapper.selectByTenantAndChannelRefundNo(tenantId, channelRefundNo);
        Require.notNull(refundOrder, PaymentCode.PAYMENT_REFUND_ORDER_NOT_FOUND);
        return syncRefundStatus(tenantId, refundOrder);
    }

    private RefundSyncResult syncRefundStatus(Long tenantId, PaymentRefundOrderVO refundOrder) {
        long startedAt = System.nanoTime();
        String currentStatus = normalizeRefundStatus(refundOrder.getStatus());
        LocalDateTime queryTime = LocalDateTime.now();
        if (isRefundTerminal(currentStatus)) {
            QueryRecordSummary summary = inTransaction(() -> recordRefundQuery(
                    tenantId,
                    refundOrder,
                    currentStatus,
                    currentStatus,
                    "NO_QUERY_TERMINAL",
                    "本地退款订单已是终态，主动查询不再覆盖本地终态",
                    null,
                    queryTime));
            logRefundSummary(refundOrder, currentStatus, startedAt, "NO_QUERY_TERMINAL");
            return refundResult(refundOrder, currentStatus, false, summary);
        }
        Require.isTrue(PaymentRefundOrderStatusEnum.REFUNDING.getCode().equals(currentStatus),
                PaymentCode.PAYMENT_REFUND_ORDER_STATE_INVALID.getCode(), "只有退款中的订单允许主动查询推进");

        IPaymentChannelAdapter.RefundQueryResult channelResponse = queryTargetStatus(tenantId, refundOrder);
        String targetStatus = channelResponse.status();
        if (PaymentRefundOrderStatusEnum.REFUNDING.getCode().equals(targetStatus)) {
            QueryRecordSummary summary = inTransaction(() -> recordRefundQuery(
                    tenantId,
                    refundOrder,
                    currentStatus,
                    targetStatus,
                    "NO_CHANGE_PROCESSING",
                    "通道返回退款处理中，本地退款订单保持退款中",
                    channelResponse,
                    queryTime));
            logRefundSummary(refundOrder, currentStatus, startedAt, "NO_CHANGE_PROCESSING");
            return refundResult(refundOrder, currentStatus, false, summary);
        }

        RefundSyncResult result = inTransaction(() -> applyRefundChannelResult(
                tenantId,
                refundOrder,
                currentStatus,
                channelResponse,
                targetStatus,
                queryTime));
        logRefundSummary(refundOrder, result.status(), startedAt, result.lastQueryResult());
        return result;
    }

    private RefundSyncResult applyRefundChannelResult(
            Long tenantId,
            PaymentRefundOrderVO refundOrder,
            String currentStatus,
            IPaymentChannelAdapter.RefundQueryResult channelResponse,
            String targetStatus,
            LocalDateTime queryTime) {
        orderStateService.requireRefundTransition(currentStatus, targetStatus);
        LocalDateTime refundTime = PaymentRefundOrderStatusEnum.SUCCESS.getCode().equals(targetStatus) ? queryTime : null;
        int updated = refundOrderMapper.updateRefundingQueryResult(tenantId, refundOrder.getId(), targetStatus, refundTime);
        if (updated == 0) {
            return handleConcurrentRefundSyncResult(tenantId, refundOrder, currentStatus, targetStatus, channelResponse, queryTime);
        }
        statusFlowService.record(
                tenantId,
                PaymentOrderStatusFlowService.ORDER_TYPE_REFUND,
                refundOrder.getId(),
                refundOrder.getRefundOrderNo(),
                currentStatus,
                targetStatus,
                PaymentOrderStatusFlowService.SOURCE_REFUND_QUERY,
                refundOrder.getRefundOrderNo(),
                queryTime,
                "主动查退款推进退款订单状态");
        String flowNo = null;
        if (PaymentRefundOrderStatusEnum.SUCCESS.getCode().equals(targetStatus)) {
            boolean duplicateRefund = duplicateRefundCompletionService.completeIfDuplicateRefund(
                    tenantId,
                    refundOrder,
                    PaymentOrderStatusFlowService.SOURCE_REFUND_QUERY,
                    refundOrder.getRefundOrderNo(),
                    queryTime);
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
                        PaymentOrderStatusFlowService.SOURCE_REFUND_QUERY,
                        refundOrder.getRefundOrderNo(),
                        queryTime,
                        "主动查退款确认退款成功");
            }
            flowNo = numberService.next(PaymentNumberService.PAY_REFUND_FLOW_NO);
            PaymentTransactionFlowEntity flow = new PaymentTransactionFlowEntity();
            flow.setFlowNo(flowNo);
            flow.setBusinessOrderId(refundOrder.getBusinessOrderId());
            flow.setPaymentOrderId(refundOrder.getPaymentOrderId());
            flow.setRefundOrderId(refundOrder.getId());
            flow.setFlowType("REFUND_SUCCESS");
            flow.setAmount(refundOrder.getRefundAmount());
            flow.setTenantId(tenantId);
            transactionFlowMapper.insert(flow);
            refundOrder.setStatus(targetStatus);
            refundOrder.setRefundTime(queryTime);
            refundOrder.setFlowNo(flowNo);
        } else if (PaymentRefundOrderStatusEnum.FAILED.getCode().equals(targetStatus)) {
            exceptionOrderService.createIfAbsent(
                    tenantId,
                    refundOrder.getRefundOrderNo(),
                    PaymentExceptionOrderService.TYPE_REFUND_MISMATCH,
                    PaymentExceptionOrderService.SEVERITY_HIGH,
                    "主动查退款发现通道退款失败，退款订单已失败并等待人工核对退款结果",
                    queryTime);
        }
        if (PaymentRefundOrderStatusEnum.SUCCESS.getCode().equals(targetStatus)
                || PaymentRefundOrderStatusEnum.FAILED.getCode().equals(targetStatus)) {
            refundOrder.setStatus(targetStatus);
            notifyRefundTerminal(tenantId, refundOrder);
        }
        QueryRecordSummary summary = recordRefundQuery(
                tenantId,
                refundOrder,
                currentStatus,
                targetStatus,
                "UPDATED",
                "已按通道退款查询结果推进退款订单状态",
                channelResponse,
                queryTime);
        return new RefundSyncResult(
                refundOrder.getRefundOrderNo(),
                targetStatus,
                flowNo,
                true,
                summary.queryCount(),
                summary.lastQueryResult());
    }

    private RefundSyncResult handleConcurrentRefundSyncResult(
            Long tenantId,
            PaymentRefundOrderVO originalRefundOrder,
            String beforeStatus,
            String targetStatus,
            IPaymentChannelAdapter.RefundQueryResult channelResponse,
            LocalDateTime queryTime) {
        PaymentRefundOrderVO latest = refundOrderMapper.selectByTenantAndRefundOrderNo(
                tenantId, originalRefundOrder.getRefundOrderNo());
        Require.notNull(latest, PaymentCode.PAYMENT_REFUND_ORDER_NOT_FOUND);
        String latestStatus = normalizeRefundStatus(latest.getStatus());
        if (targetStatus.equals(latestStatus)) {
            QueryRecordSummary summary = recordRefundQuery(
                    tenantId,
                    latest,
                    beforeStatus,
                    targetStatus,
                    "IDEMPOTENT_TERMINAL",
                    "并发重复查退款已由其它请求推进到相同终态",
                    channelResponse,
                    queryTime);
            return refundResult(latest, targetStatus, false, summary);
        }
        exceptionOrderService.createIfAbsent(
                tenantId,
                originalRefundOrder.getRefundOrderNo(),
                PaymentExceptionOrderService.TYPE_REFUND_MISMATCH,
                PaymentExceptionOrderService.SEVERITY_HIGH,
                "主动查退款并发处理后本地状态与通道目标状态不一致",
                queryTime);
        Require.isTrue(false, PaymentCode.PAYMENT_REFUND_ORDER_STATE_INVALID.getCode(), "退款订单状态已变化，请刷新后重试");
        return refundResult(latest, latestStatus, false);
    }

    private <T> T inTransaction(Supplier<T> action) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        return template.execute(status -> action.get());
    }

    private void logSummary(PaymentOrderEntity order, String status, long startedAt, String result) {
        observabilityService.logSummary("CHANNEL_PAYMENT_QUERY", order.getPayOrderNo(), status,
                order.getAmount(), order.getChannelCode(), elapsedMillis(startedAt), result);
    }

    private void logRefundSummary(PaymentRefundOrderVO refundOrder, String status, long startedAt, String result) {
        observabilityService.logSummary("CHANNEL_REFUND_QUERY", refundOrder.getRefundOrderNo(), status,
                refundOrder.getRefundAmount(), refundOrder.getChannelCode(), elapsedMillis(startedAt), result);
    }

    private void notifyPaymentTerminal(Long tenantId, Long paymentOrderId, Long businessOrderId) {
        PaymentBusinessOrderEntity businessOrder = businessOrderMapper.selectCashierBusinessOrder(tenantId, businessOrderId);
        Require.notNull(businessOrder, PaymentCode.PAYMENT_BUSINESS_ORDER_NOT_FOUND);
        PaymentApplication application = applicationMapper.selectOne(new LambdaQueryWrapper<PaymentApplication>()
                .eq(PaymentApplication::getTenantId, tenantId)
                .eq(PaymentApplication::getAppId, businessOrder.getAppCode()));
        Require.notNull(application, PaymentCode.PAYMENT_APPLICATION_NOT_FOUND);
        PaymentOrderVO paymentOrder = paymentOrderMapper.selectPaymentOrderById(tenantId, paymentOrderId);
        Require.notNull(paymentOrder, PaymentCode.PAYMENT_ORDER_NOT_FOUND);
        notificationService.notifyPaymentAfterCommit(application, businessOrder, paymentOrder);
    }

    private String selectBusinessOrderNo(Long tenantId, Long businessOrderId) {
        PaymentBusinessOrderEntity businessOrder = businessOrderMapper.selectCashierBusinessOrder(tenantId, businessOrderId);
        Require.notNull(businessOrder, PaymentCode.PAYMENT_BUSINESS_ORDER_NOT_FOUND);
        return businessOrder.getBizOrderNo();
    }

    private IPaymentChannelAdapter.PaymentQueryResult queryTargetStatus(Long tenantId, PaymentOrderEntity order) {
        return channelAdapterRegistry.requireAdapter(order.getChannelCode())
                .queryPayment(new IPaymentChannelAdapter.PaymentQueryCommand(tenantId, order));
    }

    private IPaymentChannelAdapter.RefundQueryResult queryTargetStatus(Long tenantId, PaymentRefundOrderVO refundOrder) {
        return channelAdapterRegistry.requireAdapter(refundOrder.getChannelCode())
                .queryRefund(new IPaymentChannelAdapter.RefundQueryCommand(tenantId, refundOrder));
    }

    private void notifyRefundTerminal(Long tenantId, PaymentRefundOrderVO refundOrder) {
        PaymentApplication application = applicationMapper.selectOne(new LambdaQueryWrapper<PaymentApplication>()
                .eq(PaymentApplication::getTenantId, tenantId)
                .eq(PaymentApplication::getAppId, refundOrder.getAppId()));
        Require.notNull(application, PaymentCode.PAYMENT_APPLICATION_NOT_FOUND);
        PaymentBusinessOrderEntity businessOrder = businessOrderMapper.selectCashierBusinessOrder(tenantId, refundOrder.getBusinessOrderId());
        Require.notNull(businessOrder, PaymentCode.PAYMENT_BUSINESS_ORDER_NOT_FOUND);
        notificationService.notifyRefundAfterCommit(application, businessOrder, refundOrder);
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
        return "PROCESSING".equals(status) ? PaymentRefundOrderStatusEnum.REFUNDING.getCode() : status;
    }

    private int updatePayingQueryResult(Long tenantId, PaymentOrderEntity order, String targetStatus, LocalDateTime payTime) {
        try {
            return paymentOrderMapper.updatePayingQueryResult(
                    tenantId,
                    order.getId(),
                    targetStatus,
                    PaymentOrderStatusEnum.SUCCESS.getCode().equals(targetStatus) ? 1 : 0,
                    payTime);
        } catch (DuplicateKeyException ex) {
            return -1;
        }
    }

    private String selectPaymentFlowNo(Long tenantId, Long paymentOrderId) {
        if (paymentOrderId == null) {
            return null;
        }
        return paymentOrderMapper.selectLatestFlowNo(tenantId, paymentOrderId);
    }

    private PaymentSyncResult paymentResult(String payOrderNo, String status, String flowNo, boolean changed) {
        QueryRecordSummary emptySummary = new QueryRecordSummary(0L, null);
        return paymentResult(payOrderNo, status, flowNo, changed, emptySummary);
    }

    private PaymentSyncResult paymentResult(String payOrderNo, String status, String flowNo, boolean changed, QueryRecordSummary summary) {
        return new PaymentSyncResult(payOrderNo, status, flowNo, changed, summary.queryCount(), summary.lastQueryResult());
    }

    private QueryRecordSummary recordQuery(
            Long tenantId,
            PaymentOrderEntity order,
            String beforeStatus,
            String resultStatus,
            String processResult,
            String processMessage,
            IPaymentChannelAdapter.PaymentQueryResult channelResponse,
            LocalDateTime queryTime) {
        PaymentChannelQueryRecordEntity record = new PaymentChannelQueryRecordEntity();
        record.setQueryNo(numberService.next(PaymentNumberService.PAY_QUERY_NO));
        record.setPayOrderNo(order.getPayOrderNo());
        record.setChannelTradeNo(order.getChannelTradeNo());
        record.setPaymentOrderId(order.getId());
        record.setBusinessOrderId(order.getBusinessOrderId());
        record.setChannelId(order.getChannelId());
        record.setContractId(order.getContractId());
        record.setQueryType("ACTIVE_QUERY");
        record.setRequestPayload(writeSummary(Map.of(
                "payOrderNo", order.getPayOrderNo(),
                "channelTradeNo", order.getChannelTradeNo() == null ? "" : order.getChannelTradeNo(),
                "contractId", order.getContractId() == null ? "" : String.valueOf(order.getContractId()))));
        record.setResponsePayload(writeSummary(Map.of(
                "scenario", channelResponse == null || channelResponse.scenario() == null ? "" : channelResponse.scenario(),
                "returnCode", channelResponse == null ? "" : channelResponse.returnCode(),
                "resultType", channelResponse == null ? "" : channelResponse.resultType(),
                "channelStatus", channelResponse == null ? resultStatus : channelResponse.status())));
        record.setBeforeStatus(beforeStatus);
        record.setChannelStatus(channelResponse == null ? resultStatus : channelResponse.status());
        record.setResultStatus(resultStatus);
        record.setProcessResult(processResult);
        record.setProcessMessage(processMessage);
        record.setQueryTime(queryTime);
        record.setTenantId(tenantId);
        record.setCreatedBy(PaymentContextSupport.currentUserId());
        record.setCreatedAt(queryTime);
        record.setUpdatedBy(PaymentContextSupport.currentUserId());
        record.setUpdatedAt(queryTime);
        channelQueryRecordMapper.insert(record);
        long queryCount = channelQueryRecordMapper.countByTenantAndPayOrderNo(tenantId, order.getPayOrderNo());
        PaymentChannelQueryRecordEntity lastRecord = channelQueryRecordMapper.selectLastByTenantAndPayOrderNo(tenantId, order.getPayOrderNo());
        return new QueryRecordSummary(queryCount, lastRecord == null ? processResult : lastRecord.getProcessResult());
    }

    private QueryRecordSummary recordRefundQuery(
            Long tenantId,
            PaymentRefundOrderVO refundOrder,
            String beforeStatus,
            String resultStatus,
            String processResult,
            String processMessage,
            IPaymentChannelAdapter.RefundQueryResult channelResponse,
            LocalDateTime queryTime) {
        PaymentRefundQueryRecordEntity record = new PaymentRefundQueryRecordEntity();
        record.setQueryNo(numberService.next(PaymentNumberService.PAY_REFUND_QUERY_NO));
        record.setRefundOrderNo(refundOrder.getRefundOrderNo());
        record.setBizRefundNo(refundOrder.getBizRefundNo());
        record.setPayOrderNo(refundOrder.getPayOrderNo());
        record.setChannelRefundNo(refundOrder.getChannelRefundNo());
        record.setRefundOrderId(refundOrder.getId());
        record.setPaymentOrderId(refundOrder.getPaymentOrderId());
        record.setBusinessOrderId(refundOrder.getBusinessOrderId());
        record.setQueryType("REFUND_QUERY");
        record.setRequestPayload(writeSummary(Map.of(
                "refundOrderNo", refundOrder.getRefundOrderNo(),
                "channelRefundNo", refundOrder.getChannelRefundNo() == null ? "" : refundOrder.getChannelRefundNo())));
        record.setResponsePayload(writeSummary(Map.of(
                "scenario", channelResponse == null || channelResponse.scenario() == null ? "" : channelResponse.scenario(),
                "returnCode", channelResponse == null ? "" : channelResponse.returnCode(),
                "resultType", channelResponse == null ? "" : channelResponse.resultType(),
                "channelStatus", channelResponse == null ? resultStatus : channelResponse.status())));
        record.setBeforeStatus(beforeStatus);
        record.setChannelStatus(channelResponse == null ? resultStatus : channelResponse.status());
        record.setResultStatus(resultStatus);
        record.setProcessResult(processResult);
        record.setProcessMessage(processMessage);
        record.setQueryTime(queryTime);
        record.setTenantId(tenantId);
        record.setCreatedBy(PaymentContextSupport.currentUserId());
        record.setCreatedAt(queryTime);
        record.setUpdatedBy(PaymentContextSupport.currentUserId());
        record.setUpdatedAt(queryTime);
        refundQueryRecordMapper.insert(record);
        long queryCount = refundQueryRecordMapper.countByTenantAndRefundOrderNo(tenantId, refundOrder.getRefundOrderNo());
        PaymentRefundQueryRecordEntity lastRecord = refundQueryRecordMapper.selectLastByTenantAndRefundOrderNo(tenantId, refundOrder.getRefundOrderNo());
        return new QueryRecordSummary(queryCount, lastRecord == null ? processResult : lastRecord.getProcessResult());
    }

    private RefundSyncResult refundResult(PaymentRefundOrderVO refundOrder, String status, boolean changed, QueryRecordSummary summary) {
        return new RefundSyncResult(
                refundOrder.getRefundOrderNo(),
                status,
                refundOrder.getFlowNo(),
                changed,
                summary.queryCount(),
                summary.lastQueryResult());
    }

    private RefundSyncResult refundResult(PaymentRefundOrderVO refundOrder, String status, boolean changed) {
        return refundResult(refundOrder, status, changed, new QueryRecordSummary(0L, null));
    }

    private String writeSummary(Map<String, String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException ex) {
            throw new BizException(PaymentCode.PAYMENT_ORDER_STATE_INVALID.getCode(), "查单记录摘要生成失败", ex);
        }
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    private record QueryRecordSummary(long queryCount, String lastQueryResult) {
    }

    public record PaymentSyncResult(String payOrderNo, String status, String flowNo, boolean changed, long queryCount, String lastQueryResult) {
    }

    public record RefundSyncResult(String refundOrderNo, String status, String flowNo, boolean changed, long queryCount, String lastQueryResult) {
    }
}
