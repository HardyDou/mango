package io.mango.auth.api.command;

import io.mango.auth.api.enums.ExternalAuthProvider;
import io.mango.auth.api.enums.ProviderAuthorizationIntent;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class StartProviderAuthorizationCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank
    @Size(max = 64)
    @Schema(description = "发起授权的租户标识")
    private String tenantId;

    @NotBlank
    @Size(max = 64)
    @Schema(description = "发起授权的应用编码")
    private String appCode;

    @NotNull
    @Schema(description = "第三方登录提供方")
    private ExternalAuthProvider provider;

    @NotNull
    @Schema(description = "授权用途，登录或绑定当前账号")
    private ProviderAuthorizationIntent intent;

    @NotBlank
    @Size(max = 500)
    @Schema(description = "授权完成后的回调地址")
    private String redirectUri;
}
