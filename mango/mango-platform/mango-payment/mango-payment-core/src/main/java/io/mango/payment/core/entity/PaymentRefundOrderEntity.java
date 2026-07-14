package io.mango.payment.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_refund_order")
public class PaymentRefundOrderEntity extends PaymentBaseEntity {

    private String refundOrderNo;

    private String bizRefundNo;

    private Long paymentOrderId;

    private String channelRefundNo;

    private Long refundAmount;

    private String reason;

    private String status;

    private LocalDateTime refundTime;
}
