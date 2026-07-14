package io.mango.payment.core.model.projection;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PaymentRefundOrderProjection {

    private Long id;

    private String refundOrderNo;

    private String bizRefundNo;

    private Long paymentOrderId;

    private Long contractId;

    private Long businessOrderId;

    private String payOrderNo;

    private String bizOrderNo;

    private String title;

    private String appId;

    private String methodCode;

    private String methodName;

    private String channelCode;

    private String channelName;

    private String channelMerchantNo;

    private String channelTradeNo;

    private String channelRefundNo;

    private Long refundAmount;

    private String currency;

    private String reason;

    private String status;

    private String statusName;

    private LocalDateTime refundTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private String flowNo;

    private List<PaymentOrderStatusFlowProjection> statusFlows;
}
