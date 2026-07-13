package io.mango.payment.core.model.projection;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaymentOperationAuditProjection {

    private Long id;

    private Long operatorId;

    private String operatorName;

    private String operationAction;

    private String resourceType;

    private String resourceId;

    private String operationResult;

    private LocalDateTime operationTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
