package io.mango.payment.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "支付方式路由规则分页查询")
public class PaymentMethodRoutePageQuery extends PageQuery {

    @Schema(description = "关键词")
    private String keyword;

    @Schema(description = "应用 ID")
    private Long applicationId;

    @Schema(description = "企业主体 ID")
    private Long subjectId;

    @Schema(description = "标准支付方式编码")
    private String methodCode;

    @Schema(description = "终端类型")
    private String terminalType;

    @Schema(description = "接入场景")
    private String environment;

    @Schema(description = "状态：1-启用，0-停用")
    private Integer status;
}
