package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "支付对账差异")
public class PaymentDifferenceVO {

    @Schema(description = "对账差异 ID")
    private Long id;

    @Schema(description = "差异单号")
    private String differenceNo;

    @Schema(description = "对账批次 ID")
    private Long reconciliationId;

    @Schema(description = "对账批次号")
    private String reconciliationNo;

    @Schema(description = "通道编码")
    private String channelCode;

    @Schema(description = "账单日期")
    private LocalDate billDate;

    @Schema(description = "关联订单号")
    private String relatedOrderNo;

    @Schema(description = "差异类型编码")
    private String differenceType;

    @Schema(description = "差异类型名称")
    private String differenceTypeName;

    @Schema(description = "差异金额，单位分")
    private Long differenceAmount;

    @Schema(description = "处理状态编码")
    private String processStatus;

    @Schema(description = "处理状态名称")
    private String processStatusName;

    @Schema(description = "处理动作")
    private String processAction;

    @Schema(description = "处理原因")
    private String processReason;

    @Schema(description = "处理结果")
    private String processResult;

    @Schema(description = "处理凭据文件 ID 或业务凭据 token")
    private String processEvidence;

    @Schema(description = "差异处理备注流水 ID")
    private Long adjustFlowId;

    @Schema(description = "差异处理备注流水号")
    private String adjustFlowNo;

    @Schema(description = "处理人 ID")
    private Long processorId;

    @Schema(description = "处理人名称")
    private String processorName;

    @Schema(description = "处理时间")
    private LocalDateTime processTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
