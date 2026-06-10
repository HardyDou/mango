package io.mango.payment.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "支付配置分页查询")
public class PaymentConfigPageQuery extends PageQuery {

    @Schema(description = "关键词")
    private String keyword;

    @Schema(description = "状态：1-启用，0-停用")
    private Integer status;

    @Schema(description = "业务状态编码")
    private String statusCode;

    @Schema(description = "接入应用 ID")
    private Long applicationId;

    @Schema(description = "企业主体 ID")
    private Long enterpriseSubjectId;

    @Schema(description = "支付通道 ID")
    private Long channelId;
}
