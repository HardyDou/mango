package io.mango.cms.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.cms.api.CmsSiteCategoryApi;
import io.mango.cms.api.command.SaveCmsSiteCategoryCommand;
import io.mango.cms.api.command.UpdateCmsStatusCommand;
import io.mango.cms.api.query.CmsSiteCategoryTreeQuery;
import io.mango.cms.api.vo.CmsSiteCategoryVO;
import io.mango.cms.core.service.ICmsSiteCategoryService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * CMS 站点栏目 HTTP 适配器。
 */
@Validated
@RestController
@RequestMapping("/cms")
@RequiredArgsConstructor
@Tag(name = "CMS 站点栏目", description = "站点栏目管理接口")
public class CmsSiteCategoryController implements CmsSiteCategoryApi {

    private final ICmsSiteCategoryService siteCategoryService;

    @Override
    @GetMapping("/site-categories/tree")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:site-category:list")
    @Operation(summary = "查询站点栏目树", description = "查询站点栏目树")
    public R<List<CmsSiteCategoryVO>> treeSiteCategories(@ParameterObject CmsSiteCategoryTreeQuery query) {
        return R.ok(siteCategoryService.treeSiteCategories(query));
    }

    @Override
    @GetMapping("/site-categories/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:site-category:query")
    @Operation(summary = "查询站点栏目详情", description = "查询站点栏目详情")
    public R<CmsSiteCategoryVO> detailSiteCategory(
            @Parameter(description = "主键 ID") @RequestParam("id") Long id) {
        return R.ok(siteCategoryService.detailSiteCategory(id));
    }

    @Override
    @PostMapping("/site-categories")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:site-category:add")
    @Operation(summary = "创建站点栏目", description = "创建站点栏目")
    public R<Long> createSiteCategory(@RequestBody SaveCmsSiteCategoryCommand command) {
        return R.ok(siteCategoryService.createSiteCategory(command));
    }

    @Override
    @PutMapping("/site-categories")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:site-category:edit")
    @Operation(summary = "更新站点栏目", description = "更新站点栏目")
    public R<Boolean> updateSiteCategory(@RequestBody SaveCmsSiteCategoryCommand command) {
        return R.ok(siteCategoryService.updateSiteCategory(command));
    }

    @Override
    @PutMapping("/site-categories/status")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:site-category:status")
    @Operation(summary = "更新站点栏目状态", description = "更新站点栏目状态")
    public R<Boolean> updateSiteCategoryStatus(@RequestBody UpdateCmsStatusCommand command) {
        return R.ok(siteCategoryService.updateSiteCategoryStatus(command));
    }

    @Override
    @DeleteMapping("/site-categories")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:site-category:delete")
    @Operation(summary = "删除站点栏目", description = "删除站点栏目")
    public R<Boolean> deleteSiteCategory(@Parameter(description = "主键 ID") @RequestParam("id") Long id) {
        return R.ok(siteCategoryService.deleteSiteCategory(id));
    }
}
