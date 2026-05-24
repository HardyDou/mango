package io.mango.payment.api.vo;

import io.mango.payment.api.enums.PayBizOrderStatus;
import lombok.Data;

import java.io.Serializable;

@Data
public class PayBizOrderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long bizOrderId;

    private String merchantOrderNo;

    private Long amount;

    private Long refundedAmount;

    private String currency;

    private PayBizOrderStatus status;
}
