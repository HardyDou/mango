package io.mango.payment.core.service;

public record PaymentRefundApprovalWorkflowContext(
        String tenantId,
        String approvalNo,
        String processInstanceId,
        String reason) {
}
