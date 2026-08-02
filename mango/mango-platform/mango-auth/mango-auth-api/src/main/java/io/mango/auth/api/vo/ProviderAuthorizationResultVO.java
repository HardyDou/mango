package io.mango.auth.api.vo;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.auth.api.enums.ProviderAuthorizationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class ProviderAuthorizationResultVO {

    @Schema(description = "第三方授权处理状态")
    private ProviderAuthorizationStatus status;
    @Schema(description = "授权登录成功时返回的登录信息")
    @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Feign and Jackson require this mutable login result getter"))
    @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Feign and Jackson require this mutable login result setter"))
    private LoginVO login;
    @Schema(description = "需要绑定已有账号时返回的一次性凭据")
    private String bindingTicket;
    @Schema(description = "第三方账号显示名称")
    private String providerDisplayName;
    @Schema(description = "绑定凭据剩余有效秒数")
    private long expiresInSeconds;
}
