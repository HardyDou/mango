package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 业务支付单记录视图。
 */
@Data
@Schema(description = "业务支付单记录视图")
public class PayBizOrderRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "业务支付单 ID")
    private Long id;

    @Schema(description = "应用编码")
    private String appCode;

    @Schema(description = "商户业务单号")
    private String merchantOrderNo;

    @Schema(description = "订单标题")
    private String subject;

    @Schema(description = "支付金额，单位分")
    private Long amount;

    @Schema(description = "已退款金额，单位分")
    private Long refundedAmount;

    @Schema(description = "币种")
    private String currency;

    @Schema(description = "业务单状态")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
