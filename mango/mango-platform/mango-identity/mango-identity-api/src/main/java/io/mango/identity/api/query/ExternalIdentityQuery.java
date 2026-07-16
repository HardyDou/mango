package io.mango.identity.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Schema(description = "第三方登录身份查询")
public class ExternalIdentityQuery {

    @Size(max = 32, message = "身份提供方最多32个字符")
    @Schema(description = "身份提供方")
    private String provider;

    @Size(max = 128, message = "企业ID最多128个字符")
    @Schema(description = "企业ID")
    private String corpId;

    @Size(max = 128, message = "外部用户ID最多128个字符")
    @Schema(description = "外部用户ID")
    private String externalUserId;

    @Positive(message = "用户ID必须大于0")
    @Schema(description = "用户ID")
    private Long userId;
}
