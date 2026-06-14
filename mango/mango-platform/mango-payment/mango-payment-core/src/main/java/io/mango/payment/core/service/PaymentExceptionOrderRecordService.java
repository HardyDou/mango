package io.mango.payment.core.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import io.mango.common.result.Require;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.core.entity.PaymentExceptionOrderEntity;
import io.mango.payment.core.mapper.PaymentExceptionOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentExceptionOrderRecordService {

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

    private final PaymentExceptionOrderMapper exceptionOrderMapper;
    private final PaymentNumberService numberService;

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
}
