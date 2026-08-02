package io.mango.identity.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.vo.PageResult;
import io.mango.identity.api.command.BatchDeleteIdentityUserCommand;
import io.mango.identity.api.command.BindExternalIdentityCommand;
import io.mango.identity.api.command.CreateIdentityUserCommand;
import io.mango.identity.api.command.ResetIdentityUserPasswordCommand;
import io.mango.identity.api.command.RequireIdentityUserPasswordResetCommand;
import io.mango.identity.api.command.UnbindExternalIdentityCommand;
import io.mango.identity.api.command.UpdateIdentityUserCommand;
import io.mango.identity.api.command.UpdateIdentityUserStatusCommand;
import io.mango.identity.api.command.UnlockIdentityUserCommand;
import io.mango.identity.api.command.SendContactCaptchaCommand;
import io.mango.identity.api.command.UpdateCurrentUserContactCommand;
import io.mango.identity.api.command.UpdateCurrentUserProfileCommand;
import io.mango.identity.api.command.UnbindCurrentExternalIdentityCommand;
import io.mango.identity.api.query.ExternalIdentityQuery;
import io.mango.identity.api.query.IdentityUserPageQuery;
import io.mango.identity.api.query.IdentityUserTargetQuery;
import io.mango.common.result.R;
import io.mango.identity.api.IdentityUserApi;
import io.mango.identity.api.vo.ExternalIdentityBindingVO;
import io.mango.identity.api.vo.IdentityUserInfoVO;
import io.mango.identity.api.vo.IdentityUserVO;
import io.mango.identity.api.vo.ContactCaptchaTicketVO;
import io.mango.identity.api.vo.CurrentUserProfileVO;
import io.mango.identity.core.service.IIdentityUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
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
@Validated
@Tag(name = "成员管理", description = "机构成员账号、状态、角色维护接口")
public class IdentityUserController implements IdentityUserApi {

    private final IIdentityUserService identityUserService;

