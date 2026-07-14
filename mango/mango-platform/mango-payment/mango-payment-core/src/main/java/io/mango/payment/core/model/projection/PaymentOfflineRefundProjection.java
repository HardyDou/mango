package io.mango.payment.core.model.projection;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaymentOfflineRefundProjection {

    private Long id;

    private String offlineRefundNo;

    private Long offlineCollectionId;

    private String offlineCollectionNo;

    private Long refundOrderId;

    private String refundOrderNo;

    private Long paymentOrderId;

    private String payOrderNo;

    private Long businessOrderId;

    private String bizOrderNo;

    private String title;

    private String appId;

    private Long channelId;

    private String channelCode;

    private String channelName;

    private Long refundAmount;

    private String currency;

    private String refundAccountName;

    private String refundAccountNoMask;

    private String refundBankName;

    private String refundVoucherFileIds;

    private Integer refundVoucherCount;

    private String reason;

    private String remark;

    private String refundStatus;

    private String refundStatusName;

    private LocalDateTime refundedTime;

    private Long operatorId;

    private String operatorName;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
