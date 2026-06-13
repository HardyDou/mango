package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "支付开放接口支付订单")
public class PaymentOpenPaymentOrderVO {

    @Schema(description = "支付订单 ID")
    private Long id;

    @Schema(description = "支付订单号")
    private String payOrderNo;

    @Schema(description = "业务订单 ID")
    private Long businessOrderId;

    @Schema(description = "业务订单号")
    private String bizOrderNo;

    @Schema(description = "AppId")
    private String appId;

    @Schema(description = "支付标题")
    private String title;

    @Schema(description = "支付金额，单位分")
    private Long amount;

    @Schema(description = "币种")
    private String currency;

    @Schema(description = "支付订单状态")
    private String status;

    @Schema(description = "支付方式编码")
    private String methodCode;

    @Schema(description = "支付方式名称")
    private String methodName;

    @Schema(description = "支付通道编码")
    private String channelCode;

    @Schema(description = "支付通道名称")
    private String channelName;

    @Schema(description = "通道商户号")
    private String channelMerchantNo;

    @Schema(description = "通道签约配置 ID")
    private Long contractId;

    @Schema(description = "签约能力 ID")
    private Long contractCapabilityId;

    @Schema(description = "路由规则 ID")
    private Long routeRuleId;

    @Schema(description = "通道交易号")
    private String channelTradeNo;

    @Schema(description = "是否为业务订单有效成功支付：1-是，0-否")
    private Integer successFlag;

    @Schema(description = "支付成功时间")
    private LocalDateTime payTime;

    @Schema(description = "支付订单过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "交易流水号")
    private String flowNo;

    @Schema(description = "支付物料")
    private PaymentCashierPayMaterialVO material;
}
