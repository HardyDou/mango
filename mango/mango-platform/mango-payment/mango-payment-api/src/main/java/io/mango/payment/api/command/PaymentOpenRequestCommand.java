package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "支付开放接口签名请求命令")
public class PaymentOpenRequestCommand {

    @Schema(description = "请求体原文")
    private String body;

    @Schema(description = "支付应用 AppId")
    @NotBlank(message = "AppId 不能为空")
    private String appId;

    @Schema(description = "租户 ID")
    @NotBlank(message = "tenantId 不能为空")
    private String tenantId;

    @Schema(description = "请求时间戳，Unix 秒")
    @NotBlank(message = "timestamp 不能为空")
    private String timestamp;

    @Schema(description = "随机串")
    @NotBlank(message = "nonce 不能为空")
    private String nonce;

    @Schema(description = "Base64 HMAC-SHA256 签名")
    @NotBlank(message = "signature 不能为空")
    private String signature;

    @Schema(description = "请求路径")
    @NotBlank(message = "请求路径不能为空")
    private String requestPath;

    @Schema(description = "客户端 IP")
    private String clientIp;

    @Schema(description = "业务订单号")
    private String bizOrderNo;

    @Schema(description = "支付订单号")
    private String payOrderNo;

    @Schema(description = "业务退款单号")
    private String bizRefundNo;
}
