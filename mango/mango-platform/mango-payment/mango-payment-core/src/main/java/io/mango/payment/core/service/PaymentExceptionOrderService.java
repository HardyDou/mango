package io.mango.payment.core.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.command.HandlePaymentExceptionOrderCommand;
import io.mango.payment.api.enums.PaymentRefundOrderStatusEnum;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.core.entity.PaymentExceptionOrderEntity;
import io.mango.payment.core.mapper.PaymentExceptionOrderMapper;
import io.mango.payment.api.vo.PaymentExceptionOrderActionVO;
import io.mango.payment.api.vo.PaymentExceptionOrderStatusVO;
import io.mango.payment.api.vo.PaymentExceptionOrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PaymentExceptionOrderService {

    public static final String TYPE_DUPLICATE_PAYMENT = "DUPLICATE_PAYMENT";
    public static final String TYPE_PAY_TIMEOUT = "PAY_TIMEOUT";
    public static final String TYPE_CHANNEL_FAILED = "CHANNEL_FAILED";
    public static final String TYPE_REFUND_MISMATCH = "REFUND_MISMATCH";
    public static final String TYPE_CHANNEL_CALLBACK_FAILED = "CHANNEL_CALLBACK_FAILED";
    public static final String TYPE_AMOUNT_MISMATCH = "AMOUNT_MISMATCH";
    public static final String TYPE_STATUS_MISMATCH = "STATUS_MISMATCH";

    public static final String SEVERITY_MEDIUM = "MEDIUM";
    public static final String SEVERITY_HIGH = "HIGH";

    private static final String HANDLE_STATUS_PENDING = "PENDING";
    private static final Pattern PAYMENT_ORDER_NO_PATTERN = Pattern.compile("^PO\\d{16}$");
    private static final Pattern REFUND_ORDER_NO_PATTERN = Pattern.compile("^RO\\d{16}$");
    private static final Set<String> EXCEPTION_ORDER_HANDLE_ACTIONS = Set.of(
            "ACTIVE_QUERY",
            "ACTIVE_REFUND_QUERY",
            "CLOSE_PAYMENT_ORDER",
            "ADD_EVIDENCE",
            "MANUAL_CLOSE");
    private static final Set<String> PAYMENT_EXCEPTION_TYPES = Set.of(
            TYPE_DUPLICATE_PAYMENT,
            TYPE_PAY_TIMEOUT,
            TYPE_CHANNEL_FAILED);

    private final PaymentExceptionOrderMapper exceptionOrderMapper;
    private final PaymentNumberService numberService;
    private final PaymentOperationAuditService auditService;
    private final PaymentChannelSyncService channelSyncService;
    private final PaymentChannelOrderCloseService channelOrderCloseService;

    public PageResult<PaymentExceptionOrderVO> pageExceptionOrders(PaymentConfigPageQuery query) {
        PaymentConfigPageQuery resolved = query == null ? new PaymentConfigPageQuery() : query;
        String keyword = PaymentContextSupport.trimToNull(resolved.getKeyword());
        String statusCode = PaymentContextSupport.trimToNull(resolved.getStatusCode());
        Long tenantId = PaymentContextSupport.currentTenantId();
        long total = exceptionOrderMapper.countExceptionOrders(tenantId, keyword, statusCode);
        long page = resolved.getPage();
        long size = resolved.getSize();
        List<PaymentExceptionOrderVO> rows = exceptionOrderMapper.selectExceptionOrderPage(tenantId, keyword, statusCode, size, (page - 1) * size);
        rows.forEach(this::fillExceptionOrderSummary);
        return PageResult.of(rows, total, page, size);
    }

    public PaymentExceptionOrderVO detailExceptionOrder(Long id) {
        Require.notNull(id, PaymentCode.PAYMENT_EXCEPTION_ORDER_INVALID.getCode(), "异常订单 ID 不能为空");
        PaymentExceptionOrderVO vo = exceptionOrderMapper.selectExceptionOrderDetail(PaymentContextSupport.currentTenantId(), id);
        Require.notNull(vo, PaymentCode.PAYMENT_EXCEPTION_ORDER_NOT_FOUND);
        fillExceptionOrderSummary(vo);
        return vo;
    }

    public List<PaymentExceptionOrderStatusVO> listExceptionOrderStatuses() {
        return List.of(
                exceptionOrderStatus("PENDING"),
                exceptionOrderStatus("PROCESSING"),
                exceptionOrderStatus("HANDLED"),
                exceptionOrderStatus("IGNORED"),
                exceptionOrderStatus("CLOSED"));
    }

    public List<PaymentExceptionOrderActionVO> listExceptionOrderActions() {
        return List.of(
                exceptionOrderAction("ACTIVE_QUERY", "主动查单",
                        List.copyOf(PAYMENT_EXCEPTION_TYPES),
                        "仅适用于关联支付订单的异常，系统会向通道查单并按通道结果推进支付订单状态"),
                exceptionOrderAction("CLOSE_PAYMENT_ORDER", "关闭支付订单",
                        List.copyOf(PAYMENT_EXCEPTION_TYPES),
                        "仅适用于关联支付订单的异常，系统会关闭未支付或支付中的支付订单和业务订单"),
                exceptionOrderAction("ACTIVE_REFUND_QUERY", "主动查退款",
                        List.of(TYPE_REFUND_MISMATCH),
                        "仅适用于关联退款订单的异常，系统会向通道查退款并按通道结果推进退款订单状态"),
                exceptionOrderAction("ADD_EVIDENCE", "补充凭据",
                        allExceptionTypes(),
                        "记录人工核对材料，异常单进入处理中，不直接修改支付或退款状态"),
                exceptionOrderAction("MANUAL_CLOSE", "人工复核关闭",
                        allExceptionTypes(),
                        "人工确认无需系统动作后关闭异常单，不直接修改支付或退款状态"));
    }

    public PaymentExceptionOrderVO handleExceptionOrder(HandlePaymentExceptionOrderCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_EXCEPTION_ORDER_INVALID);
        Require.notNull(command.getId(), PaymentCode.PAYMENT_EXCEPTION_ORDER_INVALID.getCode(), "异常订单 ID 不能为空");
        String handleAction = PaymentContextSupport.trimToNull(command.getHandleAction());
        String handleReason = PaymentContextSupport.trimToNull(command.getHandleReason());
        String handleResult = PaymentContextSupport.trimToNull(command.getHandleResult());
        String handleEvidence = PaymentContextSupport.trimToNull(command.getHandleEvidence());
        Require.notBlank(handleAction, PaymentCode.PAYMENT_EXCEPTION_ORDER_INVALID.getCode(), "处理动作不能为空");
        Require.notBlank(handleReason, PaymentCode.PAYMENT_EXCEPTION_ORDER_INVALID.getCode(), "处理原因不能为空");
        Require.notBlank(handleResult, PaymentCode.PAYMENT_EXCEPTION_ORDER_INVALID.getCode(), "处理结果不能为空");
        Require.isTrue(EXCEPTION_ORDER_HANDLE_ACTIONS.contains(handleAction),
                PaymentCode.PAYMENT_EXCEPTION_ORDER_INVALID.getCode(), "处理动作不受支持");
        Require.isTrue(handleAction.length() <= 64, PaymentCode.PAYMENT_EXCEPTION_ORDER_INVALID.getCode(), "处理动作不能超过 64 个字符");
        Require.isTrue(handleReason.length() <= 512, PaymentCode.PAYMENT_EXCEPTION_ORDER_INVALID.getCode(), "处理原因不能超过 512 个字符");
        Require.isTrue(handleResult.length() <= 512, PaymentCode.PAYMENT_EXCEPTION_ORDER_INVALID.getCode(), "处理结果不能超过 512 个字符");
        Require.isTrue(handleEvidence == null || handleEvidence.length() <= 512, PaymentCode.PAYMENT_EXCEPTION_ORDER_INVALID.getCode(), "处理凭据不能超过 512 个字符");

        Long tenantId = PaymentContextSupport.currentTenantId();
        PaymentExceptionOrderEntity entity = exceptionOrderMapper.selectById(command.getId());
        Require.notNull(entity, PaymentCode.PAYMENT_EXCEPTION_ORDER_NOT_FOUND);
        Require.isTrue(tenantId.equals(entity.getTenantId()) && !Integer.valueOf(1).equals(entity.getDelFlag()),
                PaymentCode.PAYMENT_EXCEPTION_ORDER_NOT_FOUND);
        Require.isTrue(canHandleExceptionOrder(entity.getHandleStatus()),
                PaymentCode.PAYMENT_EXCEPTION_ORDER_HANDLE_STATUS_INVALID);
        Require.isTrue(isActionAllowedForExceptionType(handleAction, entity.getExceptionType()),
                PaymentCode.PAYMENT_EXCEPTION_ORDER_INVALID.getCode(), "处理动作不适用于当前异常类型");
        String nextHandleStatus = nextExceptionOrderHandleStatus(handleAction);
        PaymentChannelSyncService.PaymentSyncResult channelQueryResult = activeQueryPayment(handleAction, entity.getRelatedOrderNo());
        if (channelQueryResult != null) {
            nextHandleStatus = "HANDLED";
            handleResult = handleResult + "；查单结果：支付订单 "
                    + channelQueryResult.payOrderNo()
                    + " 当前状态 "
                    + channelQueryResult.status()
                    + (channelQueryResult.changed() ? "，已按通道结果推进" : "，本地状态未变化");
            Require.isTrue(handleResult.length() <= 512,
                    PaymentCode.PAYMENT_EXCEPTION_ORDER_INVALID.getCode(), "处理结果不能超过 512 个字符");
        }
        PaymentChannelSyncService.RefundSyncResult refundQueryResult = activeQueryRefund(handleAction, entity.getRelatedOrderNo());
        if (refundQueryResult != null) {
            nextHandleStatus = PaymentRefundOrderStatusEnum.REFUNDING.getCode().equals(normalizeRefundStatus(refundQueryResult.status()))
                    ? "PROCESSING"
                    : "HANDLED";
            handleResult = handleResult + "；查退款结果：退款订单 "
                    + refundQueryResult.refundOrderNo()
                    + " 当前状态 "
                    + refundQueryResult.status()
                    + (refundQueryResult.changed() ? "，已按通道结果推进" : "，本地状态未变化");
            Require.isTrue(handleResult.length() <= 512,
                    PaymentCode.PAYMENT_EXCEPTION_ORDER_INVALID.getCode(), "处理结果不能超过 512 个字符");
        }
        PaymentChannelOrderCloseService.CloseResult closeResult = closePaymentOrder(handleAction, entity.getRelatedOrderNo());
        if (closeResult != null) {
            nextHandleStatus = "CLOSED";
            handleResult = handleResult + "；关单结果：支付订单 "
                    + closeResult.payOrderNo()
                    + " 当前状态 "
                    + closeResult.status()
                    + (closeResult.changed() ? "，已关闭" : "，本地状态未变化");
            Require.isTrue(handleResult.length() <= 512,
                    PaymentCode.PAYMENT_EXCEPTION_ORDER_INVALID.getCode(), "处理结果不能超过 512 个字符");
        }

        LocalDateTime now = LocalDateTime.now();
        Long operatorId = PaymentContextSupport.currentUserId();
        int updated = exceptionOrderMapper.handleExceptionOrder(
                tenantId,
                command.getId(),
                nextHandleStatus,
                handleAction,
                handleReason,
                handleResult,
                handleEvidence,
                operatorId,
                PaymentContextSupport.currentPrincipalName(),
                now);
        Require.isTrue(updated == 1, PaymentCode.PAYMENT_EXCEPTION_ORDER_HANDLE_STATUS_INVALID);

        auditService.record(
                PaymentOperationAuditService.ACTION_HANDLE_EXCEPTION_ORDER,
                PaymentOperationAuditService.RESOURCE_PAYMENT_EXCEPTION_ORDER,
                entity.getExceptionNo(),
                PaymentOperationAuditService.RESULT_SUCCESS);
        return detailExceptionOrder(command.getId());
    }

    public PaymentExceptionOrderEntity createIfAbsent(
            Long tenantId,
            String relatedOrderNo,
            String exceptionType,
            String severity,
            String reason,
            LocalDateTime eventTime) {
        Require.notNull(tenantId, PaymentCode.PAYMENT_EXCEPTION_ORDER_INVALID.getCode(), "租户 ID 不能为空");
        Require.notBlank(relatedOrderNo, PaymentCode.PAYMENT_EXCEPTION_ORDER_INVALID.getCode(), "关联订单号不能为空");
        Require.notBlank(exceptionType, PaymentCode.PAYMENT_EXCEPTION_ORDER_INVALID.getCode(), "异常类型不能为空");
        Require.notBlank(severity, PaymentCode.PAYMENT_EXCEPTION_ORDER_INVALID.getCode(), "异常级别不能为空");
        LocalDateTime resolvedEventTime = eventTime == null ? LocalDateTime.now() : eventTime;

        PaymentExceptionOrderEntity existing = exceptionOrderMapper.selectActiveByBusinessKey(
                tenantId, relatedOrderNo, exceptionType);
        if (existing != null) {
            return existing;
        }

        PaymentExceptionOrderEntity entity = new PaymentExceptionOrderEntity();
        entity.setId(IdWorker.getId());
        entity.setExceptionNo(numberService.next(PaymentNumberService.PAY_EXCEPTION_NO));
        entity.setRelatedOrderNo(relatedOrderNo);
        entity.setExceptionType(exceptionType);
        entity.setSeverity(severity);
        entity.setHandleStatus(HANDLE_STATUS_PENDING);
        entity.setReason(reason);
        entity.setTenantId(tenantId);
        entity.setCreatedBy(PaymentContextSupport.currentUserId());
        entity.setCreatedAt(resolvedEventTime);
        entity.setUpdatedBy(PaymentContextSupport.currentUserId());
        entity.setUpdatedAt(resolvedEventTime);
        entity.setDelFlag(0);
        try {
            exceptionOrderMapper.insert(entity);
            return entity;
        } catch (DuplicateKeyException ex) {
            PaymentExceptionOrderEntity existingAfterDuplicate =
                    exceptionOrderMapper.selectActiveByBusinessKey(tenantId, relatedOrderNo, exceptionType);
            Require.notNull(existingAfterDuplicate,
                    PaymentCode.PAYMENT_EXCEPTION_ORDER_INVALID.getCode(),
                    "异常订单幂等创建冲突，请重试");
            return existingAfterDuplicate;
        }
    }

    private PaymentChannelSyncService.PaymentSyncResult activeQueryPayment(String handleAction, String relatedOrderNo) {
        if (!"ACTIVE_QUERY".equals(handleAction)) {
            return null;
        }
        String payOrderNo = PaymentContextSupport.trimToNull(relatedOrderNo);
        Require.isTrue(isPaymentOrderNo(payOrderNo),
                PaymentCode.PAYMENT_EXCEPTION_ORDER_INVALID.getCode(), "主动查单动作必须关联支付订单号");
        return channelSyncService.syncPaymentStatus(payOrderNo);
    }

    private PaymentChannelSyncService.RefundSyncResult activeQueryRefund(String handleAction, String relatedOrderNo) {
        if (!"ACTIVE_REFUND_QUERY".equals(handleAction)) {
            return null;
        }
        String refundOrderNo = PaymentContextSupport.trimToNull(relatedOrderNo);
        Require.isTrue(isRefundOrderNo(refundOrderNo),
                PaymentCode.PAYMENT_EXCEPTION_ORDER_INVALID.getCode(), "主动查退款动作必须关联退款订单号");
        return channelSyncService.syncRefundStatus(refundOrderNo);
    }

    private PaymentChannelOrderCloseService.CloseResult closePaymentOrder(String handleAction, String relatedOrderNo) {
        if (!"CLOSE_PAYMENT_ORDER".equals(handleAction)) {
            return null;
        }
        String payOrderNo = PaymentContextSupport.trimToNull(relatedOrderNo);
        Require.isTrue(isPaymentOrderNo(payOrderNo),
                PaymentCode.PAYMENT_EXCEPTION_ORDER_INVALID.getCode(), "关闭支付订单动作必须关联支付订单号");
        return channelOrderCloseService.closePaymentOrder(payOrderNo);
    }

    private String nextExceptionOrderHandleStatus(String handleAction) {
        if ("ADD_EVIDENCE".equals(handleAction)) {
            return "PROCESSING";
        }
        if ("MANUAL_CLOSE".equals(handleAction)) {
            return "CLOSED";
        }
        return "HANDLED";
    }

    private boolean isPaymentOrderNo(String orderNo) {
        return orderNo != null && PAYMENT_ORDER_NO_PATTERN.matcher(orderNo).matches();
    }

    private boolean isRefundOrderNo(String orderNo) {
        return orderNo != null && REFUND_ORDER_NO_PATTERN.matcher(orderNo).matches();
    }

    private boolean isActionAllowedForExceptionType(String handleAction, String exceptionType) {
        if ("ACTIVE_QUERY".equals(handleAction) || "CLOSE_PAYMENT_ORDER".equals(handleAction)) {
            return exceptionType != null && PAYMENT_EXCEPTION_TYPES.contains(exceptionType);
        }
        if ("ACTIVE_REFUND_QUERY".equals(handleAction)) {
            return TYPE_REFUND_MISMATCH.equals(exceptionType);
        }
        return "ADD_EVIDENCE".equals(handleAction) || "MANUAL_CLOSE".equals(handleAction);
    }

    private List<String> allExceptionTypes() {
        return List.of(
                TYPE_DUPLICATE_PAYMENT,
                TYPE_PAY_TIMEOUT,
                TYPE_CHANNEL_FAILED,
                TYPE_REFUND_MISMATCH);
    }

    private void fillExceptionOrderSummary(PaymentExceptionOrderVO vo) {
        vo.setExceptionTypeName(exceptionTypeName(vo.getExceptionType()));
        vo.setSeverityName(exceptionSeverityName(vo.getSeverity()));
        vo.setHandleStatusName(exceptionHandleStatusName(vo.getHandleStatus()));
    }

    private boolean canHandleExceptionOrder(String handleStatus) {
        return "PENDING".equals(handleStatus) || "PROCESSING".equals(handleStatus);
    }

    private String exceptionTypeName(String exceptionType) {
        if ("DUPLICATE_PAYMENT".equals(exceptionType)) {
            return "重复支付";
        }
        if ("PAY_TIMEOUT".equals(exceptionType)) {
            return "超时未回调";
        }
        if ("AMOUNT_MISMATCH".equals(exceptionType)) {
            return "金额不一致";
        }
        if ("STATUS_MISMATCH".equals(exceptionType)) {
            return "状态不一致";
        }
        if ("REFUND_MISMATCH".equals(exceptionType)) {
            return "退款异常";
        }
        if ("CHANNEL_FAILED".equals(exceptionType)) {
            return "通道失败";
        }
        return exceptionType;
    }

    private String exceptionSeverityName(String severity) {
        if ("CRITICAL".equals(severity)) {
            return "严重";
        }
        if ("HIGH".equals(severity)) {
            return "高";
        }
        if ("MEDIUM".equals(severity)) {
            return "中";
        }
        if ("LOW".equals(severity)) {
            return "低";
        }
        return severity;
    }

    private String exceptionHandleStatusName(String handleStatus) {
        if ("PENDING".equals(handleStatus)) {
            return "待处理";
        }
        if ("PROCESSING".equals(handleStatus)) {
            return "处理中";
        }
        if ("HANDLED".equals(handleStatus)) {
            return "已处理";
        }
        if ("IGNORED".equals(handleStatus)) {
            return "已忽略";
        }
        if ("CLOSED".equals(handleStatus)) {
            return "已关闭";
        }
        return handleStatus;
    }

    private PaymentExceptionOrderStatusVO exceptionOrderStatus(String statusCode) {
        PaymentExceptionOrderStatusVO vo = new PaymentExceptionOrderStatusVO();
        vo.setStatusCode(statusCode);
        vo.setStatusName(exceptionHandleStatusName(statusCode));
        return vo;
    }

    private PaymentExceptionOrderActionVO exceptionOrderAction(
            String actionCode,
            String actionName,
            List<String> allowedExceptionTypes,
            String description) {
        PaymentExceptionOrderActionVO vo = new PaymentExceptionOrderActionVO();
        vo.setActionCode(actionCode);
        vo.setActionName(actionName);
        vo.setAllowedExceptionTypes(allowedExceptionTypes);
        vo.setDescription(description);
        return vo;
    }

    private String normalizeRefundStatus(String status) {
        return "PROCESSING".equals(status) ? "REFUNDING" : status;
    }
}
