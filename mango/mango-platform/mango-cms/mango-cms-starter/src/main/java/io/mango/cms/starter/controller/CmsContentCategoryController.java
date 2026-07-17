package io.mango.cms.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.cms.api.CmsContentCategoryApi;
import io.mango.cms.api.command.SaveCmsContentCategoryCommand;
import io.mango.cms.api.command.UpdateCmsStatusCommand;
import io.mango.cms.api.query.CmsContentCategoryPageQuery;
import io.mango.cms.api.vo.CmsContentCategoryVO;
import io.mango.cms.core.service.ICmsContentCategoryService;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
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
 * CMS 内容分类 HTTP 适配器。
 */
@Validated
@RestController
@RequestMapping("/cms")
@RequiredArgsConstructor
@Tag(name = "CMS 内容分类", description = "内容分类管理接口")
public class CmsContentCategoryController implements CmsContentCategoryApi {

    private final ICmsContentCategoryService contentCategoryService;

    @Override
    @GetMapping("/content-categories/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content-category:list")
    @Operation(summary = "分页查询内容分类", description = "分页查询内容分类")
    public R<PageResult<CmsContentCategoryVO>> pageContentCategories(@ParameterObject CmsContentCategoryPageQuery query) {
        return R.ok(contentCategoryService.pageContentCategories(query));
    }

    @Override
    @GetMapping("/content-categories/list")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content-category:list")
    @Operation(summary = "查询内容分类列表", description = "查询内容分类列表")
    public R<List<CmsContentCategoryVO>> listContentCategories(@ParameterObject CmsContentCategoryPageQuery query) {
        return R.ok(contentCategoryService.listContentCategories(query));
    }

    @Override
    @GetMapping("/content-categories/tree")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content-category:list")
    @Operation(summary = "查询内容分类树", description = "查询内容分类树")
    public R<List<CmsContentCategoryVO>> treeContentCategories(@ParameterObject CmsContentCategoryPageQuery query) {
        return R.ok(contentCategoryService.treeContentCategories(query));
    }

    @Override
    @GetMapping("/content-categories/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content-category:query")
    @Operation(summary = "查询内容分类详情", description = "查询内容分类详情")
    public R<CmsContentCategoryVO> detailContentCategory(
            @Parameter(description = "分类 ID") @RequestParam("id") Long id) {
        return R.ok(contentCategoryService.detailContentCategory(id));
    }

    @Override
    @PostMapping("/content-categories")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content-category:add")
    @Operation(summary = "创建内容分类", description = "创建内容分类")
    public R<Long> createContentCategory(@RequestBody SaveCmsContentCategoryCommand command) {
        return R.ok(contentCategoryService.createContentCategory(command));
    }

    @Override
    @PutMapping("/content-categories")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content-category:edit")
    @Operation(summary = "更新内容分类", description = "更新内容分类")
    public R<Boolean> updateContentCategory(@RequestBody SaveCmsContentCategoryCommand command) {
        return R.ok(contentCategoryService.updateContentCategory(command));
    }

    @Override
    @PutMapping("/content-categories/status")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content-category:status")
    @Operation(summary = "更新内容分类状态", description = "更新内容分类状态")
    public R<Boolean> updateContentCategoryStatus(@RequestBody UpdateCmsStatusCommand command) {
        return R.ok(contentCategoryService.updateContentCategoryStatus(command));
    }

    @Override
    @DeleteMapping("/content-categories")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content-category:delete")
    @Operation(summary = "删除内容分类", description = "删除内容分类")
    public R<Boolean> deleteContentCategory(@Parameter(description = "主键 ID") @RequestParam("id") Long id) {
        return R.ok(contentCategoryService.deleteContentCategory(id));
    }
}
