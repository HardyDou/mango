package io.mango.payment.core.service;

import io.mango.common.result.Require;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import io.mango.infra.event.api.DomainEvent;
import io.mango.infra.event.api.DomainEventSubscriber;
import io.mango.payment.api.PaymentCode;
import io.mango.workflow.api.WorkflowEventTypes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class PaymentRefundApprovalWorkflowSubscriber implements DomainEventSubscriber {

    private static final String APP_CODE = "internal-admin";
    private static final String SYSTEM_PRINCIPAL = "workflow";
    private static final Set<String> SUPPORTED_EVENTS = Set.of(
            WorkflowEventTypes.PROCESS_COMPLETED,
            WorkflowEventTypes.PROCESS_REJECTED,
            WorkflowEventTypes.PROCESS_STARTED,
            WorkflowEventTypes.TASK_COMPLETED,
            WorkflowEventTypes.TASK_REJECTED);

    private final PaymentRefundApprovalService refundApprovalService;

    @Override
    public String eventType() {
        return "*";
    }

    @Override
    public void onEvent(DomainEvent event) {
        if (event == null
                || !SUPPORTED_EVENTS.contains(event.getEventType())
                || !PaymentRefundApprovalService.WORKFLOW_BUSINESS_TYPE.equals(event.getBusinessType())) {
            return;
        }
        String approvalNo = PaymentContextSupport.trimToNull(event.getBusinessKey());
        Require.notBlank(approvalNo, PaymentCode.PAYMENT_REFUND_APPROVAL_INVALID.getCode(), "工作流事件缺少退款审批单号");
        Map<String, Object> payload = event.getPayload();
        Long tenantId = tenantId(payload);
        String processInstanceId = stringValue(payload == null ? null : payload.get("processInstanceId"));
        String reason = stringValue(payload == null ? null : payload.get("reason"));
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            ensureContext(tenantId);
            refundApprovalService.syncWorkflowProjection(tenantId, approvalNo);
            if (WorkflowEventTypes.PROCESS_COMPLETED.equals(event.getEventType())) {
                refundApprovalService.approveByWorkflow(tenantId, approvalNo, processInstanceId);
            } else if (WorkflowEventTypes.PROCESS_REJECTED.equals(event.getEventType())) {
                refundApprovalService.rejectByWorkflow(tenantId, approvalNo, processInstanceId, reason);
            }
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    private Long tenantId(Map<String, Object> payload) {
        Object value = payload == null ? null : payload.get("tenantId");
        String tenantId = stringValue(value);
        Require.notBlank(tenantId, PaymentCode.PAYMENT_REFUND_APPROVAL_INVALID.getCode(), "工作流事件缺少租户 ID");
        try {
            return Long.valueOf(tenantId);
        } catch (NumberFormatException e) {
            return Require.fail(PaymentCode.PAYMENT_REFUND_APPROVAL_INVALID.getCode(), "工作流事件租户 ID 非法: " + tenantId);
        }
    }

    private void ensureContext(Long tenantId) {
        MangoContextSnapshot current = MangoContextHolder.get();
        if (current.tenantId() != null) {
            return;
        }
        MangoContextHolder.set(current.withSecurity(null, String.valueOf(tenantId), SYSTEM_PRINCIPAL,
                null, "SYSTEM", "SYSTEM", null, APP_CODE));
    }

    private String stringValue(Object value) {
        return value == null ? null : PaymentContextSupport.trimToNull(String.valueOf(value));
    }
}
