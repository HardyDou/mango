package io.mango.payment.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.exception.BizException;
import io.mango.common.result.Require;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.enums.PaymentBusinessOrderStatusEnum;
import io.mango.payment.api.enums.PaymentRefundOrderStatusEnum;
import io.mango.payment.api.vo.PaymentRefundOrderVO;
import io.mango.payment.core.entity.PaymentApplication;
import io.mango.payment.core.entity.PaymentBusinessOrderEntity;
import io.mango.payment.core.entity.PaymentRefundQueryRecordEntity;
import io.mango.payment.core.entity.PaymentTransactionFlowEntity;
import io.mango.payment.core.mapper.PaymentApplicationMapper;
import io.mango.payment.core.mapper.PaymentBusinessOrderMapper;
import io.mango.payment.core.mapper.PaymentRefundOrderMapper;
import io.mango.payment.core.mapper.PaymentRefundQueryRecordMapper;
import io.mango.payment.core.mapper.PaymentTransactionFlowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentChannelRefundQueryService {

    private final PaymentRefundOrderMapper refundOrderMapper;
    private final PaymentRefundQueryRecordMapper refundQueryRecordMapper;
    private final PaymentBusinessOrderMapper businessOrderMapper;
    private final PaymentTransactionFlowMapper transactionFlowMapper;
    private final PaymentOrderStateService orderStateService;
    private final PaymentApplicationMapper applicationMapper;
    private final PaymentNotificationService notificationService;
    private final PaymentOrderStatusFlowService statusFlowService;
    private final PaymentChannelAdapterRegistry channelAdapterRegistry;
    private final ObjectMapper objectMapper;
    private final PaymentDuplicateRefundCompletionService duplicateRefundCompletionService;
    private final PaymentObservabilityService observabilityService;
    private final PaymentExceptionOrderService exceptionOrderService;
    private final PaymentNumberService numberService;

    @Transactional(rollbackFor = Exception.class)
    public QueryResult queryChannelRefund(String refundOrderNo) {
        Require.notBlank(refundOrderNo, PaymentCode.PAYMENT_REFUND_ORDER_INVALID.getCode(), "退款订单号不能为空");
        Long tenantId = PaymentContextSupport.currentTenantId();
        PaymentRefundOrderVO refundOrder = refundOrderMapper.selectByTenantAndRefundOrderNo(tenantId, refundOrderNo);
        Require.notNull(refundOrder, PaymentCode.PAYMENT_REFUND_ORDER_NOT_FOUND);
        return queryRefund(tenantId, refundOrder);
    }

    @Transactional(rollbackFor = Exception.class)
    public QueryResult queryChannelRefundByChannelRefundNo(String channelRefundNo) {
        Require.notBlank(channelRefundNo, PaymentCode.PAYMENT_REFUND_ORDER_INVALID.getCode(), "通道退款单号不能为空");
        Long tenantId = PaymentContextSupport.currentTenantId();
        PaymentRefundOrderVO refundOrder = refundOrderMapper.selectByTenantAndChannelRefundNo(tenantId, channelRefundNo);
        Require.notNull(refundOrder, PaymentCode.PAYMENT_REFUND_ORDER_NOT_FOUND);
        return queryRefund(tenantId, refundOrder);
    }

    private QueryResult queryRefund(Long tenantId, PaymentRefundOrderVO refundOrder) {
        long startedAt = System.nanoTime();
        String currentStatus = normalizeRefundStatus(refundOrder.getStatus());
        LocalDateTime queryTime = LocalDateTime.now();
        if (isTerminal(currentStatus)) {
            QueryRecordSummary summary = recordQuery(
                    tenantId,
                    refundOrder,
                    currentStatus,
                    currentStatus,
                    "NO_QUERY_TERMINAL",
                    "本地退款订单已是终态，主动查询不再覆盖本地终态",
                    null,
                    queryTime);
            logSummary(refundOrder, currentStatus, startedAt, "NO_QUERY_TERMINAL");
            return result(refundOrder, currentStatus, false, summary);
        }
        Require.isTrue(PaymentRefundOrderStatusEnum.REFUNDING.getCode().equals(currentStatus),
                PaymentCode.PAYMENT_REFUND_ORDER_STATE_INVALID.getCode(), "只有退款中的订单允许主动查询推进");

        IPaymentChannelAdapter.RefundQueryResult channelResponse = queryTargetStatus(tenantId, refundOrder);
        String targetStatus = channelResponse.status();
        if (PaymentRefundOrderStatusEnum.REFUNDING.getCode().equals(targetStatus)) {
            QueryRecordSummary summary = recordQuery(
                    tenantId,
                    refundOrder,
                    currentStatus,
                    targetStatus,
                    "NO_CHANGE_PROCESSING",
                    "通道返回退款处理中，本地退款订单保持退款中",
                    channelResponse,
                    queryTime);
            logSummary(refundOrder, currentStatus, startedAt, "NO_CHANGE_PROCESSING");
            return result(refundOrder, currentStatus, false, summary);
        }

        orderStateService.requireRefundTransition(currentStatus, targetStatus);
        LocalDateTime refundTime = PaymentRefundOrderStatusEnum.SUCCESS.getCode().equals(targetStatus) ? queryTime : null;
        int updated = refundOrderMapper.updateRefundingQueryResult(tenantId, refundOrder.getId(), targetStatus, refundTime);
        Require.isTrue(updated == 1, PaymentCode.PAYMENT_REFUND_ORDER_STATE_INVALID.getCode(), "退款订单状态已变化，请刷新后重试");
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
        QueryRecordSummary summary = recordQuery(
                tenantId,
                refundOrder,
                currentStatus,
                targetStatus,
                "UPDATED",
                "已按通道退款查询结果推进退款订单状态",
                channelResponse,
                queryTime);
        logSummary(refundOrder, targetStatus, startedAt, "UPDATED");
        return new QueryResult(
                refundOrder.getRefundOrderNo(),
                targetStatus,
                flowNo,
                true,
                summary.queryCount(),
                summary.lastQueryResult());
    }

    private void logSummary(PaymentRefundOrderVO refundOrder, String status, long startedAt, String result) {
        observabilityService.logSummary("CHANNEL_REFUND_QUERY", refundOrder.getRefundOrderNo(), status,
                refundOrder.getRefundAmount(), refundOrder.getChannelCode(), elapsedMillis(startedAt), result);
    }

    private IPaymentChannelAdapter.RefundQueryResult queryTargetStatus(Long tenantId, PaymentRefundOrderVO refundOrder) {
        return channelAdapterRegistry.requireAdapter(refundOrder.getChannelCode())
                .queryRefund(new IPaymentChannelAdapter.RefundQueryCommand(tenantId, refundOrder));
    }

    private void notifyRefundTerminal(Long tenantId, PaymentRefundOrderVO refundOrder) {
        PaymentApplication application = applicationMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PaymentApplication>()
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

    private QueryRecordSummary recordQuery(
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

    private QueryResult result(PaymentRefundOrderVO refundOrder, String status, boolean changed, QueryRecordSummary summary) {
        return new QueryResult(
                refundOrder.getRefundOrderNo(),
                status,
                refundOrder.getFlowNo(),
                changed,
                summary.queryCount(),
                summary.lastQueryResult());
    }

    private String writeSummary(Map<String, String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException ex) {
            throw new BizException(PaymentCode.PAYMENT_REFUND_ORDER_STATE_INVALID.getCode(), "退款查询记录摘要生成失败", ex);
        }
    }

    private boolean isTerminal(String status) {
        return PaymentRefundOrderStatusEnum.SUCCESS.getCode().equals(status)
                || PaymentRefundOrderStatusEnum.FAILED.getCode().equals(status)
                || PaymentRefundOrderStatusEnum.CLOSED.getCode().equals(status);
    }

    private String normalizeRefundStatus(String status) {
        return "PROCESSING".equals(status) ? PaymentRefundOrderStatusEnum.REFUNDING.getCode() : status;
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    private record QueryRecordSummary(long queryCount, String lastQueryResult) {
    }

    public record QueryResult(String refundOrderNo, String status, String flowNo, boolean changed, long queryCount, String lastQueryResult) {
    }
}
