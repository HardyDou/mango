package io.mango.identity.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Query for checking whether a login account can be created or restored. */
@Data
@Schema(description = "登录账号可用性查询")
public class IdentityAccountAvailabilityQuery {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 100, message = "用户名最多100个字符")
    @Schema(description = "登录用户名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @Size(max = 32, message = "登录域最多32个字符")
    @Schema(description = "登录域")
    private String realm;
}
