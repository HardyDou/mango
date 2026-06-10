package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "线下退款状态")
public class PaymentOfflineRefundStatusVO {

    @Schema(description = "状态编码")
    private String code;

    @Schema(description = "状态名称")
    private String label;
}
