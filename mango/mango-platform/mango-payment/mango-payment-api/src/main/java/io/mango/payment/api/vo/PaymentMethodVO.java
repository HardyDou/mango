package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.Data;

/**
 * 支付方式视图。
 */
@Data
@Schema(description = "支付方式视图")
public class PaymentMethodVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "支付方式编码")
    private String code;

    @Schema(description = "支付方式名称")
    private String label;

    @Schema(description = "支付通道编码")
    private String channelCode;

    @Schema(description = "支付方式状态")
    private String status;

    @Schema(description = "单笔限额，单位分")
    private Long singleLimit;

    @Schema(description = "展示顺序")
    private Integer sortOrder;
}
