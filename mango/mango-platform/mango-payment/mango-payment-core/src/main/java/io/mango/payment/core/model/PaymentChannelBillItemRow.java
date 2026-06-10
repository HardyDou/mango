package io.mango.payment.core.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通道账单明细行。
 */
@Data
public class PaymentChannelBillItemRow {

    private String channelTradeNo;

    private String tradeType;

    private Long amount;

    private Long fee;

    private LocalDateTime tradeTime;
}
