package io.mango.auth.api.vo;

import lombok.Data;

import java.util.List;

/**
 * Login response VO
 *
 * @author Mango
 */
@Data
public class LoginResponse {

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
    private List<String> roles;

    /**
     * User permissions
     */
    private List<String> permissions;
}
