package io.mango.identity.api.vo;

import lombok.Data;

/**
 * 身份用户资料。
 */
@Data
public class IdentityUserInfo {

    private static final long serialVersionUID = 1L;

    /** 用户 ID。 */
    private Long userId;

    /** 用户名。 */
    private String username;

    /** 昵称。 */
    private String nickname;

    /** 登录域。 */
    private String realm;

    /** 操作者类型。 */
    private String actorType;

    /** 归属主体类型。 */
    private String partyType;

    /** 归属主体 ID。 */
    private Long partyId;

    /** 邮箱。 */
    private String email;

    /** 手机号。 */
    private String phone;

    /** 头像地址。 */
    private String avatar;

    /** 状态：0-禁用，1-启用。 */
    private Integer status;

}
