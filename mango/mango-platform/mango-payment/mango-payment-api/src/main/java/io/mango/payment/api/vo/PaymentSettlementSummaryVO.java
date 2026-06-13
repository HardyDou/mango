package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "支付结算汇总")
public class PaymentSettlementSummaryVO {

    @Schema(description = "结算汇总 ID")
    private Long id;

    @Schema(description = "结算日期")
    private LocalDate settlementDate;

    @Schema(description = "应用编码")
    private String appCode;

    @Schema(description = "应用名称")
    private String appName;

    @Schema(description = "企业主体 ID")
    private Long enterpriseSubjectId;

    @Schema(description = "企业主体名称")
    private String subjectName;

    @Schema(description = "通道编码")
    private String channelCode;

    @Schema(description = "通道名称")
    private String channelName;

    @Schema(description = "支付成功金额，单位分")
    private Long tradeAmount;

    @Schema(description = "退款成功金额，单位分")
    private Long refundAmount;

    @Schema(description = "通道手续费，单位分")
    private Long feeAmount;

    @Schema(description = "净收款金额，单位分")
    private Long netAmount;

    @Schema(description = "支付成功笔数")
    private Integer tradeCount;

    @Schema(description = "退款成功笔数")
    private Integer refundCount;

    @Schema(description = "未解决差异笔数")
    private Integer unresolvedDifferenceCount;

    @Schema(description = "未解决差异金额，单位分")
    private Long unresolvedDifferenceAmount;

    @Schema(description = "状态编码")
    private String status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "生成人 ID")
    private Long generatedBy;

    @Schema(description = "生成人名称")
    private String generatedByName;

    @Schema(description = "生成时间")
    private LocalDateTime generatedAt;

    @Schema(description = "确认人 ID")
    private Long confirmedBy;

    @Schema(description = "确认人名称")
    private String confirmedByName;

    @Schema(description = "确认时间")
    private LocalDateTime confirmedAt;

    @Schema(description = "作废人 ID")
    private Long voidedBy;

    @Schema(description = "作废人名称")
    private String voidedByName;

    @Schema(description = "作废时间")
    private LocalDateTime voidedAt;

    @Schema(description = "作废原因")
    private String voidReason;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
