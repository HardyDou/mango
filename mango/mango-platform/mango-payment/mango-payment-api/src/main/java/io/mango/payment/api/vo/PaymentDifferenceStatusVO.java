package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "对账差异处理状态")
public class PaymentDifferenceStatusVO {

    @Schema(description = "处理状态编码")
    private String statusCode;

    @Schema(description = "处理状态名称")
    private String statusName;
}
