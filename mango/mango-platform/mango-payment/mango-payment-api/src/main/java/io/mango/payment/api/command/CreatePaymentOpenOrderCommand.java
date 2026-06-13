package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "支付开放接口创建业务订单命令")
public class CreatePaymentOpenOrderCommand {

    @Schema(description = "租户 ID")
    @NotNull(message = "租户 ID 不能为空")
    private Long tenantId;

    @Schema(description = "支付应用 AppId")
    @NotBlank(message = "AppId 不能为空")
    @Size(max = 64, message = "AppId 长度不能超过 64")
    private String appId;

    @Schema(description = "业务订单号")
    @NotBlank(message = "业务订单号不能为空")
    @Size(max = 64, message = "业务订单号长度不能超过 64")
    private String bizOrderNo;

    @Schema(description = "订单标题")
    @NotBlank(message = "订单标题不能为空")
    @Size(max = 128, message = "订单标题长度不能超过 128")
    private String title;

    @Schema(description = "企业主体 ID。未传时使用应用默认收银台允许的第一个企业主体")
    private Long subjectId;

    @Schema(description = "订单金额，单位分")
    @NotNull(message = "订单金额不能为空")
    @Min(value = 1, message = "订单金额必须大于 0")
    private Long amount;

    @Schema(description = "币种")
    @NotBlank(message = "币种不能为空")
    @Size(max = 16, message = "币种长度不能超过 16")
    private String currency;

    @Schema(description = "订单有效分钟数")
    @NotNull(message = "订单有效分钟数不能为空")
    @Min(value = 1, message = "订单有效分钟数必须大于 0")
    private Integer expireMinutes;

    @Schema(description = "业务通知地址")
    @NotBlank(message = "业务通知地址不能为空")
    @Size(max = 512, message = "业务通知地址长度不能超过 512")
    private String notifyUrl;

    @Schema(description = "业务返回地址")
    @NotBlank(message = "业务返回地址不能为空")
    @Size(max = 512, message = "业务返回地址长度不能超过 512")
    private String returnUrl;

    @Schema(description = "业务扩展信息")
    private Map<String, Object> extendInfo;
}
