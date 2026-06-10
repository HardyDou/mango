package io.mango.payment.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.AuditableEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_settlement_summary")
public class PaymentSettlementSummaryEntity extends AuditableEntity {

    private LocalDate settlementDate;

    private String appCode;

    private Long enterpriseSubjectId;

    private String channelCode;

    private Long tradeAmount;

    private Long refundAmount;

    private Long feeAmount;

    private Long netAmount;

    private Integer tradeCount;

    private Integer refundCount;

    private Integer unresolvedDifferenceCount;

    private Long unresolvedDifferenceAmount;

    private String status;

    private Long generatedBy;

    private String generatedByName;

    private LocalDateTime generatedAt;

    private Long confirmedBy;

    private String confirmedByName;

    private LocalDateTime confirmedAt;

    private Long voidedBy;

    private String voidedByName;

    private LocalDateTime voidedAt;

    private String voidReason;

    private Long tenantId;

    @TableLogic
    @TableField("del_flag")
    private Integer delFlag = 0;
}
