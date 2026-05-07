package io.mango.identity.starter.controller;

import io.mango.common.result.R;
import io.mango.identity.api.IdentityUserApi;
import io.mango.identity.api.vo.IdentityUserInfo;
import io.mango.identity.core.service.IIdentityUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "身份用户", description = "身份用户资料查询接口")
public class IdentityUserController implements IdentityUserApi {

    private final IIdentityUserService identityUserService;

    @Override
    @GetMapping("/user/info/username")
    @Operation(summary = "按用户名查询用户资料", description = "内部接口。按用户名查询身份用户资料，供认证和用户上下文链路使用")
    public R<IdentityUserInfo> getUserInfo(
            @Parameter(description = "用户名")
            @RequestParam("username") String username) {
        return R.ok(identityUserService.getUserInfo(username));
    }

    @Override
    @GetMapping("/user/info/id")
    @Operation(summary = "按用户ID查询用户资料", description = "内部接口。按用户ID查询身份用户资料，供认证和用户上下文链路使用")
    public R<IdentityUserInfo> getUserInfoById(
            @Parameter(description = "用户ID")
            @RequestParam("userId") Long userId) {
        return R.ok(identityUserService.getUserInfoById(userId));
    }

}
