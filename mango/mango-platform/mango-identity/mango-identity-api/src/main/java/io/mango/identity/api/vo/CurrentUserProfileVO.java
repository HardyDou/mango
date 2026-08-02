package io.mango.identity.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
public class CurrentUserProfileVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "当前用户 ID")
    private Long userId;
    @Schema(description = "当前用户登录名")
    private String username;
    @Schema(description = "当前用户昵称")
    private String nickname;
    @Schema(description = "头像文件引用")
    private String avatar;
    @Schema(description = "已绑定手机号")
    private String phone;
    @Schema(description = "已绑定邮箱")
    private String email;
    @Schema(description = "实名姓名")
    private String realName;
    @Schema(description = "实名证件类型")
    private String documentType;
    @Schema(description = "实名证件号码")
    private String documentNumber;
    @Schema(description = "实名认证状态")
    private String verificationStatus;
    @Schema(description = "实名认证来源")
    private String verificationSource;
}
