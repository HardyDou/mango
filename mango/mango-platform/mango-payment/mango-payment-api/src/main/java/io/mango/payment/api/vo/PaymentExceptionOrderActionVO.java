package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "异常订单处理动作")
public class PaymentExceptionOrderActionVO {

    @Schema(description = "处理动作编码")
    private String actionCode;

    @Schema(description = "处理动作名称")
    private String actionName;

    @Schema(description = "适用异常类型编码列表")
    private List<String> allowedExceptionTypes;

    @Schema(description = "动作说明")
    private String description;
}
