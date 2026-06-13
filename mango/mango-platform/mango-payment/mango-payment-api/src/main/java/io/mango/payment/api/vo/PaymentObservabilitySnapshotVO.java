package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "支付可观测性指标快照")
public class PaymentObservabilitySnapshotVO {

    @Schema(description = "支付订单总数")
    private Long paymentTotalCount;

    @Schema(description = "支付成功数")
    private Long paymentSuccessCount;

    @Schema(description = "支付失败数")
    private Long paymentFailedCount;

    @Schema(description = "支付中积压数")
    private Long paymentBacklogCount;

    @Schema(description = "支付成功率")
    private BigDecimal paymentSuccessRate;

    @Schema(description = "通道失败率")
    private BigDecimal channelFailureRate;

    @Schema(description = "回调失败数")
    private Long callbackFailureCount;

    @Schema(description = "通知失败数")
    private Long notificationFailureCount;

    @Schema(description = "退款总数")
    private Long refundTotalCount;

    @Schema(description = "退款成功数")
    private Long refundSuccessCount;

    @Schema(description = "退款失败数")
    private Long refundFailureCount;

    @Schema(description = "退款成功率")
    private BigDecimal refundSuccessRate;

    @Schema(description = "对账差异数")
    private Long differenceCount;

    @Schema(description = "未处理异常订单数")
    private Long unhandledExceptionCount;

    @Schema(description = "证书即将过期数")
    private Long expiringCertificateCount;

    @Schema(description = "告警项")
    private List<PaymentObservabilityAlertVO> alerts;
}
