package io.mango.payment.core.model.projection;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaymentOfflineBankStatementItemProjection {

    private Long id;

    private Long batchId;

    private String batchNo;

    private Integer rowNo;

    private String bankStatementNo;

    private String bankAccountNoMask;

    private String bankName;

    private LocalDateTime tradeTime;

    private Long amount;

    private String currency;

    private String counterpartyName;

    private String counterpartyAccountNoMask;

    private String summary;

    private String remark;

    private String reconciliationCode;

    private Long matchedOfflineCollectionId;

    private String matchedOfflineCollectionNo;

    private String matchedPayOrderNo;

    private String matchStatus;

    private String matchStatusName;

    private String matchMessage;

    private LocalDateTime confirmedTime;

    private String confirmedByName;

    private String confirmRemark;

    private LocalDateTime createTime;
}
