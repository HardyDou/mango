package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "重推支付通知记录命令")
public class RetryPaymentNotificationRecordCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "通知记录 ID 不能为空")
    @Schema(description = "通知记录 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @NotBlank(message = "重推原因不能为空")
    @Size(max = 512, message = "重推原因不能超过 512 个字符")
    @Schema(description = "重推原因", requiredMode = Schema.RequiredMode.REQUIRED)
    private String retryReason;
}
