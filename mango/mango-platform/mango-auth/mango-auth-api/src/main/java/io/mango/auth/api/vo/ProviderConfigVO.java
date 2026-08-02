package io.mango.auth.api.vo;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.auth.api.enums.ExternalAuthProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProviderConfigVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "第三方登录配置 ID")
    private Long id;
    @Schema(description = "配置生效的应用编码")
    private String appCode;
    @Schema(description = "第三方登录提供方")
    private ExternalAuthProvider provider;
    @Schema(description = "第三方应用 ClientId")
    private String clientId;
    @Schema(description = "第三方组织标识")
    private String providerTenantId;
    @Schema(description = "第三方应用 AgentId")
    private String agentId;
    @Schema(description = "允许使用的授权回调地址")
    @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Feign and Jackson require this mutable collection getter"))
    @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Feign and Jackson require this mutable collection setter"))
    private List<String> redirectUris = new ArrayList<>();
    @Schema(description = "是否启用第三方登录配置")
    private Boolean enabled;
    @Schema(description = "是否已经配置 Secret")
    private Boolean secretConfigured;
    @Schema(description = "配置是否满足当前提供方的必填要求")
    private Boolean complete;
    @Schema(description = "配置最后更新时间")
    private LocalDateTime updatedAt;
}
