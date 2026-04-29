package io.mango.auth.api.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 登录命令。
 */
@Data
public class LoginCommand {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 100, message = "用户名最多100个字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(max = 200, message = "密码最多200个字符")
    private String password;

    @Size(max = 32, message = "登录域最多32个字符")
    private String realm;

    @Size(max = 32, message = "操作者类型最多32个字符")
    private String actorType;

    @Size(max = 64, message = "归属主体类型最多64个字符")
    private String partyType;

    private Long partyId;

    @Size(max = 64, message = "应用编码最多64个字符")
    private String appCode;

    @Size(max = 64, message = "验证码最多64个字符")
    private String captchaCode;

    @Size(max = 128, message = "验证码键最多128个字符")
    private String captchaKey;
}
