package io.mango.payment.core.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.AuditableEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_refund_approval")
public class PaymentRefundApprovalEntity extends AuditableEntity {

    private String approvalNo;

    private Long businessOrderId;

    private Long paymentOrderId;

    private Long refundOrderId;

    private String bizOrderNo;

    private String bizRefundNo;

    private String appId;

    private Long refundAmount;

    private String reason;

    private String remark;

    private String status;

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

    private Long tenantId;

    @TableLogic
    private Integer delFlag = 0;
}
