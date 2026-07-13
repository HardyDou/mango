package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "支付开放接口签名请求命令")
public class PaymentOpenRequestCommand {

    @Schema(description = "请求体原文")
    @Size(max = 1048576, message = "请求体不能超过 1 MiB")
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
    @Size(max = 256, message = "请求路径不能超过 256 个字符")
    private String requestPath;

    @Schema(description = "客户端 IP")
    @Size(max = 64, message = "客户端 IP 不能超过 64 个字符")
    private String clientIp;

    @Schema(description = "业务订单号")
    @Size(max = 64, message = "业务订单号不能超过 64 个字符")
    private String bizOrderNo;

    @Schema(description = "支付订单号")
    @Size(max = 64, message = "支付订单号不能超过 64 个字符")
    private String payOrderNo;

    @Schema(description = "业务退款单号")
    @Size(max = 64, message = "业务退款单号不能超过 64 个字符")
    private String bizRefundNo;
}
