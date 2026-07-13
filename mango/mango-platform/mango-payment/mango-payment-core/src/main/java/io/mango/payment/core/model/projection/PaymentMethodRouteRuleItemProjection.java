package io.mango.payment.core.model.projection;

import lombok.Data;

import java.io.Serializable;

@Data
public class PaymentMethodRouteRuleItemProjection implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long ruleId;

    private Long contractCapabilityId;

    private Long contractId;

    private String contractName;

    private Long channelId;

    private String channelName;

    private String methodCode;

    private String terminalType;

    private Integer priority;

    private Integer weight;

    private Long minAmount;

    private Long maxAmount;

    private Integer status;
}
