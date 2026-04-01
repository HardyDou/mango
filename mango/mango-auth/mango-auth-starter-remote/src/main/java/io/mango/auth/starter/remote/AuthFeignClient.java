package io.mango.auth.starter.remote;

import io.mango.auth.api.vo.LoginRequest;
import io.mango.auth.api.vo.LoginResponse;
import io.mango.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Auth Feign client for remote authentication operations
 *
 * @author Mango
 */
@FeignClient(name = "auth-service", path = "/auth")
public interface AuthFeignClient {

    /**
     * User login
     *
     * @param loginRequest login credentials
     * @return login response with access token
     */
    @PostMapping("/login")
    R<LoginResponse> login(@RequestBody LoginRequest loginRequest);

    /**
     * Refresh token
     *
     * @param refreshToken the refresh token
     * @return new login response with new access token
     */
    @PostMapping("/refresh")
    R<LoginResponse> refresh(@RequestParam String refreshToken);

    /**
     * User logout
     *
     * @param token authorization token
     * @return void
     */
    @PostMapping("/logout")
    R<Void> logout(@RequestHeader("Authorization") String token);

    /**
     * Validate token
     *
     * @param token authorization token
     * @return true if token is valid
     */
    @GetMapping("/validate")
    R<Boolean> validate(@RequestHeader("Authorization") String token);
}
