package io.mango.identity.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.vo.PageResult;
import io.mango.identity.api.command.CreateIdentityUserCommand;
import io.mango.identity.api.command.ResetIdentityUserPasswordCommand;
import io.mango.identity.api.command.UpdateIdentityUserCommand;
import io.mango.identity.api.command.UpdateIdentityUserStatusCommand;
import io.mango.identity.api.query.IdentityUserPageQuery;
import io.mango.identity.api.query.IdentityUserTargetQuery;
import io.mango.common.result.R;
import io.mango.identity.api.IdentityUserApi;
import io.mango.identity.api.vo.IdentityUserInfo;
import io.mango.identity.api.vo.IdentityUserVO;
import io.mango.identity.core.service.IIdentityUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 机构成员账号控制器。
 */
@RestController
@RequestMapping("/identity")
@RequiredArgsConstructor
@Tag(name = "成员管理", description = "机构成员账号、状态、角色维护接口")
public class IdentityUserController implements IdentityUserApi {

    private final IIdentityUserService identityUserService;

    @GetMapping("/users/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:user:list")
    @Operation(summary = "分页查询机构成员", description = "权限接口。分页查询当前机构可管理的成员账号")
    public R<PageResult<IdentityUserVO>> page(@ParameterObject IdentityUserPageQuery query) {
        return R.ok(identityUserService.page(query));
    }

    @GetMapping("/users/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:user:query")
    @Operation(summary = "获取机构成员详情", description = "权限接口。按用户ID查询当前机构可管理的成员账号详情")
    public R<IdentityUserVO> detail(
            @Parameter(description = "用户ID")
            @RequestParam Long userId) {
        IdentityUserVO user = identityUserService.detail(userId);
        return user == null ? R.fail(404, "成员不存在") : R.ok(user);
    }

    @PostMapping("/users")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:user:add")
    @Operation(summary = "新增机构成员", description = "权限接口。创建当前机构下的成员账号")
    public R<Long> create(@Valid @RequestBody CreateIdentityUserCommand command) {
        try {
            return R.ok(identityUserService.create(command));
        } catch (IllegalArgumentException e) {
            return R.fail(400, e.getMessage());
        }
    }

    @PutMapping("/users")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:user:edit")
    @Operation(summary = "修改机构成员", description = "权限接口。更新当前机构可管理的成员账号")
    public R<Boolean> update(@Valid @RequestBody UpdateIdentityUserCommand command) {
        Boolean success = identityUserService.update(command);
        return Boolean.TRUE.equals(success) ? R.ok(true) : R.fail(404, "成员不存在");
    }

    @DeleteMapping("/users")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:user:delete")
    @Operation(summary = "移除机构成员", description = "权限接口。按用户ID移除当前机构成员身份，不删除全局账号")
    public R<Boolean> delete(
            @Parameter(description = "用户ID")
            @RequestParam Long userId) {
        Boolean success = identityUserService.delete(userId);
        return Boolean.TRUE.equals(success) ? R.ok(true) : R.fail(403, "无权移除该成员");
    }

    @PutMapping("/users/status")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:user:status")
    @Operation(summary = "修改机构成员状态", description = "权限接口。启用或禁用当前机构成员身份")
    public R<Boolean> updateStatus(@Valid @RequestBody UpdateIdentityUserStatusCommand command) {
        Boolean success = identityUserService.updateStatus(command);
        return Boolean.TRUE.equals(success) ? R.ok(true) : R.fail(403, "无权修改该成员状态");
    }

    @PutMapping("/users/password/reset")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:user:reset-password")
    @Operation(summary = "重置成员账号密码", description = "权限接口。重置当前机构可管理的成员账号密码")
    public R<Boolean> resetPassword(@Valid @RequestBody ResetIdentityUserPasswordCommand command) {
        Boolean success = identityUserService.resetPassword(command);
        return Boolean.TRUE.equals(success) ? R.ok(true) : R.fail(404, "成员不存在");
    }

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

    @Override
    @GetMapping("/user/info/targets")
    @Operation(summary = "按接收目标解析用户资料", description = "内部接口。按用户、部门、岗位或角色解析当前租户内可接收通知的身份用户资料")
    public R<List<IdentityUserInfo>> listUserInfosByTarget(@ParameterObject @Valid IdentityUserTargetQuery query) {
        return R.ok(identityUserService.listUserInfosByTarget(query));
    }

}
