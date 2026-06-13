package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "支付通知记录")
public class PaymentNotificationRecordVO {

    @Schema(description = "通知记录 ID")
    private Long id;

    @Schema(description = "通知单号")
    private String notificationNo;

    @Schema(description = "关联订单号")
    private String relatedOrderNo;

    @Schema(description = "通知类型编码")
    private String notificationType;

    @Schema(description = "通知类型名称")
    private String notificationTypeName;

    @Schema(description = "通知目标地址")
    private String targetUrl;

    @Schema(description = "通知状态编码")
    private String notifyStatus;

    @Schema(description = "通知状态名称")
    private String notifyStatusName;

    @Schema(description = "重试次数")
    private Integer retryTimes;

    @Schema(description = "计划通知时间")
    private LocalDateTime scheduledNotifyTime;

    @Schema(description = "下一次重试时间")
    private LocalDateTime nextRetryTime;

    @Schema(description = "通知报文快照")
    private String payloadJson;

    @Schema(description = "响应码")
    private String responseCode;

    @Schema(description = "响应信息")
    private String responseMessage;

    @Schema(description = "最后人工重推时间")
    private LocalDateTime lastManualRetryTime;

    @Schema(description = "最后人工重推原因")
    private String lastManualRetryReason;

    @Schema(description = "最后人工重推结果")
    private String lastManualRetryResult;

    @Schema(description = "最后人工重推人 ID")
    private Long lastManualRetryOperatorId;

    @Schema(description = "最后人工重推人名称")
    private String lastManualRetryOperatorName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
