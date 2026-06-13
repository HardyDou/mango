package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "支付对账批次")
public class PaymentReconciliationVO {

    @Schema(description = "对账批次 ID")
    private Long id;

    @Schema(description = "对账批次号")
    private String reconciliationNo;

    @Schema(description = "通道编码")
    private String channelCode;

    @Schema(description = "账单日期")
    private LocalDate billDate;

    @Schema(description = "账单笔数")
    private Integer totalCount;

    @Schema(description = "账单金额，单位分")
    private Long totalAmount;

    @Schema(description = "通道手续费，单位分")
    private Long totalFee;

    @Schema(description = "匹配状态编码")
    private String matchStatus;

    @Schema(description = "匹配状态名称")
    private String matchStatusName;

    @Schema(description = "账单文件 ID")
    private Long billFileId;

    @Schema(description = "账单文件名")
    private String billFileName;

    @Schema(description = "账单文件摘要")
    private String fileDigest;

    @Schema(description = "导入人 ID")
    private Long importerId;

    @Schema(description = "导入人名称")
    private String importerName;

    @Schema(description = "导入时间")
    private LocalDateTime importTime;

    @Schema(description = "对账结果说明")
    private String reconcileResult;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "账单明细")
    private List<PaymentChannelBillDetailVO> details;
}
