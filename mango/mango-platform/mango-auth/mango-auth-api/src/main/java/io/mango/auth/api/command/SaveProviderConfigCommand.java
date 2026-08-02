package io.mango.auth.api.command;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.auth.api.enums.ExternalAuthProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class SaveProviderConfigCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Positive
    @Schema(description = "第三方登录配置 ID，新增时为空")
    private Long id;

    @NotBlank
    @Size(max = 64)
    @Schema(description = "配置生效的应用编码")
    private String appCode;

    @NotNull
    @Schema(description = "第三方登录提供方")
    private ExternalAuthProvider provider;

    @Size(max = 128)
    @Schema(description = "第三方应用 ClientId，钉钉配置必填")
    private String clientId;

    @Size(max = 128)
    @Schema(description = "第三方组织标识，企业微信为企业 ID")
    private String providerTenantId;

    @Size(max = 64)
    @Schema(description = "第三方应用 AgentId，企业微信配置必填")
    private String agentId;

    @Size(max = 512)
    @Schema(description = "第三方应用 Secret，更新时留空表示保持原值")
    private String secret;

    @NotNull
    @Size(min = 1, max = 10)
    @Schema(description = "允许使用的授权回调地址")
    @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Jackson and validation require the mutable command collection getter"))
    @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Jackson requires the mutable command collection setter"))
    private List<@NotBlank @Size(max = 500) String> redirectUris = new ArrayList<>();

    @NotNull
    @Schema(description = "是否启用第三方登录配置")
    private Boolean enabled;
}
