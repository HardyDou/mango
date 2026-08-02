package io.mango.auth.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProviderAuthorizationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "第三方平台授权地址")
    private String authorizationUrl;
    @Schema(description = "授权状态凭据剩余有效秒数")
    private long expiresInSeconds;
}
