package io.mango.payment.core.model.projection;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PaymentOrderProjection {

    private Long id;

    private String payOrderNo;

    private Long businessOrderId;

    private String bizOrderNo;

    private String title;

    private String appId;

    private Long subjectId;

    private String subjectName;

    private Long cashierConfigId;

    private String cashierName;

    private Long methodId;

    private String methodCode;

    private String methodName;

    private Long channelId;

    private String channelCode;

    private String channelName;

    private String channelMerchantNo;

    private Long contractId;

    private String contractName;

    private Long contractCapabilityId;

    private Long routeRuleId;

    private Long amount;

    private Long refundedAmount;

    private Long occupyingRefundAmount;

    private Long refundableAmount;

    private String currency;

    private String status;

    private String statusName;

    private String channelTradeNo;

    private Integer successFlag;

    private LocalDateTime payTime;

    private LocalDateTime expireTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private String flowNo;

    private List<PaymentOrderStatusFlowProjection> statusFlows;
}
