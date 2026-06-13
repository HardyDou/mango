package io.mango.payment.core.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaymentCashierPayResultRow {

    private Long paymentOrderId;

    private String payOrderNo;

    private Long amount;

    private String status;

    private LocalDateTime updatedAt;

    private String channelCode;

    private String channelName;

    private String methodCode;

    private String methodName;

    private String paymentMaterialType;

    private String paymentMaterialJson;

    private Long businessOrderId;

    private String bizOrderNo;

    private String appCode;

    private String title;

    private Long subjectId;

    private String currency;

    private LocalDateTime expireTime;
}
