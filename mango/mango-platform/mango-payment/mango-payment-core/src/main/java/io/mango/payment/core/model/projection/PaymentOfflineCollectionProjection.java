package io.mango.payment.core.model.projection;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaymentOfflineCollectionProjection {

    private Long id;

    private String offlineCollectionNo;

    private Long paymentOrderId;

    private String payOrderNo;

    private Long businessOrderId;

    private String bizOrderNo;

    private String title;

    private String appId;

    private Long channelId;

    private String channelCode;

    private String channelName;

    private Long contractId;

    private String contractName;

    private Long contractCapabilityId;

    private Long subjectId;

    private String subjectName;

    private Long bankAccountId;

    private String accountName;

    private String accountNoMask;

    private String bankName;

    private Long amount;

    private String currency;

    private Long transferAmount;

    private String voucherFileIds;

    private LocalDateTime submittedTime;

    private String submitRemark;

    private Long confirmedAmount;

    private Long confirmedBy;

    private String confirmedByName;

    private String confirmRemark;

    private String reconciliationCode;

    private String transferRemark;

    private Integer voucherCount;

    private String collectionStatus;

    private String collectionStatusName;

    private LocalDateTime expireTime;

    private LocalDateTime confirmedTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
