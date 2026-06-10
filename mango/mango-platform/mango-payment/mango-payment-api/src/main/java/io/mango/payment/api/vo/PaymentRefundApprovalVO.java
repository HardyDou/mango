package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "后台退款审批")
public class PaymentRefundApprovalVO {

    @Schema(description = "退款审批 ID")
    private Long id;

    @Schema(description = "退款审批单号")
    private String approvalNo;

    @Schema(description = "支付应用 AppId")
    private String appId;

    @Schema(description = "业务订单号")
    private String bizOrderNo;

    @Schema(description = "业务退款单号")
    private String bizRefundNo;

    @Schema(description = "原支付订单 ID")
    private Long paymentOrderId;

    @Schema(description = "原支付订单号")
    private String payOrderNo;

    @Schema(description = "退款订单 ID")
    private Long refundOrderId;

    @Schema(description = "退款订单号")
    private String refundOrderNo;

    @Schema(description = "退款金额，单位分")
    private Long refundAmount;

    @Schema(description = "退款原因")
    private String reason;

    @Schema(description = "退款备注")
    private String remark;

    @Schema(description = "审批状态编码")
    private String status;

    @Schema(description = "审批状态名称")
    private String statusName;

    @Schema(description = "申请人 ID")
    private Long applicantId;

    @Schema(description = "申请人名称")
    private String applicantName;

    @Schema(description = "申请时间")
    private LocalDateTime applyTime;

    @Schema(description = "审核人 ID")
    private Long reviewerId;

    @Schema(description = "审核人名称")
    private String reviewerName;

    @Schema(description = "审核说明")
    private String reviewReason;

    @Schema(description = "审核时间")
    private LocalDateTime reviewTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
