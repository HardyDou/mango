package io.mango.payment.core.model;

import lombok.Data;

@Data
public class PaymentMethodRouteCandidate {

    private Long routeRuleId;

    private String ruleCode;

    private String ruleName;

    private Long appId;

    private String appName;

    private Long subjectId;

    private String subjectName;

    private String methodCode;

    private String methodName;

    private String terminalType;

    private String environment;

    private String routeMode;

    private Integer fallbackEnabled;

    private Long routeItemId;

    private Long contractCapabilityId;

    private Long contractId;

    private String contractName;

    private Long channelId;

    private String channelName;

    private Integer itemPriority;

    private Integer itemWeight;

    private Long itemMinAmount;

    private Long itemMaxAmount;

    private Integer itemStatus;

    private Long capabilityMinAmount;

    private Long capabilityMaxAmount;

    private Integer capabilityStatus;

    private Integer contractStatus;

    private Integer channelStatus;
}
