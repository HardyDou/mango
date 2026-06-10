package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "交易流水")
public class PaymentTransactionFlowVO {

    @Schema(description = "交易流水 ID")
    private Long id;

    @Schema(description = "流水号")
    private String flowNo;

    @Schema(description = "业务订单 ID")
    private Long businessOrderId;

    @Schema(description = "业务订单号")
    private String bizOrderNo;

    @Schema(description = "支付订单 ID")
    private Long paymentOrderId;

    @Schema(description = "支付订单号")
    private String payOrderNo;

    @Schema(description = "退款订单 ID")
    private Long refundOrderId;

    @Schema(description = "退款订单号")
    private String refundOrderNo;

    @Schema(description = "流水类型编码")
    private String flowType;

    @Schema(description = "流水类型名称")
    private String flowTypeName;

    @Schema(description = "金额，单位分")
    private Long amount;

    @Schema(description = "币种")
    private String currency;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
