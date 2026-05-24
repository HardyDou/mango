package io.mango.payment.api.vo;

import io.mango.payment.api.enums.RefundOrderStatus;
import lombok.Data;

import java.io.Serializable;

@Data
public class RefundOrderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long refundOrderId;

    private Long bizOrderId;

    private Long paymentOrderId;

    private String merchantRefundNo;

    private Long refundAmount;

    private RefundOrderStatus status;
}
