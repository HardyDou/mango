package io.mango.identity.starter.controller;

import io.mango.common.result.R;
import io.mango.identity.api.IdentityUserApi;
import io.mango.identity.api.vo.IdentityUserInfo;
import io.mango.identity.core.service.IIdentityUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 身份用户资料控制器。
 */
@RestController
@RequestMapping("/identity")
@RequiredArgsConstructor
public class IdentityUserController implements IdentityUserApi {

    private final IIdentityUserService identityUserService;

    @Override
    @GetMapping("/user/info/username")
    public R<IdentityUserInfo> getUserInfo(@RequestParam("username") String username) {
        return R.ok(identityUserService.getUserInfo(username));
    }

    @Override
    @GetMapping("/user/info/id")
    public R<IdentityUserInfo> getUserInfoById(@RequestParam("userId") Long userId) {
        return R.ok(identityUserService.getUserInfoById(userId));
    }

}
