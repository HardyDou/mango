package io.mango.identity.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 修改机构成员状态命令。
 */
@Data
@Schema(description = "修改机构成员状态命令")
public class UpdateIdentityUserStatusCommand {

    @Schema(description = "用户ID，后端会解析为当前机构成员", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Schema(description = "成员状态：0-禁用，1-启用", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "状态不能为空")
    private Integer status;
}
