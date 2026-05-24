package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.Data;

/**
 * 支付管理域视图。
 */
@Data
@Schema(description = "支付管理域视图")
public class PaymentManageDomainVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "管理域编码")
    private String code;

    @Schema(description = "管理域名称")
    private String title;

    @Schema(description = "管理域说明")
    private String description;

    @Schema(description = "短标签")
    private String badge;

    @Schema(description = "展示顺序")
    private Integer sortOrder;
}
