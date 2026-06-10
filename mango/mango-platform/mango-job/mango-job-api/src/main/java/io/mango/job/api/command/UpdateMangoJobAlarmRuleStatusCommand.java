package io.mango.job.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 更新 Mango Job 告警规则启停命令。
 */
@Data
@Schema(description = "更新 Mango Job 告警规则启停命令")
public class UpdateMangoJobAlarmRuleStatusCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "告警规则 ID 不能为空")
    @Schema(description = "告警规则 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @NotNull(message = "启用状态不能为空")
    @Schema(description = "是否启用", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean enabled;
}
