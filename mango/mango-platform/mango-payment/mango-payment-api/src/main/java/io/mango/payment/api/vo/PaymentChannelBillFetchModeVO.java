package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "通道账单获取方式选项")
public class PaymentChannelBillFetchModeVO {

    @Schema(description = "获取方式编码")
    private String fetchMode;

    @Schema(description = "获取方式名称")
    private String fetchModeName;
}
