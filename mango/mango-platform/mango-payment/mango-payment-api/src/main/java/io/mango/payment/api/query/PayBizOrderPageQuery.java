package io.mango.payment.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业务支付单分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "业务支付单分页查询")
public class PayBizOrderPageQuery extends PageQuery {

    @Schema(description = "应用编码")
    private String appCode;

    @Schema(description = "商户业务单号，支持模糊搜索")
    private String merchantOrderNo;

    @Schema(description = "业务单状态")
    private String status;
}
