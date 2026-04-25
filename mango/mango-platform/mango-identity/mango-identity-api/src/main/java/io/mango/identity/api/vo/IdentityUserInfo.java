package io.mango.identity.api.vo;

import lombok.Data;

/**
 * Identity user profile.
 *
 * @author Mango
 */
@Data
public class IdentityUserInfo {

    private static final long serialVersionUID = 1L;

    /**
     * User ID
     */
    private Long userId;

    /**
     * Username
     */
    private String username;

    /**
     * Nickname
     */
    private String nickname;

    /**
     * Email
     */
    private String email;

    /**
     * Phone number
     */
    private String phone;

    /**
     * Avatar URL
     */
    private String avatar;

    /**
     * Status (0: disabled, 1: enabled)
     */
    private Integer status;

}
