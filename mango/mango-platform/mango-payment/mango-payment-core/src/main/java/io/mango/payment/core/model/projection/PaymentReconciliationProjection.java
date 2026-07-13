package io.mango.payment.core.model.projection;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PaymentReconciliationProjection {

    private Long id;

    private String reconciliationNo;

    private String channelCode;

    private LocalDate billDate;

    private Integer totalCount;

    private Long totalAmount;

    private Long totalFee;

    private String matchStatus;

    private String matchStatusName;

    private Long billFileId;

    private String billFileName;

    private String fileDigest;

    private Long importerId;

    private String importerName;

    private LocalDateTime importTime;

    private String reconcileResult;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<PaymentChannelBillDetailProjection> details;
}
