package io.mango.identity.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "绑定第三方登录身份命令")
public class BindExternalIdentityCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "用户ID不能为空")
    @Schema(description = "Mango 用户ID")
    private Long userId;

    @NotBlank(message = "身份提供方不能为空")
    @Size(max = 32, message = "身份提供方最多32个字符")
    @Schema(description = "身份提供方，例如 WECOM")
    private String provider;

    @NotBlank(message = "企业ID不能为空")
    @Size(max = 128, message = "企业ID最多128个字符")
    @Schema(description = "企业微信 CorpId")
    private String corpId;

    @NotBlank(message = "外部用户ID不能为空")
    @Size(max = 128, message = "外部用户ID最多128个字符")
    @Schema(description = "企业微信 userid")
    private String externalUserId;

    @Size(max = 128, message = "显示名称最多128个字符")
    @Schema(description = "显示名称快照")
    private String displayName;

    @Size(max = 32, message = "绑定来源最多32个字符")
    @Schema(description = "绑定来源：SYNC/ADMIN/SELF")
    private String bindSource;
}
