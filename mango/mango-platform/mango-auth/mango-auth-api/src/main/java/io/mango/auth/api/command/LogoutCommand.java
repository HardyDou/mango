package io.mango.auth.api.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 退出登录命令。
 */
@Data
public class LogoutCommand {

    @NotBlank(message = "令牌不能为空")
    @Size(max = 4096, message = "令牌最多4096个字符")
    private String token;
}
