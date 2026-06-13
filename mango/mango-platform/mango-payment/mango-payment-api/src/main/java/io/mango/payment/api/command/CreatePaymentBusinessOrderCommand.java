package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "创建后台业务订单命令")
public class CreatePaymentBusinessOrderCommand {

    @Schema(description = "支付应用 AppId")
    @NotBlank(message = "AppId 不能为空")
    @Size(max = 64, message = "AppId 长度不能超过 64")
    private String appId;

    @Schema(description = "业务订单号")
    @Size(max = 64, message = "业务订单号长度不能超过 64")
    private String bizOrderNo;

    @Schema(description = "支付标题")
    @NotBlank(message = "支付标题不能为空")
    @Size(max = 128, message = "支付标题长度不能超过 128")
    private String title;

    @Schema(description = "收款主体 ID")
    @NotNull(message = "收款主体 ID 不能为空")
    private Long subjectId;

    @Schema(description = "订单金额，单位分")
    @NotNull(message = "订单金额不能为空")
    @Positive(message = "订单金额必须大于 0")
    private Long amount;

    @Schema(description = "币种")
    @Size(max = 16, message = "币种长度不能超过 16")
    private String currency;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "通知地址")
    @Size(max = 512, message = "通知地址长度不能超过 512")
    private String notifyUrl;

    @Schema(description = "返回地址")
    @Size(max = 512, message = "返回地址长度不能超过 512")
    private String returnUrl;

    @Schema(description = "扩展信息 JSON")
    @Size(max = 2048, message = "扩展信息长度不能超过 2048")
    private String extendInfo;
}
