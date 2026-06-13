package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "通道账单明细")
public class PaymentChannelBillDetailVO {

    @Schema(description = "账单明细 ID")
    private Long id;

    @Schema(description = "对账批次 ID")
    private Long reconciliationId;

    @Schema(description = "账单批次号")
    private String batchNo;

    @Schema(description = "通道编码")
    private String channelCode;

    @Schema(description = "账单日期")
    private LocalDate billDate;

    @Schema(description = "通道交易号")
    private String channelTradeNo;

    @Schema(description = "交易类型编码")
    private String tradeType;

    @Schema(description = "交易类型名称")
    private String tradeTypeName;

    @Schema(description = "金额，单位分")
    private Long amount;

    @Schema(description = "手续费，单位分")
    private Long fee;

    @Schema(description = "通道交易时间")
    private LocalDateTime tradeTime;

    @Schema(description = "匹配状态编码")
    private String matchStatus;

    @Schema(description = "匹配状态名称")
    private String matchStatusName;

    @Schema(description = "匹配到的支付订单号")
    private String matchedOrderNo;

    @Schema(description = "匹配说明")
    private String matchMessage;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
