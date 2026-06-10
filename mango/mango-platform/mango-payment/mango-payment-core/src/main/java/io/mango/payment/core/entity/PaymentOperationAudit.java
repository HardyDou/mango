package io.mango.payment.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.AuditableEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_operation_audit")
public class PaymentOperationAudit extends AuditableEntity {

    private Long operatorId;

    private String operatorName;

    private String operationAction;

    private String resourceType;

    private String resourceId;

    private String operationResult;

    private LocalDateTime operationTime;

    private Long tenantId;
}
