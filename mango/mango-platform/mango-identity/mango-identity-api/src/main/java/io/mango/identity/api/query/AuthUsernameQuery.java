package io.mango.identity.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 认证用户名查询条件。
 */
@Data
@Schema(description = "认证用户名查询条件")
public class AuthUsernameQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "登录域")
    @Size(max = 32, message = "登录域最多32个字符")
    private String realm;

    @Schema(description = "用户名")
    @NotBlank(message = "用户名不能为空")
    @Size(max = 100, message = "用户名最多100个字符")
    private String username;
}
