package io.mango.identity.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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

    @Size(max = 64, message = "应用编码最多64个字符")
    @Schema(description = "应用编码，默认 internal-admin")
    private String appCode;

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
    @Schema(description = "第三方平台返回的完整显示名称")
    private String displayName;

    @Positive(message = "头像文件ID必须大于0")
    @Schema(description = "已导入 Mango 文件中心的第三方头像文件ID")
    private Long avatarFileId;

    @Schema(description = "是否用本次头像快照替换已有头像；为 true 时允许将头像清空")
    private Boolean replaceAvatarFile;

    @Size(max = 32, message = "绑定来源最多32个字符")
    @Schema(description = "绑定来源：SYNC/ADMIN/SELF")
    private String bindSource;
}
