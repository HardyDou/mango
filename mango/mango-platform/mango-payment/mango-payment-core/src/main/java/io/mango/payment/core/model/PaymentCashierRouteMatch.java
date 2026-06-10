package io.mango.payment.core.model;

import lombok.Data;

@Data
public class PaymentCashierRouteMatch {

    private Long contractId;

    private String contractName;

    private Long channelId;

    private String channelCode;

    private String channelName;

    private Long contractCapabilityId;

    private Long routeRuleId;

    private String channelMerchantNo;
}
