package io.mango.payment.core.service;

import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.command.RetryPaymentNotificationRecordCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentNotificationRecordVO;
import io.mango.payment.api.vo.PaymentNotificationStatusVO;
import io.mango.payment.core.entity.PaymentNotificationRecordEntity;
import io.mango.payment.core.mapper.PaymentNotificationRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentNotificationRecordService {

    private final PaymentNotificationRecordMapper notificationRecordMapper;
    private final PaymentNotificationService notificationService;
    private final PaymentOperationAuditService auditService;

    public PageResult<PaymentNotificationRecordVO> pageNotificationRecords(PaymentConfigPageQuery query) {
        PaymentConfigPageQuery resolved = query == null ? new PaymentConfigPageQuery() : query;
        String keyword = PaymentContextSupport.trimToNull(resolved.getKeyword());
        String statusCode = PaymentContextSupport.trimToNull(resolved.getStatusCode());
        Long tenantId = PaymentContextSupport.currentTenantId();
        long total = notificationRecordMapper.countNotificationRecords(tenantId, keyword, statusCode);
        long page = resolved.getPage();
        long size = resolved.getSize();
        List<PaymentNotificationRecordVO> rows = notificationRecordMapper.selectNotificationRecordPage(
                tenantId, keyword, statusCode, size, (page - 1) * size);
        rows.forEach(this::fillNotificationRecordSummary);
        return PageResult.of(rows, total, page, size);
    }

    public PaymentNotificationRecordVO detailNotificationRecord(Long id) {
        Require.notNull(id, PaymentCode.PAYMENT_NOTIFICATION_RECORD_INVALID.getCode(), "通知记录 ID 不能为空");
        PaymentNotificationRecordVO vo = notificationRecordMapper.selectNotificationRecordDetail(PaymentContextSupport.currentTenantId(), id);
        Require.notNull(vo, PaymentCode.PAYMENT_NOTIFICATION_RECORD_NOT_FOUND);
        fillNotificationRecordSummary(vo);
        return vo;
    }

    public List<PaymentNotificationStatusVO> listNotificationStatuses() {
        return List.of(
                notificationStatus("SUCCESS"),
                notificationStatus("RETRYING"),
                notificationStatus("FAILED"),
                notificationStatus("PENDING"));
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentNotificationRecordVO retryNotificationRecord(RetryPaymentNotificationRecordCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_NOTIFICATION_RECORD_INVALID);
        Require.notNull(command.getId(), PaymentCode.PAYMENT_NOTIFICATION_RECORD_INVALID.getCode(), "通知记录 ID 不能为空");
        String retryReason = PaymentContextSupport.trimToNull(command.getRetryReason());
        Require.notBlank(retryReason, PaymentCode.PAYMENT_NOTIFICATION_RECORD_INVALID.getCode(), "重推原因不能为空");
        Require.isTrue(retryReason.length() <= 512, PaymentCode.PAYMENT_NOTIFICATION_RECORD_INVALID.getCode(), "重推原因不能超过 512 个字符");

        Long tenantId = PaymentContextSupport.currentTenantId();
        PaymentNotificationRecordEntity entity = notificationRecordMapper.selectById(command.getId());
        Require.notNull(entity, PaymentCode.PAYMENT_NOTIFICATION_RECORD_NOT_FOUND);
        Require.isTrue(tenantId.equals(entity.getTenantId()) && !Integer.valueOf(1).equals(entity.getDelFlag()),
                PaymentCode.PAYMENT_NOTIFICATION_RECORD_NOT_FOUND);
        Require.isTrue(canRetryNotificationRecord(entity.getNotifyStatus()),
                PaymentCode.PAYMENT_NOTIFICATION_RECORD_RETRY_STATUS_INVALID);
        Require.notBlank(entity.getPayloadJson(),
                PaymentCode.PAYMENT_NOTIFICATION_RECORD_RETRY_STATUS_INVALID.getCode(), "通知报文快照不存在，不能人工重推");

        LocalDateTime now = LocalDateTime.now();
        Long operatorId = PaymentContextSupport.currentUserId();
        String retryResult = "人工补偿重推已登记，等待通知任务执行 ACK";
        int updated = notificationRecordMapper.manualRetryNotificationRecord(
                tenantId,
                command.getId(),
                retryReason,
                retryResult,
                now,
                operatorId,
                PaymentContextSupport.currentPrincipalName());
        Require.isTrue(updated == 1, PaymentCode.PAYMENT_NOTIFICATION_RECORD_RETRY_STATUS_INVALID);

        auditService.record(
                PaymentOperationAuditService.ACTION_RETRY_NOTIFICATION_RECORD,
                PaymentOperationAuditService.RESOURCE_PAYMENT_NOTIFICATION_RECORD,
                entity.getNotificationNo(),
                PaymentOperationAuditService.RESULT_SUCCESS);
        return detailNotificationRecord(command.getId());
    }

    public int deliverDueNotificationRecords(long limit) {
        int delivered = notificationService.deliverDueNotificationRecords(limit);
        auditService.record(
                PaymentOperationAuditService.ACTION_DELIVER_DUE_NOTIFICATION_RECORDS,
                PaymentOperationAuditService.RESOURCE_PAYMENT_NOTIFICATION_RECORD,
                "DUE_NOTIFICATION_RECORDS",
                PaymentOperationAuditService.RESULT_SUCCESS);
        return delivered;
    }

    private void fillNotificationRecordSummary(PaymentNotificationRecordVO vo) {
        vo.setNotificationTypeName(notificationTypeName(vo.getNotificationType()));
        vo.setNotifyStatusName(notificationStatusName(vo.getNotifyStatus()));
    }

    private boolean canRetryNotificationRecord(String notifyStatus) {
        return "FAILED".equals(notifyStatus) || "RETRYING".equals(notifyStatus) || "PENDING".equals(notifyStatus);
    }

    private PaymentNotificationStatusVO notificationStatus(String statusCode) {
        PaymentNotificationStatusVO vo = new PaymentNotificationStatusVO();
        vo.setStatusCode(statusCode);
        vo.setStatusName(notificationStatusName(statusCode));
        return vo;
    }

    private String notificationTypeName(String notificationType) {
        if ("PAYMENT_SUCCESS".equals(notificationType)) {
            return "支付成功通知";
        }
        if ("PAYMENT_FAILED".equals(notificationType)) {
            return "支付失败通知";
        }
        if ("PAYMENT_CLOSED".equals(notificationType)) {
            return "支付关闭通知";
        }
        if ("REFUND_SUCCESS".equals(notificationType)) {
            return "退款成功通知";
        }
        if ("REFUND_FAILED".equals(notificationType)) {
            return "退款失败通知";
        }
        return notificationType;
    }

    private String notificationStatusName(String notifyStatus) {
        if ("SUCCESS".equals(notifyStatus)) {
            return "通知成功";
        }
        if ("RETRYING".equals(notifyStatus)) {
            return "重试中";
        }
        if ("FAILED".equals(notifyStatus)) {
            return "通知失败";
        }
        if ("PENDING".equals(notifyStatus)) {
            return "待通知";
        }
        return notifyStatus;
    }
}
