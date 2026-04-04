package io.mango.auth.api.vo;

import io.mango.common.vo.BaseVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Login response VO
 *
 * @author Mango
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LoginResponse extends BaseVO {

    private static final long serialVersionUID = 1L;

    /**
     * Access token (JWT)
     */
    private String accessToken;

    /**
     * Token type (Bearer)
     */
    private String tokenType = "Bearer";

    /**
     * Expires in (seconds)
     */
    private Long expiresIn;

    /**
     * Refresh token
     */
    private String refreshToken;

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
     * User roles
     */
    private java.util.List<String> roles;

    /**
     * User permissions
     */
    private java.util.List<String> permissions;
}
