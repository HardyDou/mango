package io.mango.payment.core.model.projection;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaymentNotificationRecordProjection {

    private Long id;

    private String notificationNo;

    private String relatedOrderNo;

    private String notificationType;

    private String notificationTypeName;

    private String targetUrl;

    private String notifyStatus;

    private String notifyStatusName;

    private Integer retryTimes;

    private LocalDateTime scheduledNotifyTime;

    private LocalDateTime nextRetryTime;

    private String payloadJson;

    private String responseCode;

    private String responseMessage;

    private LocalDateTime lastManualRetryTime;

    private String lastManualRetryReason;

    private String lastManualRetryResult;

    private Long lastManualRetryOperatorId;

    private String lastManualRetryOperatorName;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
