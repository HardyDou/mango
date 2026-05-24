package io.mango.payment.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 退款单分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "退款单分页查询")
public class RefundOrderPageQuery extends PageQuery {

    @Schema(description = "业务支付单 ID")
    private Long bizOrderId;

    @Schema(description = "商户退款单号，支持模糊搜索")
    private String merchantRefundNo;

    @Schema(description = "退款单状态")
    private String status;
}
