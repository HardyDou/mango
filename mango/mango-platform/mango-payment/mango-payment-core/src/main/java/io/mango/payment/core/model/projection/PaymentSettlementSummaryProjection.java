package io.mango.payment.core.model.projection;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PaymentSettlementSummaryProjection {

    private Long id;

    private LocalDate settlementDate;

    private String appCode;

    private String appName;

    private Long enterpriseSubjectId;

    private String subjectName;

    private String channelCode;

    private String channelName;

    private Long tradeAmount;

    private Long refundAmount;

    private Long feeAmount;

    private Long netAmount;

    private Integer tradeCount;

    private Integer refundCount;

    private Integer unresolvedDifferenceCount;

    private Long unresolvedDifferenceAmount;

    private String status;

    private String statusName;

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

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
