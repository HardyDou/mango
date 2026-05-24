package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 支付单记录视图。
 */
@Data
@Schema(description = "支付单记录视图")
public class PaymentOrderRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "支付单 ID")
    private Long id;

    @Schema(description = "业务支付单 ID")
    private Long bizOrderId;

    @Schema(description = "渠道编码")
    private String channelCode;

    @Schema(description = "渠道支付单号")
    private String channelOrderNo;

    @Schema(description = "支付方式")
    private String payMethod;

    @Schema(description = "幂等键")
    private String idempotencyKey;

    @Schema(description = "支付金额，单位分")
    private Long amount;

    @Schema(description = "支付单状态")
    private String status;

    @Schema(description = "支付材料类型")
    private String materialType;

    @Schema(description = "支付材料内容")
    private String materialContent;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
