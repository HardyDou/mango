package io.mango.authorization.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** Token 刷新结果。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Token 刷新结果")
public class TokenPairVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "访问令牌")
    private String accessToken;

    @Schema(description = "刷新令牌")
    private String refreshToken;

    public String accessToken() { return accessToken; }
    public String refreshToken() { return refreshToken; }
}
