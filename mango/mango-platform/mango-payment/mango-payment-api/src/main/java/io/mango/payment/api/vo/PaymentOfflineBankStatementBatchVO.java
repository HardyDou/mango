package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "线下银行流水导入批次")
public class PaymentOfflineBankStatementBatchVO {

    @Schema(description = "批次 ID")
    private Long id;

    @Schema(description = "批次号")
    private String batchNo;

    @Schema(description = "脱敏收款账号")
    private String bankAccountNoMask;

    @Schema(description = "收款开户行")
    private String bankName;

    @Schema(description = "银行流水文件 ID")
    private Long statementFileId;

    @Schema(description = "银行流水文件名")
    private String statementFileName;

    @Schema(description = "文件摘要")
    private String fileDigest;

    @Schema(description = "总笔数")
    private Integer totalCount;

    @Schema(description = "匹配笔数")
    private Integer matchedCount;

    @Schema(description = "确认笔数")
    private Integer confirmedCount;

    @Schema(description = "差异笔数")
    private Integer differenceCount;

    @Schema(description = "批次状态编码")
    private String batchStatus;

    @Schema(description = "批次状态名称")
    private String batchStatusName;

    @Schema(description = "导入人名称")
    private String importerName;

    @Schema(description = "导入时间")
    private LocalDateTime importTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "流水明细")
    private List<PaymentOfflineBankStatementItemVO> items;
}
