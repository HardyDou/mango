package io.mango.payment.core.model.projection;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaymentRefundApprovalProjection {

    private Long id;

    private String approvalNo;

    private String appId;

    private String bizOrderNo;

    private String bizRefundNo;

    private Long paymentOrderId;

    private String payOrderNo;

    private Long refundOrderId;

    private String refundOrderNo;

    private Long refundAmount;

    private String reason;

    private String remark;

    private String status;

    private String statusName;

    private Long workflowApplyId;

    private String workflowProcessInstanceId;

    private String workflowProcessDefinitionKey;

    private String workflowApplyStatus;

    private String workflowApplyStatusName;

    private String workflowCurrentTaskNames;

    private String workflowCurrentAssigneeNames;

    private LocalDateTime workflowSyncedAt;

    private Long applicantId;

    private String applicantName;

    private LocalDateTime applyTime;

    private Long reviewerId;

    private String reviewerName;

    private String reviewReason;

    private LocalDateTime reviewTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
