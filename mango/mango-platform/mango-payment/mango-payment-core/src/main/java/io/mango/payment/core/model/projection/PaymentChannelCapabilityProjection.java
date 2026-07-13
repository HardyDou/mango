package io.mango.payment.core.model.projection;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class PaymentChannelCapabilityProjection implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long channelId;

    private String channelName;

    private String methodCode;

    private String methodName;

    private String terminalType;

    private String environment;

    private Integer supportsRefund;

    private Integer supportsQuery;

    private Integer supportsClose;

    private Integer supportsBill;

    private Integer supportsReconcile;

    private Long minAmount;

    private Long maxAmount;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
