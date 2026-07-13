package io.mango.payment.core.model.projection;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaymentExceptionOrderProjection {

    private Long id;

    private String exceptionNo;

    private String relatedOrderNo;

    private String exceptionType;

    private String exceptionTypeName;

    private String severity;

    private String severityName;

    private String handleStatus;

    private String handleStatusName;

    private String reason;

    private String handleAction;

    private String handleReason;

    private String handleResult;

    private String handleEvidence;

    private Long handlerId;

    private String handlerName;

    private LocalDateTime handleTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
