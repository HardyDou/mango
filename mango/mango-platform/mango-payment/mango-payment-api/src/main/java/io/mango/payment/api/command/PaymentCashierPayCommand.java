package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "收银台支付命令")
public class PaymentCashierPayCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "收银台配置 ID 不能为空")
    @Schema(description = "收银台配置 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long cashierConfigId;

    @Schema(description = "业务订单 ID。后台预览可为空，真实支付必须关联业务订单")
    @Positive(message = "业务订单 ID 必须大于 0")
    private Long businessOrderId;

    @NotBlank(message = "支付方式编码不能为空")
    @Schema(description = "标准支付方式编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String methodCode;

    @Schema(description = "网银银行编码")
    @Size(max = 64, message = "网银银行编码不能超过 64 个字符")
    private String bankCode;

    @Schema(description = "网银银行名称")
    @Size(max = 128, message = "网银银行名称不能超过 128 个字符")
    private String bankName;

    @Schema(description = "付款账号或卡号。仅部分需要在收银台预采集付款信息的通道使用")
    @Size(max = 128, message = "付款账号不能超过 128 个字符")
    private String payerAccountNo;

    @Schema(description = "付款户名。仅部分需要在收银台预采集付款信息的通道使用")
    @Size(max = 128, message = "付款户名不能超过 128 个字符")
    private String payerName;

    @Schema(description = "付款人请求 IP。由服务端根据 HTTP 请求写入，前端不需要传")
    @Size(max = 64, message = "付款人请求 IP 不能超过 64 个字符")
    private String clientIp;
}
