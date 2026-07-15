package io.mango.authorization.starter.controller;

import io.mango.authorization.api.DataScopeApi;
import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.command.SaveRoleDataScopeCommand;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.authorization.api.query.EffectiveDataScopeQuery;
import io.mango.authorization.api.vo.EffectiveDataScopeVO;
import io.mango.authorization.api.vo.RoleDataScopeVO;
import io.mango.authorization.core.service.IRoleDataScopeService;
import io.mango.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 数据权限控制器。
 */
@RestController
@RequestMapping("/authorization/data-scopes")
@RequiredArgsConstructor
@Validated
@Tag(name = "数据权限", description = "角色数据权限配置与解析接口")
public class DataScopeController implements DataScopeApi {

    private final IRoleDataScopeService roleDataScopeService;

    @Override
    @GetMapping("/roles")
    @Operation(summary = "查询角色数据权限", description = "权限接口。按角色ID查询数据权限配置")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:role:query")
    public R<List<RoleDataScopeVO>> listRoleScopes(
            @Parameter(description = "角色ID") @RequestParam(name = "roleId") Long roleId) {
        return R.ok(roleDataScopeService.listByRole(roleId));
    }

    @Override
    @PostMapping("/roles")
    @Operation(summary = "保存角色数据权限", description = "权限接口。创建或更新角色在指定资源上的数据权限")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:role:assign")
    public R<Boolean> saveRoleScope(@RequestBody SaveRoleDataScopeCommand command) {
        return R.ok(roleDataScopeService.save(command));
    }

    @Override
    @DeleteMapping("/roles")
    @Operation(summary = "删除角色数据权限", description = "权限接口。删除角色在指定资源上的数据权限配置")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:role:assign")
    public R<Boolean> deleteRoleScope(
            @Parameter(description = "角色ID") @RequestParam(name = "roleId") Long roleId,
            @Parameter(description = "资源编码") @RequestParam(name = "resourceCode") String resourceCode) {
        return R.ok(roleDataScopeService.delete(roleId, resourceCode));
    }

    @Override
    @GetMapping("/effective")
    @Operation(summary = "查询生效数据权限", description = "登录接口。查询当前主体在指定资源上的生效数据权限")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    public R<EffectiveDataScopeVO> effective(@ParameterObject EffectiveDataScopeQuery scopeQuery) {
        return R.ok(roleDataScopeService.resolveCurrent(scopeQuery));
    }
}
