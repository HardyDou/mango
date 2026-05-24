package io.mango.payment.api.vo;

import io.mango.payment.api.enums.PaymentMaterialType;
import io.mango.payment.api.enums.PaymentOrderStatus;
import lombok.Data;

import java.io.Serializable;

@Data
public class PaymentOrderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long paymentOrderId;

    private Long bizOrderId;

    private String channelCode;

    private Long amount;

    private PaymentOrderStatus status;

    private PaymentMaterialType materialType;

    private String materialContent;
}
