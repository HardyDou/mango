package io.mango.identity.starter.remote;

import io.mango.common.result.R;
import io.mango.identity.api.AuthIdentityApi;
import io.mango.identity.api.command.ChangeRequiredPasswordCommand;
import io.mango.identity.api.query.AuthUsernameQuery;
import io.mango.identity.api.vo.AuthUserInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 认证身份远程客户端。
 */
@FeignClient(name = "mango-identity", contextId = "authIdentityFeignClient", path = "/identity")
public interface AuthIdentityFeignClient extends AuthIdentityApi {

    @Override
    @GetMapping("/auth/username")
    R<AuthUserInfo> getByUsernameForAuth(@SpringQueryMap AuthUsernameQuery query);

    @Override
    @GetMapping("/auth/id")
    R<AuthUserInfo> getByIdForAuth(@RequestParam("userId") Long userId);

    @Override
    @PostMapping("/auth/login-failure")
    R<Boolean> recordLoginFailure(@RequestParam("userId") Long userId);

    @Override
    @PostMapping("/auth/login-success")
    R<Boolean> recordLoginSuccess(@RequestParam("userId") Long userId);

    @Override
    @PostMapping("/auth/password/change-required")
    R<Boolean> changeRequiredPassword(@RequestBody ChangeRequiredPasswordCommand command);
}
