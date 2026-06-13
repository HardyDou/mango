package io.mango.payment.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PaymentChannelBillFetchBatchVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long sourceId;
    private String batchNo;
    private Long reconciliationId;
    private String reconciliationNo;
    private String channelCode;
    private String fetchMode;
    private String fetchModeName;
    private LocalDate billDate;
    private LocalDateTime requestStartTime;
    private LocalDateTime requestEndTime;
    private String requestCursor;
    private Integer requestPage;
    private Integer pageSize;
    private String responseDigest;
    private Integer totalCount;
    private String fetchStatus;
    private String fetchStatusName;
    private String fetchResult;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime fetchStartTime;
    private LocalDateTime fetchEndTime;
    private LocalDateTime createTime;
}
