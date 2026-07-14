package io.mango.payment.core.model.projection;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaymentTransactionFlowProjection {

    private Long id;

    private String flowNo;

    private Long businessOrderId;

    private String bizOrderNo;

    private Long paymentOrderId;

    private String payOrderNo;

    private Long refundOrderId;

    private String refundOrderNo;

    private String flowType;

    private String flowTypeName;

    private Long amount;

    private String currency;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
