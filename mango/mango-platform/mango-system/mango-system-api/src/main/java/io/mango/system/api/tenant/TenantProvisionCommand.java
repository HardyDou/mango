package io.mango.system.api.tenant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "租户初始化命令")
public class TenantProvisionCommand {

    @NotNull(message = "租户 ID 不能为空")
    @Positive(message = "租户 ID 必须大于 0")
    @Schema(description = "租户 ID")
    private final Long tenantId;

    @NotBlank(message = "租户编码不能为空")
    @Size(max = 50, message = "租户编码长度不能超过 50")
    @Schema(description = "租户编码")
    private final String tenantCode;

    @NotBlank(message = "租户名称不能为空")
    @Size(max = 100, message = "租户名称长度不能超过 100")
    @Schema(description = "租户名称")
    private final String tenantName;
}
