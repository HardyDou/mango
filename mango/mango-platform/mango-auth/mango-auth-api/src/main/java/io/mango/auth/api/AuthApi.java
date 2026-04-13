package io.mango.auth.api;

import io.mango.auth.api.vo.LoginRequest;
import io.mango.auth.api.vo.LoginResponse;

/**
 * Authentication API interface
 * Exposed via Controller (local) or Feign Client (remote)
 *
 * @author Mango
 */
public interface AuthApi {

    /**
     * User login
     *
     * @param loginRequest login credentials
     * @return login response with access token
     */
    LoginResponse login(LoginRequest loginRequest);

    /**
     * Refresh token
     *
     * @param refreshToken the refresh token (passed as request body)
     * @return new login response with new access token
     */
    LoginResponse refreshToken(String refreshToken);

    /**
     * User logout
     *
     * @param token authorization token (passed as Authorization header)
     */
    void logout(String token);

    /**
     * Validate token
     *
     * @param token authorization token (passed as Authorization header)
     * @return true if token is valid
     */
    boolean validateToken(String token);
}
