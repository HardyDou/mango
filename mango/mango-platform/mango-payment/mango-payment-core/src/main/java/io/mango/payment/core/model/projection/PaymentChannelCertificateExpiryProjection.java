package io.mango.payment.core.model.projection;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class PaymentChannelCertificateExpiryProjection implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long contractId;

    private String contractName;

    private Long contractCapabilityId;

    private String channelCode;

    private String channelName;

    private Long subjectId;

    private String subjectName;

    private String methodCode;

    private String terminalType;

    private LocalDateTime certificateExpireTime;

    private Long daysToExpire;
}
