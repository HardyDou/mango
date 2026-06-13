package io.mango.payment.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.AuditableEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_transaction_flow")
public class PaymentTransactionFlowEntity extends AuditableEntity {

    private String flowNo;

    private Long businessOrderId;

    private Long paymentOrderId;

    private Long refundOrderId;

    private String flowType;

    private Long amount;

    private Long tenantId;
}
