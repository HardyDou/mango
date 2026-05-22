package io.mango.template.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.template.api.TemplateCategoryApi;
import io.mango.template.api.command.SaveTemplateCategoryCommand;
import io.mango.template.api.command.UpdateTemplateCategoryStatusCommand;
import io.mango.template.api.query.TemplateCategoryPageQuery;
import io.mango.template.api.vo.TemplateCategoryVO;
import io.mango.template.core.service.ITemplateCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模板分类管理接口。
 */
@RestController
@RequestMapping("/template/categories")
@RequiredArgsConstructor
@Validated
@Tag(name = "模板分类管理", description = "模板分类维护接口")
public class TemplateCategoryController implements TemplateCategoryApi {

    private final ITemplateCategoryService categoryService;

    @GetMapping("/page")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "分页查询模板分类")
    @Operation(summary = "分页查询模板分类", description = "按分类名称、编码和状态分页查询模板分类。")
    @Override
    public R<PageResult<TemplateCategoryVO>> page(@ParameterObject TemplateCategoryPageQuery query) {
        return categoryService.page(query);
    }

    @GetMapping("/list")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "查询模板分类列表")
    @Operation(summary = "查询模板分类列表", description = "查询模板分类列表，用于下拉选择。")
    @Override
    public R<List<TemplateCategoryVO>> list(@ParameterObject TemplateCategoryPageQuery query) {
        return categoryService.list(query);
    }

    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "查询模板分类详情")
    @Operation(summary = "查询模板分类详情", description = "按ID查询模板分类详情。")
    @Override
    public R<TemplateCategoryVO> detail(@Parameter(description = "模板分类ID", required = true) @RequestParam Long id) {
        return categoryService.detail(id);
    }

    @PostMapping
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "新增模板分类")
    @Operation(summary = "新增模板分类", description = "创建模板分类。")
    @Override
    public R<Long> create(@RequestBody SaveTemplateCategoryCommand command) {
        return categoryService.create(command);
    }

    @PutMapping
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "修改模板分类")
    @Operation(summary = "修改模板分类", description = "修改模板分类名称、排序、状态和备注。")
    @Override
    public R<Boolean> update(@RequestBody SaveTemplateCategoryCommand command) {
        return categoryService.update(command);
    }

    @PutMapping("/status")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "启停模板分类")
    @Operation(summary = "启停模板分类", description = "启用或停用模板分类。")
    @Override
    public R<Boolean> updateStatus(@RequestBody UpdateTemplateCategoryStatusCommand command) {
        return categoryService.updateStatus(command);
    }

    @DeleteMapping
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "删除模板分类")
    @Operation(summary = "删除模板分类", description = "删除模板分类。")
    @Override
    public R<Boolean> delete(@Parameter(description = "模板分类ID", required = true) @RequestParam Long id) {
        return categoryService.delete(id);
    }
}
