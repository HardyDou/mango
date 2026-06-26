package io.mango.identity.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 要求成员下次登录改密命令。
 */
@Data
@Schema(description = "要求成员下次登录改密命令")
public class RequireIdentityUserPasswordResetCommand {

    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "用户ID不能为空")
    private Long userId;
}
