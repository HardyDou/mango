package io.mango.payment.core.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.command.HandlePaymentDifferenceCommand;
import io.mango.payment.api.enums.PaymentOrderStatusEnum;
import io.mango.payment.api.enums.PaymentRefundOrderStatusEnum;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentDifferenceActionVO;
import io.mango.payment.api.vo.PaymentDifferenceStatusVO;
import io.mango.payment.api.vo.PaymentDifferenceVO;
import io.mango.payment.core.entity.PaymentDifferenceEntity;
import io.mango.payment.core.entity.PaymentTransactionFlowEntity;
import io.mango.payment.core.mapper.PaymentDifferenceMapper;
import io.mango.payment.core.mapper.PaymentTransactionFlowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PaymentDifferenceService {

    private static final Pattern PAYMENT_ORDER_NO_PATTERN = Pattern.compile("^PO\\d{16}$");
    private static final Pattern REFUND_ORDER_NO_PATTERN = Pattern.compile("^RO\\d{16}$");

    private final PaymentDifferenceMapper differenceMapper;
    private final PaymentTransactionFlowMapper transactionFlowMapper;
    private final PaymentChannelSyncService channelSyncService;
    private final PaymentOperationAuditService auditService;
    private final PaymentNumberService numberService;
    private final PaymentOrderViewSupport viewSupport;

    public PageResult<PaymentDifferenceVO> pageDifferences(PaymentConfigPageQuery query) {
        PaymentConfigPageQuery resolved = query == null ? new PaymentConfigPageQuery() : query;
        String keyword = PaymentContextSupport.trimToNull(resolved.getKeyword());
        String statusCode = PaymentContextSupport.trimToNull(resolved.getStatusCode());
        Long tenantId = PaymentContextSupport.currentTenantId();
        long total = differenceMapper.countDifferences(tenantId, keyword, statusCode);
        long page = resolved.getPage();
        long size = resolved.getSize();
        List<PaymentDifferenceVO> rows = differenceMapper.selectDifferencePage(tenantId, keyword, statusCode, size, (page - 1) * size);
        rows.forEach(this::fillDifferenceSummary);
        return PageResult.of(rows, total, page, size);
    }

    public PaymentDifferenceVO detailDifference(Long id) {
        Require.notNull(id, PaymentCode.PAYMENT_DIFFERENCE_INVALID.getCode(), "对账差异 ID 不能为空");
        PaymentDifferenceVO vo = differenceMapper.selectDifferenceDetail(PaymentContextSupport.currentTenantId(), id);
        Require.notNull(vo, PaymentCode.PAYMENT_DIFFERENCE_NOT_FOUND);
        fillDifferenceSummary(vo);
        return vo;
    }

    public List<PaymentDifferenceStatusVO> listDifferenceStatuses() {
        return List.of(
                differenceStatus("PENDING"),
                differenceStatus("PROCESSING"),
                differenceStatus("HANDLED"),
                differenceStatus("IGNORED"),
                differenceStatus("CLOSED"));
    }

    public List<PaymentDifferenceActionVO> listDifferenceActions() {
        return List.of(
                differenceAction("ACTIVE_QUERY", "主动查单"),
                differenceAction("SUPPLEMENT_ORDER", "挂起待认领"),
                differenceAction("IGNORE", "忽略差异"),
                differenceAction("CLOSE", "关闭差异"));
    }

    public PaymentDifferenceVO handleDifference(HandlePaymentDifferenceCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_DIFFERENCE_INVALID);
        Require.notNull(command.getId(), PaymentCode.PAYMENT_DIFFERENCE_INVALID.getCode(), "对账差异 ID 不能为空");
        String processAction = PaymentContextSupport.trimToNull(command.getProcessAction());
        String processReason = PaymentContextSupport.trimToNull(command.getProcessReason());
        String processResult = PaymentContextSupport.trimToNull(command.getProcessResult());
        String processEvidence = PaymentContextSupport.trimToNull(command.getProcessEvidence());
        Require.notBlank(processAction, PaymentCode.PAYMENT_DIFFERENCE_INVALID.getCode(), "处理动作不能为空");
        Require.notBlank(processReason, PaymentCode.PAYMENT_DIFFERENCE_INVALID.getCode(), "处理原因不能为空");
        Require.notBlank(processResult, PaymentCode.PAYMENT_DIFFERENCE_INVALID.getCode(), "处理结果不能为空");
        Require.isTrue(isSupportedDifferenceAction(processAction), PaymentCode.PAYMENT_DIFFERENCE_INVALID.getCode(), "处理动作不支持");
        Require.isTrue(processAction.length() <= 64, PaymentCode.PAYMENT_DIFFERENCE_INVALID.getCode(), "处理动作不能超过 64 个字符");
        Require.isTrue(processReason.length() <= 512, PaymentCode.PAYMENT_DIFFERENCE_INVALID.getCode(), "处理原因不能超过 512 个字符");
        Require.isTrue(processResult.length() <= 512, PaymentCode.PAYMENT_DIFFERENCE_INVALID.getCode(), "处理结果不能超过 512 个字符");
        Require.isTrue(processEvidence == null || processEvidence.length() <= 512, PaymentCode.PAYMENT_DIFFERENCE_INVALID.getCode(), "处理凭据不能超过 512 个字符");

        Long tenantId = PaymentContextSupport.currentTenantId();
        PaymentDifferenceEntity entity = differenceMapper.selectById(command.getId());
        Require.notNull(entity, PaymentCode.PAYMENT_DIFFERENCE_NOT_FOUND);
        Require.isTrue(tenantId.equals(entity.getTenantId()) && !Integer.valueOf(1).equals(entity.getDelFlag()),
                PaymentCode.PAYMENT_DIFFERENCE_NOT_FOUND);
        Require.isTrue(canHandleDifference(entity.getProcessStatus()),
                PaymentCode.PAYMENT_DIFFERENCE_HANDLE_STATUS_INVALID);
        DifferenceActionResult actionResult = executeDifferenceAction(processAction, entity);
        processResult = mergeProcessResult(processResult, actionResult.message());
        Require.isTrue(processResult.length() <= 512, PaymentCode.PAYMENT_DIFFERENCE_INVALID.getCode(), "处理结果不能超过 512 个字符");

        LocalDateTime now = LocalDateTime.now();
        Long operatorId = PaymentContextSupport.currentUserId();
        PaymentTransactionFlowEntity adjustFlow = createDifferenceAdjustFlow(now, operatorId, tenantId);
        int updated = differenceMapper.handleDifference(
                tenantId,
                command.getId(),
                actionResult.nextStatus(),
                processAction,
                processReason,
                processResult,
                processEvidence,
                adjustFlow.getId(),
                adjustFlow.getFlowNo(),
                operatorId,
                PaymentContextSupport.currentPrincipalName(),
                now);
        Require.isTrue(updated == 1, PaymentCode.PAYMENT_DIFFERENCE_HANDLE_STATUS_INVALID);

        auditService.record(
                PaymentOperationAuditService.ACTION_HANDLE_DIFFERENCE,
                PaymentOperationAuditService.RESOURCE_PAYMENT_DIFFERENCE,
                entity.getDifferenceNo(),
                PaymentOperationAuditService.RESULT_SUCCESS);
        return detailDifference(command.getId());
    }

    private PaymentTransactionFlowEntity createDifferenceAdjustFlow(LocalDateTime now, Long operatorId, Long tenantId) {
        PaymentTransactionFlowEntity flow = new PaymentTransactionFlowEntity();
        flow.setId(IdWorker.getId());
        flow.setFlowNo(numberService.next(PaymentNumberService.PAY_ADJUST_FLOW_NO));
        flow.setBusinessOrderId(null);
        flow.setPaymentOrderId(null);
        flow.setRefundOrderId(null);
        flow.setFlowType("ADJUST_NOTE");
        flow.setAmount(0L);
        flow.setTenantId(tenantId);
        flow.setCreatedBy(operatorId);
        flow.setCreatedAt(now);
        flow.setUpdatedBy(operatorId);
        flow.setUpdatedAt(now);
        transactionFlowMapper.insert(flow);
        return flow;
    }

    private void fillDifferenceSummary(PaymentDifferenceVO vo) {
        vo.setDifferenceTypeName(differenceTypeName(vo.getDifferenceType()));
        vo.setProcessStatusName(differenceStatusName(vo.getProcessStatus()));
    }

    private boolean canHandleDifference(String processStatus) {
        return "PENDING".equals(processStatus) || "PROCESSING".equals(processStatus);
    }

    private boolean isSupportedDifferenceAction(String processAction) {
        return "ACTIVE_QUERY".equals(processAction)
                || "SUPPLEMENT_ORDER".equals(processAction)
                || "IGNORE".equals(processAction)
                || "CLOSE".equals(processAction);
    }

    private DifferenceActionResult executeDifferenceAction(String processAction, PaymentDifferenceEntity entity) {
        if ("ACTIVE_QUERY".equals(processAction)) {
            return executeDifferenceActiveQuery(entity);
        }
        return new DifferenceActionResult(resolveDifferenceNextStatus(processAction), null);
    }

    private DifferenceActionResult executeDifferenceActiveQuery(PaymentDifferenceEntity entity) {
        String relatedOrderNo = PaymentContextSupport.trimToNull(entity.getRelatedOrderNo());
        Require.notBlank(relatedOrderNo, PaymentCode.PAYMENT_DIFFERENCE_INVALID.getCode(), "主动查单需要关联订单号");
        if (isPaymentOrderNo(relatedOrderNo)) {
            PaymentChannelSyncService.PaymentSyncResult queryResult = channelSyncService.syncPaymentStatus(relatedOrderNo);
            return new DifferenceActionResult(
                    paymentQueryNextStatus(queryResult.status()),
                    "主动查单结果：支付订单 " + queryResult.payOrderNo()
                            + " 当前状态 " + queryResult.status()
                            + (queryResult.changed() ? "，已按通道结果推进" : "，本地状态未变化"));
        }
        if (isRefundOrderNo(relatedOrderNo)) {
            PaymentChannelSyncService.RefundSyncResult queryResult = channelSyncService.syncRefundStatus(relatedOrderNo);
            return new DifferenceActionResult(
                    refundQueryNextStatus(queryResult.status()),
                    "主动查退款结果：退款订单 " + queryResult.refundOrderNo()
                            + " 当前状态 " + queryResult.status()
                            + (queryResult.changed() ? "，已按通道结果推进" : "，本地状态未变化"));
        }
        Require.isTrue(false, PaymentCode.PAYMENT_DIFFERENCE_INVALID.getCode(), "主动查单需要关联本地支付单号或退款单号");
        return new DifferenceActionResult("PROCESSING", null);
    }

    private boolean isPaymentOrderNo(String orderNo) {
        return orderNo != null && PAYMENT_ORDER_NO_PATTERN.matcher(orderNo).matches();
    }

    private boolean isRefundOrderNo(String orderNo) {
        return orderNo != null && REFUND_ORDER_NO_PATTERN.matcher(orderNo).matches();
    }

    private String paymentQueryNextStatus(String paymentStatus) {
        if (PaymentOrderStatusEnum.PAYING.getCode().equals(paymentStatus)) {
            return "PROCESSING";
        }
        return "HANDLED";
    }

    private String refundQueryNextStatus(String refundStatus) {
        if (PaymentRefundOrderStatusEnum.REFUNDING.getCode().equals(viewSupport.normalizeRefundStatus(refundStatus))) {
            return "PROCESSING";
        }
        return "HANDLED";
    }

    private String mergeProcessResult(String processResult, String actionMessage) {
        if (!StringUtils.hasText(actionMessage)) {
            return processResult;
        }
        return processResult + "；" + actionMessage;
    }

    private String resolveDifferenceNextStatus(String processAction) {
        if ("IGNORE".equals(processAction)) {
            return "IGNORED";
        }
        if ("CLOSE".equals(processAction)) {
            return "CLOSED";
        }
        if ("SUPPLEMENT_ORDER".equals(processAction)) {
            return "PROCESSING";
        }
        return "HANDLED";
    }

    private String differenceTypeName(String differenceType) {
        if ("LOCAL_SUCCESS_CHANNEL_MISSING".equals(differenceType)) {
            return "我方成功通道无单";
        }
        if ("CHANNEL_SUCCESS_LOCAL_MISSING".equals(differenceType)) {
            return "通道成功我方无单";
        }
        if ("AMOUNT_MISMATCH".equals(differenceType)) {
            return "金额不一致";
        }
        if ("STATUS_MISMATCH".equals(differenceType)) {
            return "状态不一致";
        }
        if ("REFUND_MISMATCH".equals(differenceType)) {
            return "退款不一致";
        }
        if ("FEE_MISMATCH".equals(differenceType)) {
            return "手续费不一致";
        }
        return differenceType;
    }

    private String differenceStatusName(String processStatus) {
        if ("PENDING".equals(processStatus)) {
            return "待处理";
        }
        if ("PROCESSING".equals(processStatus)) {
            return "处理中";
        }
        if ("HANDLED".equals(processStatus)) {
            return "已处理";
        }
        if ("IGNORED".equals(processStatus)) {
            return "已忽略";
        }
        if ("CLOSED".equals(processStatus)) {
            return "已关闭";
        }
        return processStatus;
    }

    private PaymentDifferenceStatusVO differenceStatus(String statusCode) {
        PaymentDifferenceStatusVO vo = new PaymentDifferenceStatusVO();
        vo.setStatusCode(statusCode);
        vo.setStatusName(differenceStatusName(statusCode));
        return vo;
    }

    private PaymentDifferenceActionVO differenceAction(String actionCode, String actionName) {
        PaymentDifferenceActionVO vo = new PaymentDifferenceActionVO();
        vo.setActionCode(actionCode);
        vo.setActionName(actionName);
        return vo;
    }

    private record DifferenceActionResult(String nextStatus, String message) {
    }
}
