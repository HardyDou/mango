package io.mango.identity.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.identity.api.AuthIdentityApi;
import io.mango.identity.api.AuthIdentitySecurityProvider;
import io.mango.identity.api.AuthUserProvider;
import io.mango.identity.api.command.ChangeRequiredPasswordCommand;
import io.mango.identity.api.query.AuthUsernameQuery;
import io.mango.identity.api.vo.AuthUserInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证链路使用的内部身份接口。
 */
@ApiAccess(mode = ApiResourceAccessMode.INTERNAL)
@RestController
@RequestMapping("/identity")
@RequiredArgsConstructor
@Tag(name = "身份认证-内部", description = "认证链路内部身份查询接口")
public class AuthIdentityController implements AuthIdentityApi {

    private final AuthUserProvider authUserProvider;
    private final AuthIdentitySecurityProvider authIdentitySecurityProvider;

    @Override
    @GetMapping("/auth/username")
    @Operation(summary = "按用户名查询认证身份", description = "内部接口。认证链路按登录域和用户名查询用户认证事实")
    public R<AuthUserInfo> getByUsernameForAuth(@ParameterObject AuthUsernameQuery query) {
        return R.ok(authUserProvider.getByUsernameForAuth(query.getUsername(), query.getRealm()));
    }

    @Override
    @GetMapping("/auth/id")
    @Operation(summary = "按用户ID查询认证身份", description = "内部接口。认证链路按用户ID查询用户认证事实")
    public R<AuthUserInfo> getByIdForAuth(
            @Parameter(description = "用户ID")
            @RequestParam("userId") Long userId) {
        return R.ok(authUserProvider.getByIdForAuth(userId));
    }

    @Override
    @PostMapping("/auth/login-failure")
    @Operation(summary = "记录登录失败", description = "内部接口。认证链路记录密码校验失败并按策略锁定账号")
    public R<Boolean> recordLoginFailure(@RequestParam("userId") Long userId) {
        authIdentitySecurityProvider.recordLoginFailure(userId);
        return R.ok(true);
    }

    @Override
    @PostMapping("/auth/login-success")
    @Operation(summary = "记录登录成功", description = "内部接口。认证链路记录登录成功并清理失败锁定状态")
    public R<Boolean> recordLoginSuccess(@RequestParam("userId") Long userId) {
        authIdentitySecurityProvider.recordLoginSuccess(userId);
        return R.ok(true);
    }

    @Override
    @PostMapping("/auth/password/change-required")
    @Operation(summary = "强制改密", description = "内部接口。认证链路使用强制改密凭据修改用户密码")
    public R<Boolean> changeRequiredPassword(@Valid @RequestBody ChangeRequiredPasswordCommand command) {
        authIdentitySecurityProvider.changeRequiredPassword(command);
        return R.ok(true);
    }
}
