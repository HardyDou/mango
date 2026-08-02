package io.mango.auth.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class BindExistingAccountCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank
    @Size(max = 256)
    @Schema(description = "第三方授权绑定凭据")
    private String bindingTicket;

    @NotBlank
    @Size(max = 100)
    @Schema(description = "已有 Mango 账号用户名")
    private String username;

    @NotBlank
    @Size(max = 200)
    @Schema(description = "已有 Mango 账号当前密码")
    private String password;
}
