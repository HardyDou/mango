package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "支付开放接口业务订单")
public class PaymentOpenBusinessOrderVO {

    @Schema(description = "业务订单 ID")
    private Long id;

    @Schema(description = "业务订单号")
    private String bizOrderNo;

    @Schema(description = "AppId")
    private String appId;

    @Schema(description = "订单标题")
    private String title;

    @Schema(description = "企业主体 ID")
    private Long subjectId;

    @Schema(description = "应付金额，单位分")
    private Long amount;

    @Schema(description = "已支付金额，单位分")
    private Long paidAmount;

    @Schema(description = "已退款金额，单位分")
    private Long refundedAmount;

    @Schema(description = "币种")
    private String currency;

    @Schema(description = "订单状态")
    private String status;

    @Schema(description = "订单过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "业务通知地址")
    private String notifyUrl;

    @Schema(description = "业务返回地址")
    private String returnUrl;

    @Schema(description = "业务扩展信息 JSON")
    private String extendInfo;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
