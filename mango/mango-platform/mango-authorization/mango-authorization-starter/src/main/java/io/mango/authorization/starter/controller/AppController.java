package io.mango.authorization.starter.controller;

import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.AppApi;
import io.mango.authorization.api.ITokenProvider;
import io.mango.authorization.api.TokenContextHolder;
import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.command.AppCommand;
import io.mango.authorization.api.vo.AppRuntimeDescriptorVO;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.authorization.api.vo.AppVO;
import io.mango.authorization.core.service.IAuthorizationAppService;
import io.mango.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 授权应用管理控制器。
 */
@RestController
@RequestMapping("/authorization/apps")
@RequiredArgsConstructor
@Tag(name = "授权应用", description = "授权应用与前端运行配置管理接口")
public class AppController implements AppApi {

    private final IAuthorizationAppService appService;
    private final ITokenProvider tokenService;

    @Override
    @GetMapping
    @Operation(summary = "获取应用列表", description = "权限接口。查询授权应用及前端运行配置列表")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:app:list")
    public R<List<AppVO>> list() {
        return R.ok(appService.listByQuery(null));
    }

    @Override
    @GetMapping("/detail")
    @Operation(summary = "获取应用详情", description = "权限接口。按应用ID查询授权应用及前端运行配置详情")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:app:query")
    public R<AppVO> get(@Parameter(description = "应用ID") @RequestParam Long appId) {
        AppVO app = appService.get(appId);
        return app == null ? R.fail(404, "应用不存在") : R.ok(app);
    }

    @Override
    @GetMapping("/runtime")
    @Operation(summary = "获取应用运行配置", description = "登录接口。返回当前租户和主体可访问的前端运行配置")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "查询应用运行配置")
    public R<List<AppVO>> runtime() {
        return R.ok(appService.listRuntimeApps(getCurrentAuthorizationQuery()));
    }

    @Override
    @GetMapping("/runtime/descriptor")
    @Operation(summary = "获取前端运行描述", description = "登录接口。返回当前部署配置档、运行单元和模块运行策略")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "查询前端运行描述")
    public R<AppRuntimeDescriptorVO> runtimeDescriptor(
            @Parameter(description = "逻辑应用编码") @RequestParam(defaultValue = "internal-admin") String appCode) {
        return R.ok(appService.runtimeDescriptor(getCurrentAuthorizationQuery(), appCode));
    }

    @Override
    @GetMapping({"/runtime/{appCode}", "/runtime/detail/{appCode}"})
    @Operation(summary = "获取单个应用运行配置", description = "登录接口。按应用编码返回当前主体可访问的前端运行配置")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "查询单个应用运行配置")
    public R<AppVO> runtimeDetail(@Parameter(description = "应用编码") @PathVariable String appCode) {
        AppVO app = appService.getByAppCode(appCode);
        boolean visible = appService.listRuntimeApps(getCurrentAuthorizationQuery())
                .stream()
                .anyMatch(item -> appCode.equals(item.getAppCode()));
        return app == null || !visible ? R.fail(404, "应用运行配置不存在") : R.ok(app);
    }

    private AuthorizationQuery getCurrentAuthorizationQuery() {
        String rawToken = TokenContextHolder.getToken();
        String token = stripBearer(rawToken);
        if (token == null) {
            return null;
        }
        Long memberId = parseLong(tokenService.getClaim(token, "memberId"));
        if (memberId == null) {
            return null;
        }
        return AuthorizationQuery.member(memberId)
                .withTenantId(tokenService.getClaim(token, "tenantId"))
                .withRealm(tokenService.getClaim(token, "realm"))
                .withActorType(tokenService.getClaim(token, "actorType"))
                .withParty(tokenService.getClaim(token, "partyType"), parseLong(tokenService.getClaim(token, "partyId")));
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String stripBearer(String token) {
        if (token == null) {
            return null;
        }
        if (token.startsWith(ITokenProvider.BEARER_PREFIX)) {
            return token.substring(ITokenProvider.BEARER_PREFIX.length());
        }
        return token;
    }

    @Override
    @PostMapping
    @Operation(summary = "创建应用", description = "权限接口。创建授权应用及前端运行配置")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:app:add")
    public R<Long> create(@Valid @RequestBody AppCommand command) {
        return R.ok(appService.create(command));
    }

    @Override
    @PutMapping
    @Operation(summary = "更新应用", description = "权限接口。更新授权应用及前端运行配置")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:app:edit")
    public R<Boolean> update(@Valid @RequestBody AppCommand command) {
        Boolean success = appService.update(command);
        return Boolean.TRUE.equals(success) ? R.ok(true) : R.fail(404, "应用不存在");
    }

    @Override
    @DeleteMapping
    @Operation(summary = "删除应用", description = "权限接口。按应用ID删除授权应用")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:app:delete")
    public R<Boolean> delete(@Parameter(description = "应用ID") @RequestParam Long appId) {
        Boolean success = appService.delete(appId);
        return Boolean.TRUE.equals(success) ? R.ok(true) : R.fail(404, "应用不存在");
    }
}
