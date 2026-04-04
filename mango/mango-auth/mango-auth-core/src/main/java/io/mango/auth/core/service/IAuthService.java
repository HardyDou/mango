package io.mango.auth.core.service;

import io.mango.auth.api.vo.LoginRequest;
import io.mango.auth.api.vo.LoginResponse;

/**
 * Authentication service interface
 *
 * @author Mango
 */
public interface IAuthService {

    /**
     * User login
     *
     * @param loginRequest login request containing username and password
     * @return login response with JWT token
     */
    LoginResponse login(LoginRequest loginRequest);

    /**
     * Refresh token
     *
     * @param refreshToken refresh token
     * @return new login response with new tokens
     */
    LoginResponse refreshToken(String refreshToken);

    /**
     * Logout
     *
     * @param token JWT token
     */
    void logout(String token);

    /**
     * Validate token
     *
     * @param token JWT token
     * @return true if token is valid
     */
    boolean validateToken(String token);

    /**
     * Get user ID from token
     *
     * @param token JWT token
     * @return user ID
     */
    Long getUserIdFromToken(String token);
}
