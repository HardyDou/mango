package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "业务订单")
public class PaymentBusinessOrderVO {

    @Schema(description = "业务订单 ID")
    private Long id;

    @Schema(description = "业务订单号")
    private String bizOrderNo;

    @Schema(description = "AppId")
    private String appId;

    @Schema(description = "应用名称")
    private String appName;

    @Schema(description = "支付标题")
    private String title;

    @Schema(description = "企业主体 ID")
    private Long subjectId;

    @Schema(description = "企业主体名称")
    private String subjectName;

    @Schema(description = "匹配的收银台配置 ID")
    private Long cashierConfigId;

    @Schema(description = "匹配的收银台名称")
    private String cashierName;

    @Schema(description = "应付金额，单位分")
    private Long amount;

    @Schema(description = "已支付金额，单位分")
    private Long paidAmount;

    @Schema(description = "已退款金额，单位分")
    private Long refundedAmount;

    @Schema(description = "币种")
    private String currency;

    @Schema(description = "状态编码")
    private String status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "是否允许发起支付")
    private Boolean payable;

    @Schema(description = "不允许发起支付的原因")
    private String payDisabledReason;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "通知地址")
    private String notifyUrl;

    @Schema(description = "返回地址")
    private String returnUrl;

    @Schema(description = "扩展信息")
    private String extendInfo;

    @Schema(description = "支付订单数")
    private Long paymentOrderCount;

    @Schema(description = "退款订单数")
    private Long refundOrderCount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "状态流转记录")
    private List<PaymentOrderStatusFlowVO> statusFlows;
}
