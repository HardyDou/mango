package io.mango.identity.starter.remote;

import io.mango.identity.api.AuthIdentityApi;
import io.mango.identity.api.vo.AuthUserInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Internal auth identity Feign client.
 */
@FeignClient(name = "mango-identity", path = "/identity/auth")
public interface AuthIdentityFeignClient extends AuthIdentityApi {

    @Override
    @GetMapping("/username/{username}")
    AuthUserInfo getByUsernameForAuth(@PathVariable String username);

    @Override
    @GetMapping("/id/{userId}")
    AuthUserInfo getByIdForAuth(@PathVariable Long userId);
}
