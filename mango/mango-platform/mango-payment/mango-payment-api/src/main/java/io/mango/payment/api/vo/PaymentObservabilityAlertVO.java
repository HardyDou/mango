package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "支付可观测性告警项")
public class PaymentObservabilityAlertVO {

    @Schema(description = "告警类型")
    private String alertType;

    @Schema(description = "告警级别")
    private String severity;

    @Schema(description = "告警对象")
    private String target;

    @Schema(description = "当前值")
    private String currentValue;

    @Schema(description = "阈值")
    private String threshold;

    @Schema(description = "告警说明")
    private String message;
}
