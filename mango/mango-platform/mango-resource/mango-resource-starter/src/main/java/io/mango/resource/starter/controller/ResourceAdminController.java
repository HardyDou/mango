package io.mango.resource.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.resource.api.ResourceAdminApi;
import io.mango.resource.api.query.ResourceLogPageQuery;
import io.mango.resource.api.query.ResourceRegistryPageQuery;
import io.mango.resource.api.vo.ResourceChangeLogVO;
import io.mango.resource.api.vo.ResourceHandlerSpecVO;
import io.mango.resource.api.vo.ResourceRegistryVO;
import io.mango.resource.api.vo.ResourceSyncLogVO;
import io.mango.resource.core.service.IResourceAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/resource")
@RequiredArgsConstructor
@Validated
@Tag(name = "资源管理", description = "资源注册中心后台管理接口")
public class ResourceAdminController implements ResourceAdminApi {

    private final IResourceAdminService resourceAdminService;

    @GetMapping("/registries/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:resource:registry:list")
    @Operation(summary = "分页查询注册资源", description = "按资源类型、模块、状态和关键词分页查询资源注册记录")
    public R<PageResult<ResourceRegistryVO>> pageRegistries(@ParameterObject ResourceRegistryPageQuery query) {
        return R.ok(resourceAdminService.pageRegistries(query));
    }

    @PostMapping("/sync/force")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:resource:sync:force")
    @Operation(summary = "强制同步资源", description = "忽略声明摘要未变化判断并重新执行本地资源同步")
    public R<Boolean> forceSync() {
        return R.ok(resourceAdminService.forceSync());
    }

    @DeleteMapping("/registries")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:resource:registry:delete")
    @Operation(summary = "删除注册资源", description = "默认逻辑禁用目标资源，physical=true 时请求目标处理器物理删除")
    public R<Boolean> deleteResource(
            @Parameter(description = "稳定资源ID") @RequestParam("resourceId") String resourceId,
            @Parameter(description = "是否物理删除") @RequestParam(value = "physical", defaultValue = "false")
            Boolean physical) {
        return R.ok(resourceAdminService.deleteResource(resourceId, physical));
    }

    @GetMapping("/sync-logs/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:resource:sync-log:list")
    @Operation(summary = "分页查询资源同步记录", description = "按资源注册记录ID分页查询同步执行结果")
    public R<PageResult<ResourceSyncLogVO>> pageSyncLogs(@ParameterObject ResourceLogPageQuery query) {
        return R.ok(resourceAdminService.pageSyncLogs(query));
    }

    @GetMapping("/change-logs/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:resource:change-log:list")
    @Operation(summary = "分页查询资源变更记录", description = "按资源注册记录ID分页查询声明变更前后内容")
    public R<PageResult<ResourceChangeLogVO>> pageChangeLogs(@ParameterObject ResourceLogPageQuery query) {
        return R.ok(resourceAdminService.pageChangeLogs(query));
    }

    @GetMapping("/handler-specs")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:resource:handler:list")
    @Operation(summary = "查询资源处理器字段契约", description = "查询当前应用已注册处理器支持的资源类型和字段说明")
    public R<List<ResourceHandlerSpecVO>> listHandlerSpecs() {
        return R.ok(resourceAdminService.listHandlerSpecs());
    }
}
