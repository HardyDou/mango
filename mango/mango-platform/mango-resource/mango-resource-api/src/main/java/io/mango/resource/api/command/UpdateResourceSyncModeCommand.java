package io.mango.resource.api.command;

import io.mango.resource.api.enums.ResourceSyncMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 后台更新资源同步模式命令。
 */
@Data
@Schema(description = "后台更新资源同步模式命令")
public class UpdateResourceSyncModeCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "资源ID不能为空")
    @Schema(description = "稳定资源ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String resourceId;

    @NotNull(message = "同步模式不能为空")
    @Schema(description = "同步模式：AUTO、INIT_ONLY、MANUAL、LOCKED", requiredMode = Schema.RequiredMode.REQUIRED)
    private ResourceSyncMode syncMode;
}
