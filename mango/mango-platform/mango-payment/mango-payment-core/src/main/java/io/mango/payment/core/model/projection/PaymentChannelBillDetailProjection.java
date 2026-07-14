package io.mango.payment.core.model.projection;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PaymentChannelBillDetailProjection {

    private Long id;

    private Long reconciliationId;

    private String batchNo;

    private String channelCode;

    private LocalDate billDate;

    private String channelTradeNo;

    private String tradeType;

    private String tradeTypeName;

    private Long amount;

    private Long fee;

    private LocalDateTime tradeTime;

    private String matchStatus;

    private String matchStatusName;

    private String matchedOrderNo;

    private String matchMessage;

    private LocalDateTime createTime;
}
