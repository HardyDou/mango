package io.mango.identity.starter.controller;

import io.mango.identity.api.AuthIdentityApi;
import io.mango.identity.api.vo.AuthUserInfo;
import io.mango.identity.core.entity.IdentityUser;
import io.mango.identity.core.service.IIdentityUserService;
import io.mango.infra.web.api.Inner;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal identity endpoints used by authentication.
 */
@Inner
@RestController
@RequestMapping("/identity/auth")
@RequiredArgsConstructor
public class AuthIdentityController implements AuthIdentityApi {

    private final IIdentityUserService identityUserService;

    @Override
    @GetMapping("/username/{username}")
    public AuthUserInfo getByUsernameForAuth(@PathVariable String username) {
        return convertToAuthUserInfo(identityUserService.getByUsername(username));
    }

    @Override
    @GetMapping("/id/{userId}")
    public AuthUserInfo getByIdForAuth(@PathVariable Long userId) {
        return convertToAuthUserInfo(identityUserService.getById(userId));
    }

    private AuthUserInfo convertToAuthUserInfo(IdentityUser entity) {
        if (entity == null) {
            return null;
        }
        AuthUserInfo authUser = new AuthUserInfo();
        authUser.setUserId(entity.getUserId());
        authUser.setUsername(entity.getUsername());
        authUser.setPassword(entity.getPassword());
        authUser.setNickname(entity.getNickname());
        authUser.setStatus(entity.getStatus());
        return authUser;
    }
}
