package io.mango.auth.api.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 刷新令牌命令。
 */
@Data
public class RefreshTokenCommand {

    @NotBlank(message = "刷新令牌不能为空")
    @Size(max = 4096, message = "刷新令牌最多4096个字符")
    private String refreshToken;
}
