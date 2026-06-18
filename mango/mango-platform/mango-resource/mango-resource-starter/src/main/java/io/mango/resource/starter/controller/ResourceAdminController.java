package io.mango.resource.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.resource.api.ResourceRegistryApi;
import io.mango.resource.api.command.RegisterResourceDeclarationsCommand;
import io.mango.resource.starter.query.ResourceLogPageQuery;
import io.mango.resource.starter.query.ResourceRegistryPageQuery;
import io.mango.resource.starter.service.ResourceAdminService;
import io.mango.resource.starter.vo.ResourceChangeLogVO;
import io.mango.resource.starter.vo.ResourceRegistryVO;
import io.mango.resource.starter.vo.ResourceSyncLogVO;
import io.mango.resource.api.model.ResourceHandlerSpec;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/resource")
@RequiredArgsConstructor
@Tag(name = "资源管理", description = "资源注册中心后台管理接口")
public class ResourceAdminController implements ResourceRegistryApi {

    private final ResourceAdminService resourceAdminService;

    @Override
    @PostMapping("/declarations/register")
    @Operation(summary = "注册远程资源声明")
    public R<Boolean> registerDeclarations(@Valid @RequestBody RegisterResourceDeclarationsCommand command) {
        resourceAdminService.registerDeclarations(command);
        return R.ok(Boolean.TRUE);
    }

    @GetMapping("/registries/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:resource:registry:list")
    @Operation(summary = "分页查询注册资源")
    public R<PageResult<ResourceRegistryVO>> pageRegistries(@ParameterObject ResourceRegistryPageQuery query) {
        return R.ok(resourceAdminService.pageRegistries(query));
    }

    @PostMapping("/sync/force")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:resource:sync:force")
    @Operation(summary = "强制同步资源")
    public R<Boolean> forceSync() {
        resourceAdminService.forceSync();
        return R.ok(Boolean.TRUE);
    }

    @DeleteMapping("/registries")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:resource:registry:delete")
    @Operation(summary = "删除注册资源")
    public R<Boolean> deleteResource(@RequestParam String resourceId,
                                     @RequestParam(defaultValue = "false") boolean physical) {
        resourceAdminService.deleteResource(resourceId, physical);
        return R.ok(Boolean.TRUE);
    }

    @GetMapping("/sync-logs/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:resource:sync-log:list")
    @Operation(summary = "分页查询资源同步记录")
    public R<PageResult<ResourceSyncLogVO>> pageSyncLogs(@ParameterObject ResourceLogPageQuery query) {
        return R.ok(resourceAdminService.pageSyncLogs(query));
    }

    @GetMapping("/change-logs/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:resource:change-log:list")
    @Operation(summary = "分页查询资源变更记录")
    public R<PageResult<ResourceChangeLogVO>> pageChangeLogs(@ParameterObject ResourceLogPageQuery query) {
        return R.ok(resourceAdminService.pageChangeLogs(query));
    }

    @GetMapping("/handler-specs")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:resource:handler:list")
    @Operation(summary = "查询资源处理器字段契约")
    public R<List<ResourceHandlerSpec>> listHandlerSpecs() {
        return R.ok(resourceAdminService.listHandlerSpecs());
    }
}
