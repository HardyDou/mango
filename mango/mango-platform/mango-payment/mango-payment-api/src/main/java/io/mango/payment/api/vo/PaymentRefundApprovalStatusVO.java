package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "后台退款审批状态")
public class PaymentRefundApprovalStatusVO {

    @Schema(description = "状态编码")
    private String statusCode;

    @Schema(description = "状态名称")
    private String statusName;
}
