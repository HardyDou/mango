package io.mango.payment.core.model.projection;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PaymentDifferenceProjection {

    private Long id;

    private String differenceNo;

    private Long reconciliationId;

    private String reconciliationNo;

    private String channelCode;

    private LocalDate billDate;

    private String relatedOrderNo;

    private String differenceType;

    private String differenceTypeName;

    private Long differenceAmount;

    private String processStatus;

    private String processStatusName;

    private String processAction;

    private String processReason;

    private String processResult;

    private String processEvidence;

    private Long adjustFlowId;

    private String adjustFlowNo;

    private Long processorId;

    private String processorName;

    private LocalDateTime processTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
