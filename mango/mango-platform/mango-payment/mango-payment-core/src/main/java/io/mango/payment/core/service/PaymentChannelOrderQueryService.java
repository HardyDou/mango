package io.mango.payment.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.exception.BizException;
import io.mango.common.result.Require;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.enums.PaymentBusinessOrderStatusEnum;
import io.mango.payment.api.enums.PaymentOrderStatusEnum;
import io.mango.payment.api.vo.PaymentOrderVO;
import io.mango.payment.core.entity.PaymentApplication;
import io.mango.payment.core.entity.PaymentBusinessOrderEntity;
import io.mango.payment.core.entity.PaymentChannelQueryRecordEntity;
import io.mango.payment.core.entity.PaymentOrderEntity;
import io.mango.payment.core.entity.PaymentTransactionFlowEntity;
import io.mango.payment.core.mapper.PaymentApplicationMapper;
import io.mango.payment.core.mapper.PaymentBusinessOrderMapper;
import io.mango.payment.core.mapper.PaymentChannelQueryRecordMapper;
import io.mango.payment.core.mapper.PaymentOrderMapper;
import io.mango.payment.core.mapper.PaymentTransactionFlowMapper;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentChannelOrderQueryService {

    private final PaymentOrderMapper paymentOrderMapper;
    private final PaymentBusinessOrderMapper businessOrderMapper;
    private final PaymentApplicationMapper applicationMapper;
    private final PaymentTransactionFlowMapper transactionFlowMapper;
    private final PaymentChannelQueryRecordMapper channelQueryRecordMapper;
    private final PaymentOrderStateService orderStateService;
    private final PaymentNotificationService notificationService;
    private final PaymentOrderStatusFlowService statusFlowService;
    private final PaymentDuplicatePaymentService duplicatePaymentService;
    private final PaymentChannelAdapterRegistry channelAdapterRegistry;
    private final ObjectMapper objectMapper;
    private final PaymentObservabilityService observabilityService;
    private final PaymentExceptionOrderService exceptionOrderService;
    private final PaymentNumberService numberService;

    @Transactional(rollbackFor = Exception.class)
    public QueryResult queryChannelPayment(String payOrderNo) {
        long startedAt = System.nanoTime();
        Require.notBlank(payOrderNo, PaymentCode.PAYMENT_CASHIER_PAY_INVALID.getCode(), "支付订单号不能为空");
        Long tenantId = PaymentContextSupport.currentTenantId();
        PaymentOrderEntity order = paymentOrderMapper.selectByTenantAndPayOrderNo(tenantId, payOrderNo);
        Require.notNull(order, PaymentCode.PAYMENT_ORDER_NOT_FOUND);
        String currentStatus = order.getStatus();
        LocalDateTime queryTime = LocalDateTime.now();
        if (isTerminal(currentStatus)) {
            QueryRecordSummary summary = recordQuery(
                    tenantId,
                    order,
                    currentStatus,
                    currentStatus,
                    "NO_QUERY_TERMINAL",
                    "本地支付订单已是终态，主动查单不再覆盖本地终态",
                    null,
                    queryTime);
            logSummary(order, currentStatus, startedAt, "NO_QUERY_TERMINAL");
            return result(order.getPayOrderNo(), currentStatus, selectFlowNo(tenantId, order.getId()), false, summary);
        }
        Require.isTrue(PaymentOrderStatusEnum.PAYING.getCode().equals(currentStatus),
                PaymentCode.PAYMENT_ORDER_STATE_INVALID.getCode(), "只有支付中的订单允许主动查单推进");

        IPaymentChannelAdapter.PaymentQueryResult channelResponse = queryTargetStatus(tenantId, order);
        String targetStatus = channelResponse.status();
        if (PaymentOrderStatusEnum.PAYING.getCode().equals(targetStatus)) {
            QueryRecordSummary summary = recordQuery(
                    tenantId,
                    order,
                    currentStatus,
                    targetStatus,
                    "NO_CHANGE_PROCESSING",
                    "通道返回处理中，本地支付订单保持支付中",
                    channelResponse,
                    queryTime);
            logSummary(order, currentStatus, startedAt, "NO_CHANGE_PROCESSING");
            return result(order.getPayOrderNo(), currentStatus, selectFlowNo(tenantId, order.getId()), false, summary);
        }

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
            logSummary(order, duplicateResult.status(), startedAt,
                    duplicateResult.refunded() ? "DUPLICATE_REFUNDED" : "DUPLICATE_EXCEPTION");
            return result(order.getPayOrderNo(), duplicateResult.status(), selectFlowNo(tenantId, order.getId()), true, summary);
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
        logSummary(order, targetStatus, startedAt, "UPDATED");
        return result(order.getPayOrderNo(), targetStatus, flowNo, true, summary);
    }

    private void logSummary(PaymentOrderEntity order, String status, long startedAt, String result) {
        observabilityService.logSummary("CHANNEL_PAYMENT_QUERY", order.getPayOrderNo(), status,
                order.getAmount(), order.getChannelCode(), elapsedMillis(startedAt), result);
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

    private boolean isTerminal(String status) {
        return PaymentOrderStatusEnum.SUCCESS.getCode().equals(status)
                || PaymentOrderStatusEnum.FAILED.getCode().equals(status)
                || PaymentOrderStatusEnum.CLOSED.getCode().equals(status);
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

    private String selectFlowNo(Long tenantId, Long paymentOrderId) {
        if (paymentOrderId == null) {
            return null;
        }
        return paymentOrderMapper.selectLatestFlowNo(tenantId, paymentOrderId);
    }

    private QueryResult result(String payOrderNo, String status, String flowNo, boolean changed) {
        QueryRecordSummary emptySummary = new QueryRecordSummary(0L, null);
        return result(payOrderNo, status, flowNo, changed, emptySummary);
    }

    private QueryResult result(String payOrderNo, String status, String flowNo, boolean changed, QueryRecordSummary summary) {
        return new QueryResult(payOrderNo, status, flowNo, changed, summary.queryCount(), summary.lastQueryResult());
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

    public record QueryResult(String payOrderNo, String status, String flowNo, boolean changed, long queryCount, String lastQueryResult) {
    }
}
