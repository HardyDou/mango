package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 支付订单同步状态结果。
 */
@Data
@Schema(description = "支付订单同步状态结果")
public class PaymentOrderSyncStatusVO {

    @Schema(description = "支付订单号")
    private String payOrderNo;

    @Schema(description = "支付订单状态编码")
    private String status;

    @Schema(description = "支付订单状态名称")
    private String statusName;

    @Schema(description = "交易流水号")
    private String flowNo;

    @Schema(description = "本次同步是否推进了本地状态")
    private Boolean changed;

    @Schema(description = "累计主动查单次数")
    private Long queryCount;

    @Schema(description = "最近一次查单处理结果")
    private String lastQueryResult;
}
