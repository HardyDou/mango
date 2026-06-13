package io.mango.payment.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.AuditableEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_virtual_channel_payment")
public class PaymentVirtualChannelPayment extends AuditableEntity {

    private String virtualPaymentNo;

    private String payOrderNo;

    private String channelTradeNo;

    private Long cashierConfigId;

    private Long paymentMethodId;

    private String paymentMethodCode;

    private String title;

    private Long amount;

    private String payerName;

    private String status;

    private LocalDateTime paidTime;

    private Long tenantId;
}
