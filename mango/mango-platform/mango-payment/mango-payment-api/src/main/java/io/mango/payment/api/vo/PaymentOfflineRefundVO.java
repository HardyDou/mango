package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "线下退款订单")
public class PaymentOfflineRefundVO {

    @Schema(description = "线下退款 ID")
    private Long id;

    @Schema(description = "线下退款单号")
    private String offlineRefundNo;

    @Schema(description = "线下收款 ID")
    private Long offlineCollectionId;

    @Schema(description = "线下收款单号")
    private String offlineCollectionNo;

    @Schema(description = "统一退款订单 ID")
    private Long refundOrderId;

    @Schema(description = "统一退款订单号")
    private String refundOrderNo;

    @Schema(description = "支付订单 ID")
    private Long paymentOrderId;

    @Schema(description = "支付订单号")
    private String payOrderNo;

    @Schema(description = "业务订单 ID")
    private Long businessOrderId;

    @Schema(description = "业务订单号")
    private String bizOrderNo;

    @Schema(description = "支付标题")
    private String title;

    @Schema(description = "AppId")
    private String appId;

    @Schema(description = "通道 ID")
    private Long channelId;

    @Schema(description = "通道编码")
    private String channelCode;

    @Schema(description = "通道名称")
    private String channelName;

    @Schema(description = "退款金额，单位分")
    private Long refundAmount;

    @Schema(description = "币种")
    private String currency;

    @Schema(description = "退款账户户名")
    private String refundAccountName;

    @Schema(description = "脱敏退款账号")
    private String refundAccountNoMask;

    @Schema(description = "退款开户行")
    private String refundBankName;

    @Schema(description = "退款凭证文件 ID，多个用英文逗号分隔")
    private String refundVoucherFileIds;

    @Schema(description = "退款凭证数量")
    private Integer refundVoucherCount;

    @Schema(description = "退款原因")
    private String reason;

    @Schema(description = "退款备注")
    private String remark;

    @Schema(description = "退款状态编码")
    private String refundStatus;

    @Schema(description = "退款状态名称")
    private String refundStatusName;

    @Schema(description = "退款完成时间")
    private LocalDateTime refundedTime;

    @Schema(description = "操作人 ID")
    private Long operatorId;

    @Schema(description = "操作人名称")
    private String operatorName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
