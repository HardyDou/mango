package io.mango.payment.core.model.projection;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PaymentBusinessOrderProjection {

    private Long id;

    private String bizOrderNo;

    private String appId;

    private String appName;

    private String title;

    private Long subjectId;

    private String subjectName;

    private Long cashierConfigId;

    private String cashierName;

    private Long amount;

    private Long paidAmount;

    private Long refundedAmount;

    private String currency;

    private String status;

    private String statusName;

    private Boolean payable;

    private String payDisabledReason;

    private LocalDateTime expireTime;

    private String notifyUrl;

    private String returnUrl;

    private String extendInfo;

    private Long paymentOrderCount;

    private Long refundOrderCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<PaymentOrderStatusFlowProjection> statusFlows;
}
