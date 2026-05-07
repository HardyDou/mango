package io.mango.auth.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 登录结果 VO。
 */
@Data
@Schema(description = "登录结果")
public class LoginVO {

    @Schema(description = "访问令牌")
    private String accessToken;

    @Schema(description = "令牌类型，固定为 Bearer")
    private String tokenType = "Bearer";

    @Schema(description = "访问令牌有效期，单位秒")
    private Long expiresIn;

    @Schema(description = "刷新令牌")
    private String refreshToken;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "登录域")
    private String realm;

    @Schema(description = "操作者类型")
    private String actorType;

    @Schema(description = "归属主体类型")
    private String partyType;

    @Schema(description = "归属主体ID")
    private Long partyId;

    @Schema(description = "应用编码")
    private String appCode;

    @Schema(description = "角色编码列表")
    private List<String> roles;

    @Schema(description = "权限编码列表")
    private List<String> permissions;
}
