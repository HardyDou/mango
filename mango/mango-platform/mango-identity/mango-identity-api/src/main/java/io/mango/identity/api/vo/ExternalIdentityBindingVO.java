package io.mango.identity.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "第三方登录身份绑定")
public class ExternalIdentityBindingVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "绑定ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "应用编码")
    private String appCode;

    @Schema(description = "身份提供方")
    private String provider;

    @Schema(description = "企业ID")
    private String corpId;

    @Schema(description = "外部用户ID")
    private String externalUserId;

    @Schema(description = "显示名称快照")
    private String displayName;

    @Schema(description = "第三方头像文件ID")
    private Long avatarFileId;

    @Schema(description = "绑定来源")
    private String bindSource;

    @Schema(description = "绑定状态")
    private String bindStatus;

    @Schema(description = "绑定时间")
    private LocalDateTime bindTime;

    @Schema(description = "最近登录时间")
    private LocalDateTime lastLoginTime;
}
