package io.mango.auth.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 令牌校验命令。
 */
@Data
@Schema(description = "令牌校验命令")
public class ValidateTokenCommand {

    @Schema(description = "访问令牌")
    @NotBlank(message = "令牌不能为空")
    @Size(max = 4096, message = "令牌最多4096个字符")
    private String token;
}
