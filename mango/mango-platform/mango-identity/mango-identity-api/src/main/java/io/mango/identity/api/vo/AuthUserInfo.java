package io.mango.identity.api.vo;

import lombok.Data;

/**
 * 内部认证用户事实。
 */
@Data
public class AuthUserInfo {

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

    /**
     * 密码哈希，只允许内部认证链路使用。
     */
    private String password;

    /** 状态：0-禁用，1-启用。 */
    private int status;
}
