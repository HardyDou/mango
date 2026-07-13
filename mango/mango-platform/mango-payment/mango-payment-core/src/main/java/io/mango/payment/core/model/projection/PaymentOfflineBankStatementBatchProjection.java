package io.mango.payment.core.model.projection;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PaymentOfflineBankStatementBatchProjection {

    private Long id;

    private String batchNo;

    private String bankAccountNoMask;

    private String bankName;

    private Long statementFileId;

    private String statementFileName;

    private String fileDigest;

    private Integer totalCount;

    private Integer matchedCount;

    private Integer confirmedCount;

    private Integer differenceCount;

    private String batchStatus;

    private String batchStatusName;

    private String importerName;

    private LocalDateTime importTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<PaymentOfflineBankStatementItemProjection> items;
}
