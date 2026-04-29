package io.mango.authorization.starter.controller;

import io.mango.authorization.api.AppApi;
import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.command.AppCommand;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.authorization.api.vo.AppVO;
import io.mango.authorization.core.service.IAuthorizationAppService;
import io.mango.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 授权应用入口管理控制器。
 */
@RestController
@RequestMapping("/authorization/apps")
@RequiredArgsConstructor
@Tag(name = "授权应用入口", description = "授权应用入口管理接口")
public class AppController implements AppApi {

    private final IAuthorizationAppService appService;

    @Override
    @GetMapping
    @Operation(summary = "获取应用入口列表")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:app:list")
    public R<List<AppVO>> list() {
        return R.ok(appService.list());
    }

    @Override
    @GetMapping("/{appId}")
    @Operation(summary = "获取应用入口详情")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:app:query")
    public R<AppVO> get(@Parameter(description = "应用ID") @PathVariable Long appId) {
        AppVO app = appService.get(appId);
        return app == null ? R.fail(404, "应用入口不存在") : R.ok(app);
    }

    @Override
    @PostMapping
    @Operation(summary = "创建应用入口")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:app:add")
    public R<Long> create(@RequestBody AppCommand command) {
        return R.ok(appService.create(command));
    }

    @Override
    @PutMapping
    @Operation(summary = "更新应用入口")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:app:edit")
    public R<Boolean> update(@RequestBody AppCommand command) {
        Boolean success = appService.update(command);
        return Boolean.TRUE.equals(success) ? R.ok(true) : R.fail(404, "应用入口不存在");
    }

    @Override
    @DeleteMapping("/{appId}")
    @Operation(summary = "删除应用入口")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:app:delete")
    public R<Boolean> delete(@Parameter(description = "应用ID") @PathVariable Long appId) {
        Boolean success = appService.delete(appId);
        return Boolean.TRUE.equals(success) ? R.ok(true) : R.fail(404, "应用入口不存在");
    }
}
