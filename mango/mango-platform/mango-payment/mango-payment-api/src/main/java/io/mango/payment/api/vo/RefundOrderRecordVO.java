package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 退款单记录视图。
 */
@Data
@Schema(description = "退款单记录视图")
public class RefundOrderRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "退款单 ID")
    private Long id;

    @Schema(description = "业务支付单 ID")
    private Long bizOrderId;

    @Schema(description = "支付单 ID")
    private Long paymentOrderId;

    @Schema(description = "商户退款单号")
    private String merchantRefundNo;

    @Schema(description = "渠道退款单号")
    private String channelRefundNo;

    @Schema(description = "幂等键")
    private String idempotencyKey;

    @Schema(description = "退款金额，单位分")
    private Long refundAmount;

    @Schema(description = "退款单状态")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
