package io.mango.auth.starter.remote;

import io.mango.auth.api.AuthApi;
import io.mango.auth.api.vo.LoginRequest;
import io.mango.auth.api.vo.LoginResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Auth Feign client - implements AuthApi for remote calls
 *
 * @author Mango
 */
@FeignClient(name = "auth-service", path = "/auth")
public interface AuthFeignClient extends AuthApi {

    @Override
    @PostMapping("/login")
    LoginResponse login(@RequestBody LoginRequest loginRequest);

    @Override
    @PostMapping("/refresh")
    LoginResponse refreshToken(@RequestBody String refreshToken);

    @Override
    @PostMapping("/logout")
    void logout(@RequestHeader("Authorization") String token);

    @Override
    @GetMapping("/validate")
    boolean validateToken(@RequestHeader("Authorization") String token);
}
