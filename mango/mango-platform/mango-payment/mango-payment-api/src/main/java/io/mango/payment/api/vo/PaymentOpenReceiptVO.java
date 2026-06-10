package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "支付开放接口支付凭证")
public class PaymentOpenReceiptVO {

    @Schema(description = "支付凭证号")
    private String receiptNo;

    @Schema(description = "业务订单号")
    private String bizOrderNo;

    @Schema(description = "支付订单号")
    private String payOrderNo;

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

    @Schema(description = "通道交易号")
    private String channelTradeNo;

    @Schema(description = "交易流水号")
    private String flowNo;

    @Schema(description = "支付成功时间")
    private LocalDateTime payTime;

    @Schema(description = "支付订单创建时间")
    private LocalDateTime createTime;

    @Schema(description = "凭证出具时间")
    private LocalDateTime issuedTime;
}
