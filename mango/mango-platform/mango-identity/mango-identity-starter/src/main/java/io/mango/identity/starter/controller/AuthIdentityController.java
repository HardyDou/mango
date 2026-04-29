package io.mango.identity.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.identity.api.AuthIdentityApi;
import io.mango.identity.api.AuthUserProvider;
import io.mango.identity.api.vo.AuthUserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证链路使用的内部身份接口。
 */
@ApiAccess(mode = ApiResourceAccessMode.INTERNAL)
@RestController
@RequestMapping("/identity")
@RequiredArgsConstructor
public class AuthIdentityController implements AuthIdentityApi {

    private final AuthUserProvider authUserProvider;

    @Override
    @GetMapping("/auth/username/{username}")
    public R<AuthUserInfo> getByUsernameForAuth(@PathVariable("username") String username) {
        return R.ok(authUserProvider.getByUsernameForAuth(username));
    }

    @Override
    @GetMapping("/auth/realm/{realm}/username/{username}")
    public R<AuthUserInfo> getByUsernameForAuth(
            @PathVariable("realm") String realm,
            @PathVariable("username") String username) {
        return R.ok(authUserProvider.getByUsernameForAuth(username, realm));
    }

    @Override
    @GetMapping("/auth/id/{userId}")
    public R<AuthUserInfo> getByIdForAuth(@PathVariable("userId") Long userId) {
        return R.ok(authUserProvider.getByIdForAuth(userId));
    }
}
