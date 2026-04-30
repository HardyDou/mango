package io.mango.identity.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 身份用户资料。
 */
@Data
@Schema(description = "身份用户资料")
public class IdentityUserInfo {

    private static final long serialVersionUID = 1L;

    /** 用户 ID。 */
    @Schema(description = "用户 ID")
    private Long userId;

    /** 用户名。 */
    @Schema(description = "用户名")
    private String username;

    /** 昵称。 */
    @Schema(description = "昵称")
    private String nickname;

    /** 登录域。 */
    @Schema(description = "登录域，例如 INTERNAL、CUSTOMER、FINANCIAL")
    private String realm;

    /** 操作者类型。 */
    @Schema(description = "操作者类型，例如 INTERNAL_USER、CUSTOMER_USER、FINANCIAL_USER")
    private String actorType;

    /** 归属主体类型。 */
    @Schema(description = "归属主体类型，例如 COMPANY、FINANCIAL_INSTITUTION、PERSON")
    private String partyType;

    /** 归属主体 ID。 */
    @Schema(description = "归属主体 ID")
    private Long partyId;

    /** 邮箱。 */
    @Schema(description = "邮箱")
    private String email;

    /** 手机号。 */
    @Schema(description = "手机号")
    private String phone;

    /** 头像地址。 */
    @Schema(description = "头像地址")
    private String avatar;

    /** 状态：0-禁用，1-启用。 */
    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;

}
