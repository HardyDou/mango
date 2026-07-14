package io.mango.payment.core.model.projection;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaymentOrderStatusFlowProjection {

    private String fromStatus;

    private String toStatus;

    private String statusCode;

    private String statusName;

    private LocalDateTime happenTime;

    private String triggerSource;

    private String source;

    private String triggerNo;

    private Long operatorId;

    private String operatorName;

    private String remark;
}
