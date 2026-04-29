package io.mango.identity.starter.remote;

import io.mango.common.result.R;
import io.mango.identity.api.AuthIdentityApi;
import io.mango.identity.api.vo.AuthUserInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 认证用户事实 Feign 客户端。
 */
@FeignClient(name = "mango-identity", path = "/identity")
public interface AuthIdentityFeignClient extends AuthIdentityApi {

    @Override
    @GetMapping("/auth/username/{username}")
    R<AuthUserInfo> getByUsernameForAuth(@PathVariable("username") String username);

    @Override
    @GetMapping("/auth/realm/{realm}/username/{username}")
    R<AuthUserInfo> getByUsernameForAuth(
            @PathVariable("realm") String realm,
            @PathVariable("username") String username);

    @Override
    @GetMapping("/auth/id/{userId}")
    R<AuthUserInfo> getByIdForAuth(@PathVariable("userId") Long userId);
}
