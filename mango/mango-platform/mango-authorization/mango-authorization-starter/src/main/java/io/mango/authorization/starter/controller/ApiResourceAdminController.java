package io.mango.authorization.starter.controller;

import io.mango.authorization.api.ApiResourceAdminApi;
import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.command.ApiResourceManualCommand;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.authorization.api.query.ApiResourceManualQuery;
import io.mango.authorization.api.query.ApiResourcePageQuery;
import io.mango.authorization.api.vo.ApiResourceFilterOptionsVO;
import io.mango.authorization.api.vo.ApiResourceVO;
import io.mango.authorization.core.service.IApiResourceService;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 权限标识管理端点。
 *
 * @author hardy
 */
@RestController
@RequestMapping("/authorization/api-resources")
@RequiredArgsConstructor
@Validated
@Tag(name = "权限标识", description = "接口资源登记来源、模块、领域与权限码查询接口")
public class ApiResourceAdminController implements ApiResourceAdminApi {

    private final IApiResourceService apiResourceService;

    @GetMapping("/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:api-resource:list")
    @Operation(summary = "分页查询权限标识",
            description = "按模块、领域、登记来源、访问模式、状态和关键词查询接口资源登记明细")
    @Override
    public R<PageResult<ApiResourceVO>> pageApiResources(@ParameterObject ApiResourcePageQuery query) {
        return R.ok(apiResourceService.pageApiResources(query));
    }

    @GetMapping("/filter-options")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:api-resource:list")
    @Operation(summary = "查询权限标识筛选维度", description = "返回当前已登记接口资源的模块和领域列表")
    @Override
    public R<ApiResourceFilterOptionsVO> listApiResourceFilterOptions() {
        return R.ok(apiResourceService.listApiResourceFilterOptions());
    }

    @PostMapping("/manual")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:api-resource:manual")
    @Operation(summary = "人工接管接口资源",
            description = "按模块、HTTP 方法和路径登记人工资源，接管后优先于 Controller 扫描、固定声明和 Gateway 默认策略")
    @Override
    public R<Void> saveManualApiResource(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "人工接管命令")
            @RequestBody ApiResourceManualCommand command) {
        apiResourceService.saveManualApiResource(command);
        return R.ok();
    }

    @PutMapping("/manual/release")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:api-resource:manual")
    @Operation(summary = "释放接口资源人工接管",
            description = "停用人工接管记录，使 Controller 扫描或固定声明在下次同步后重新生效")
    @Override
    public R<Void> releaseManualApiResource(@ParameterObject ApiResourceManualQuery query) {
        apiResourceService.releaseManualApiResource(query);
        return R.ok();
    }
}
