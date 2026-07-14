package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "支付开放接口业务扩展信息")
public class PaymentOpenExtendInfoCommand {

    @Size(max = 128, message = "展示名称不能超过 128 个字符")
    @Schema(description = "业务展示名称")
    private String displayName;

    @Size(max = 128, message = "业务引用号不能超过 128 个字符")
    @Schema(description = "业务引用号")
    private String businessRefNo;

    @Size(max = 64, message = "业务场景不能超过 64 个字符")
    @Schema(description = "业务场景")
    private String scenario;

    @Size(max = 64, message = "业务场景后缀不能超过 64 个字符")
    @Schema(description = "业务场景后缀")
    private String suffix;
}
