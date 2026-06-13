package io.mango.payment.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.AuditableEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_notification_record")
public class PaymentNotificationRecordEntity extends AuditableEntity {

    private String notificationNo;

    private String relatedOrderNo;

    private String notificationType;

    private String targetUrl;

    private String notifyStatus;

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

    private Long tenantId;

    @TableLogic
    @TableField("del_flag")
    private Integer delFlag = 0;
}
