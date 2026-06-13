package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "线下银行流水明细")
public class PaymentOfflineBankStatementItemVO {

    @Schema(description = "流水明细 ID")
    private Long id;

    @Schema(description = "导入批次 ID")
    private Long batchId;

    @Schema(description = "导入批次号")
    private String batchNo;

    @Schema(description = "Excel 行号")
    private Integer rowNo;

    @Schema(description = "银行流水号")
    private String bankStatementNo;

    @Schema(description = "脱敏收款账号")
    private String bankAccountNoMask;

    @Schema(description = "收款开户行")
    private String bankName;

    @Schema(description = "交易时间")
    private LocalDateTime tradeTime;

    @Schema(description = "收入金额，单位分")
    private Long amount;

    @Schema(description = "币种")
    private String currency;

    @Schema(description = "对方户名")
    private String counterpartyName;

    @Schema(description = "脱敏对方账号")
    private String counterpartyAccountNoMask;

    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "解析出的随机对账码")
    private String reconciliationCode;

    @Schema(description = "匹配线下收款 ID")
    private Long matchedOfflineCollectionId;

    @Schema(description = "匹配线下收款单号")
    private String matchedOfflineCollectionNo;

    @Schema(description = "匹配支付订单号")
    private String matchedPayOrderNo;

    @Schema(description = "匹配状态编码")
    private String matchStatus;

    @Schema(description = "匹配状态名称")
    private String matchStatusName;

    @Schema(description = "匹配说明")
    private String matchMessage;

    @Schema(description = "确认时间")
    private LocalDateTime confirmedTime;

    @Schema(description = "确认人名称")
    private String confirmedByName;

    @Schema(description = "确认说明")
    private String confirmRemark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
