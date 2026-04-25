package io.mango.identity.starter.controller;

import io.mango.identity.api.IdentityUserApi;
import io.mango.identity.api.vo.IdentityUserInfo;
import io.mango.identity.core.service.IIdentityUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Identity user controller - implements IdentityUserApi
 *
 * @author Mango
 */
@RestController
@RequestMapping("/identity/user")
@RequiredArgsConstructor
public class IdentityUserController implements IdentityUserApi {

    private final IIdentityUserService identityUserService;

    @Override
    @GetMapping("/info/username/{username}")
    public IdentityUserInfo getUserInfo(@PathVariable String username) {
        return identityUserService.getUserInfo(username);
    }

    @Override
    @GetMapping("/info/id/{userId}")
    public IdentityUserInfo getUserInfoById(@PathVariable Long userId) {
        return identityUserService.getUserInfoById(userId);
    }

}