    @Override
    @GetMapping("/me/profile")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "读取当前用户资料")
    @Operation(summary = "读取当前用户资料", description = "读取当前登录用户的基础资料、联系方式和实名认证信息")
    public R<CurrentUserProfileVO> currentProfile() {
        return R.ok(identityUserService.currentProfile());
    }

    @Override
    @PutMapping("/me/profile")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "更新当前用户资料")
    @Operation(summary = "更新当前用户资料", description = "更新当前登录用户的昵称、头像和实名认证信息")
    public R<CurrentUserProfileVO> updateCurrentProfile(@RequestBody UpdateCurrentUserProfileCommand command) {
        return R.ok(identityUserService.updateCurrentProfile(command));
    }

    @Override
    @PostMapping("/me/contact-captcha")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "发送新联系方式验证码")
    @Operation(summary = "发送新联系方式验证码", description = "向当前用户准备绑定的新手机号或邮箱发送验证码")
    public R<ContactCaptchaTicketVO> sendCurrentContactCaptcha(@RequestBody SendContactCaptchaCommand command) {
        return R.ok(identityUserService.sendCurrentContactCaptcha(command));
    }

    @Override
    @PutMapping("/me/contact")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "更新当前用户联系方式")
    @Operation(summary = "更新当前用户联系方式", description = "校验当前密码和验证码后更新手机号或邮箱")
    public R<CurrentUserProfileVO> updateCurrentContact(@RequestBody UpdateCurrentUserContactCommand command) {
        return R.ok(identityUserService.updateCurrentContact(command));
    }

    @GetMapping("/users/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:user:list")
    @Operation(summary = "分页查询机构成员", description = "权限接口。分页查询当前机构可管理的成员账号")
    @Override
    public R<PageResult<IdentityUserVO>> page(@ParameterObject IdentityUserPageQuery query) {
        return R.ok(identityUserService.pageResult(query));
    }

    @GetMapping("/users/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:user:query")
    @Operation(summary = "获取机构成员详情", description = "权限接口。按用户ID查询当前机构可管理的成员账号详情")
    @Override
    public R<IdentityUserVO> detail(
            @Parameter(description = "用户ID")
            @RequestParam("userId") Long userId) {
        return R.ok(identityUserService.detail(userId));
    }

    @PostMapping("/users")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:user:add")
    @Operation(summary = "新增机构成员", description = "权限接口。创建当前机构下的成员账号")
    @Override
    public R<Long> create(@RequestBody CreateIdentityUserCommand command) {
        return R.ok(identityUserService.create(command));
    }

    @PutMapping("/users")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:user:edit")
    @Operation(summary = "修改机构成员", description = "权限接口。更新当前机构可管理的成员账号")
    @Override
    public R<Boolean> update(@RequestBody UpdateIdentityUserCommand command) {
        return R.ok(identityUserService.update(command));
    }

    @DeleteMapping("/users")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:user:delete")
    @Operation(summary = "移除机构成员", description = "权限接口。按用户ID移除当前机构成员身份，不删除全局账号")
    @Override
    public R<Boolean> delete(
            @Parameter(description = "用户ID")
            @RequestParam("userId") Long userId) {
        return R.ok(identityUserService.deleteUser(userId));
    }

    @PostMapping("/users/delete-batch")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:user:delete")
    @Operation(summary = "批量移除机构成员", description = "权限接口。按用户ID批量移除当前机构成员身份，不删除全局账号")
    @Override
    public R<Integer> deleteBatch(@RequestBody BatchDeleteIdentityUserCommand command) {
        return R.ok(identityUserService.deleteBatch(command));
    }

    @PutMapping("/users/status")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:user:status")
    @Operation(summary = "修改机构成员状态", description = "权限接口。启用或禁用当前机构成员身份")
    @Override
    public R<Boolean> updateStatus(@RequestBody UpdateIdentityUserStatusCommand command) {
        return R.ok(identityUserService.updateStatus(command));
    }

    @PutMapping("/users/password/reset")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:user:reset-password")
    @Operation(summary = "重置成员账号密码", description = "权限接口。重置当前机构可管理的成员账号密码")
    @Override
    public R<Boolean> resetPassword(@RequestBody ResetIdentityUserPasswordCommand command) {
        return R.ok(identityUserService.resetPassword(command));
    }

    @PutMapping("/users/unlock")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:user:unlock")
    @Operation(summary = "解锁成员账号", description = "权限接口。清除当前机构可管理成员账号的登录失败锁定状态，不改变启用禁用状态")
    @Override
    public R<Boolean> unlock(@RequestBody UnlockIdentityUserCommand command) {
        return R.ok(identityUserService.unlock(command));
    }

    @PutMapping("/users/password/reset-required")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:user:password-policy")
    @Operation(summary = "要求成员下次登录改密", description = "权限接口。标记当前机构可管理成员账号下次登录必须修改密码")
    @Override
    public R<Boolean> requirePasswordReset(@RequestBody RequireIdentityUserPasswordResetCommand command) {
        return R.ok(identityUserService.requirePasswordReset(command));
    }

    @Override
    @GetMapping("/user/info/username")
    @Operation(summary = "按用户名查询用户资料", description = "内部接口。按用户名查询身份用户资料，供认证和用户上下文链路使用")
    public R<IdentityUserInfoVO> getUserInfo(
            @Parameter(description = "用户名")
            @RequestParam("username") String username) {
        return R.ok(identityUserService.getUserInfo(username));
    }

    @Override
    @GetMapping("/user/info/id")
    @Operation(summary = "按用户ID查询用户资料", description = "内部接口。按用户ID查询身份用户资料，供认证和用户上下文链路使用")
    public R<IdentityUserInfoVO> getUserInfoById(
            @Parameter(description = "用户ID")
            @RequestParam("userId") Long userId) {
        return R.ok(identityUserService.getUserInfoById(userId));
    }

    @Override
    @GetMapping("/user/info/targets")
    @Operation(summary = "按接收目标解析用户资料", description = "内部接口。按用户、部门、岗位或角色解析当前租户内可接收通知的身份用户资料")
    public R<List<IdentityUserInfoVO>> listUserInfosByTarget(@ParameterObject IdentityUserTargetQuery query) {
        return R.ok(identityUserService.listUserInfosByTarget(query));
    }

    @Override
    @PostMapping("/users/external-identities")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:user:edit")
    @Operation(summary = "绑定第三方登录身份", description = "权限接口。为成员绑定企业微信等第三方登录身份")
    public R<ExternalIdentityBindingVO> bindExternalIdentity(@RequestBody BindExternalIdentityCommand command) {
        return R.ok(identityUserService.bindExternalIdentity(command));
    }

    @Override
    @DeleteMapping("/users/external-identities")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:user:edit")
    @Operation(summary = "解绑第三方登录身份", description = "权限接口。解绑成员的企业微信等第三方登录身份")
    public R<Boolean> unbindExternalIdentity(@RequestBody UnbindExternalIdentityCommand command) {
        return R.ok(identityUserService.unbindExternalIdentity(command));
    }

    @Override
    @GetMapping("/users/external-identity")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:user:query")
    @Operation(summary = "查询第三方登录身份", description = "权限接口。按 provider/corpId/userid 查询第三方登录身份绑定")
    public R<ExternalIdentityBindingVO> findExternalIdentity(@ParameterObject ExternalIdentityQuery query) {
        return R.ok(identityUserService.findExternalIdentity(query));
    }

    @Override
    @GetMapping("/users/external-identities")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:user:query")
    @Operation(summary = "查询成员第三方登录身份", description = "权限接口。查询成员已绑定的企业微信等登录身份")
    public R<List<ExternalIdentityBindingVO>> listExternalIdentities(
            @Parameter(description = "用户ID") @RequestParam("userId") Long userId) {
        return R.ok(identityUserService.listExternalIdentities(userId));
    }

    @Override
    @GetMapping("/me/external-identities")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "查询当前用户第三方授权")
    @Operation(summary = "查询当前用户第三方授权", description = "查询当前登录用户在当前应用下已绑定的第三方身份")
    public R<List<ExternalIdentityBindingVO>> listCurrentExternalIdentities() {
        return R.ok(identityUserService.listCurrentExternalIdentities());
    }

    @Override
    @DeleteMapping("/me/external-identities")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "解绑当前用户第三方授权")
    @Operation(summary = "解绑当前用户第三方授权", description = "校验当前密码后解除指定的第三方身份绑定")
    public R<Boolean> unbindCurrentExternalIdentity(@RequestBody UnbindCurrentExternalIdentityCommand command) {
        return R.ok(identityUserService.unbindCurrentExternalIdentity(command));
    }

}
