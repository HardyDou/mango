package io.mango.payment.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 支付单分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "支付单分页查询")
public class PaymentOrderPageQuery extends PageQuery {

    @Schema(description = "业务支付单 ID")
    private Long bizOrderId;

    @Schema(description = "渠道编码")
    private String channelCode;

    @Schema(description = "支付单状态")
    private String status;
}
