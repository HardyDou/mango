package io.mango.infra.security.api;

import java.util.Map;

/**
 * Token service interface.
 * <p>
 * Provides JWT token generation, validation and refresh.
 * Implementation: {@code JjwtTokenServiceImpl} (JJWT library).
 *
 * @author Mango
 */
public interface ITokenService {

    /** Bearer token prefix in Authorization header. */
    String BEARER_PREFIX = "Bearer ";

    /** Token type: access token. */
    String TOKEN_TYPE_ACCESS = "access";

    /** Token type: refresh token. */
    String TOKEN_TYPE_REFRESH = "refresh";

    /**
     * Generate access token (short-lived, default 2 hours).
     *
     * @param userId      user ID
     * @param username    username
     * @param extraClaims additional claims to embed in the token
     * @return access token string
     */
    String generateAccessToken(Long userId, String username, Map<String, Object> extraClaims);

    /**
     * Generate refresh token (long-lived, default 7 days).
     *
     * @param userId   user ID
     * @param username username
     * @return refresh token string
     */
    String generateRefreshToken(Long userId, String username);

    /**
     * Validate token is not expired and has valid signature.
     *
     * @param token JWT token string
     * @return true if valid, false otherwise
     */
    boolean validateToken(String token);

    /**
     * Extract user ID from token.
     *
     * @param token JWT token string
     * @return user ID or null if invalid
     */
    Long getUserId(String token);

    /**
     * Extract username from token.
     *
     * @param token JWT token string
     * @return username or null if invalid
     */
    String getUsername(String token);

    /**
     * Extract token type (access or refresh).
     *
     * @param token JWT token string
     * @return "access", "refresh", or null if invalid
     */
    String getTokenType(String token);

    /**
     * Refresh token pair using a valid refresh token.
     *
     * @param refreshToken valid refresh token
     * @return new token pair (access + refresh), or null if refresh token invalid
     */
    TokenPair refresh(String refreshToken);

    /**
     * Token pair record for refresh response.
     */
    record TokenPair(String accessToken, String refreshToken) {}
}
