package io.mango.identity.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
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
    private String realm;

    @Schema(description = "用户名")
    private String username;
}
