package io.mango.payment.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "支付方式路由规则分页查询")
public class PaymentMethodRoutePageQuery extends PageQuery {

    @Schema(description = "关键词")
    @Size(max = 100, message = "关键词不能超过 100 个字符")
    private String keyword;

    @Schema(description = "应用 ID")
    @Positive(message = "应用 ID 必须大于 0")
    private Long applicationId;

    @Schema(description = "企业主体 ID")
    @Positive(message = "企业主体 ID 必须大于 0")
    private Long subjectId;

    @Schema(description = "标准支付方式编码")
    @Size(max = 64, message = "标准支付方式编码不能超过 64 个字符")
    private String methodCode;

    @Schema(description = "终端类型")
    @Size(max = 32, message = "终端类型不能超过 32 个字符")
    private String terminalType;

    @Schema(description = "内部路由域")
    @Size(max = 32, message = "内部路由域不能超过 32 个字符")
    private String environment;

    @Schema(description = "状态：1-启用，0-停用")
    @Min(value = 0, message = "状态不能小于 0")
    @Max(value = 1, message = "状态不能大于 1")
    private Integer status;
}
