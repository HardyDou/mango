package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 支付管理配置项视图。
 */
@Data
@Schema(description = "支付管理配置项视图")
public class PaymentManageItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "配置项 ID")
    private Long id;

    @Schema(description = "管理域编码")
    private String domain;

    @Schema(description = "配置项编码")
    private String code;

    @Schema(description = "配置项名称")
    private String name;

    @Schema(description = "配置归属")
    private String owner;

    @Schema(description = "配置状态")
    private String status;

    @Schema(description = "主要配置摘要")
    private String primaryText;

    @Schema(description = "辅助说明")
    private String secondaryText;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
