package io.mango.template.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.template.api.TemplateApi;
import io.mango.template.api.command.*;
import io.mango.template.api.query.TemplatePageQuery;
import io.mango.template.api.query.TemplateRenderRecordPageQuery;
import io.mango.template.api.vo.TemplateDetailVO;
import io.mango.template.api.vo.TemplateRenderRecordVO;
import io.mango.template.api.vo.TemplateRenderResultVO;
import io.mango.template.api.vo.TemplateVO;
import io.mango.template.core.service.ITemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模板管理接口。
 */
@RestController
@RequestMapping("/template/templates")
@RequiredArgsConstructor
@Validated
@Tag(name = "模板管理", description = "模板库、变量提取、同步渲染和异步渲染接口")
public class TemplateController implements TemplateApi {

    private final ITemplateService templateService;

    @GetMapping("/page")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "分页查询模板")
    @Operation(summary = "分页查询模板", description = "按模板名称、编码、分类、源格式和状态分页查询模板库。")
    @Override
    public R<PageResult<TemplateVO>> page(@ParameterObject TemplatePageQuery query) {
        return templateService.page(query);
    }

    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "查询模板详情")
    @Operation(summary = "查询模板详情", description = "查询模板基础信息及版本列表。")
    @Override
    public R<TemplateDetailVO> detail(@Parameter(description = "模板ID", required = true) @RequestParam Long id) {
        return templateService.detail(id);
    }

    @PostMapping
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "新增模板")
    @Operation(summary = "新增模板", description = "创建模板主数据，模板内容通过发布版本维护。")
    @Override
    public R<Long> create(@RequestBody SaveTemplateCommand command) {
        return templateService.create(command);
    }

    @PutMapping
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "修改模板")
    @Operation(summary = "修改模板", description = "修改模板名称、分类、源格式和备注等主数据。")
    @Override
    public R<Boolean> update(@RequestBody SaveTemplateCommand command) {
        return templateService.update(command);
    }

    @DeleteMapping
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "删除模板")
    @Operation(summary = "删除模板", description = "删除模板主数据、版本和渲染记录。")
    @Override
    public R<Boolean> delete(@Parameter(description = "模板ID", required = true) @RequestParam Long id) {
        return templateService.delete(id);
    }

    @PutMapping("/status")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "启停模板")
    @Operation(summary = "启停模板", description = "启用或停用模板，停用后不可作为最新启用版本渲染。")
    @Override
    public R<Boolean> updateStatus(@RequestBody UpdateTemplateStatusCommand command) {
        return templateService.updateStatus(command);
    }

    @PostMapping("/versions")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "发布模板版本")
    @Operation(summary = "发布模板版本", description = "发布新的模板版本，并设置为当前发布版本。")
    @Override
    public R<Long> publishVersion(@RequestBody PublishTemplateVersionCommand command) {
        return templateService.publishVersion(command);
    }

    @PutMapping("/versions/current")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "启用模板历史版本")
    @Operation(summary = "启用模板历史版本", description = "把指定历史版本设置为当前启用版本。")
    @Override
    public R<Boolean> activateVersion(@RequestBody ActivateTemplateVersionCommand command) {
        return templateService.activateVersion(command);
    }

    @PostMapping("/variables/extract")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "提取模板变量建议")
    @Operation(summary = "提取模板变量建议", description = "从文本、HTML 或文档模板中提取变量建议。变量定义以人工维护的变量清单为准，TEXT/HTML 使用 Freemarker 语法。")
    @Override
    public R<List<String>> extractVariables(@RequestBody ExtractTemplateVariablesCommand command) {
        return templateService.extractVariables(command);
    }

    @PostMapping("/render")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "同步渲染模板")
    @Operation(summary = "同步渲染模板", description = "业务方传入动态变量并同步获得渲染结果。")
    @Override
    public R<TemplateRenderResultVO> render(@RequestBody TemplateRenderCommand command) {
        return templateService.render(command);
    }

    @PostMapping("/render/async")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "异步渲染模板")
    @Operation(summary = "异步渲染模板", description = "业务方传入动态变量并提交异步渲染任务，后续通过记录查询结果。")
    @Override
    public R<TemplateRenderResultVO> renderAsync(@RequestBody TemplateRenderCommand command) {
        return templateService.renderAsync(command);
    }

    @GetMapping("/render-records/detail")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "查询渲染记录")
    @Operation(summary = "查询渲染记录", description = "按渲染记录ID查询同步或异步渲染执行结果。")
    @Override
    public R<TemplateRenderRecordVO> renderRecord(@Parameter(description = "渲染记录ID", required = true) @RequestParam Long id) {
        return templateService.renderRecord(id);
    }

    @GetMapping("/render-records/page")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "分页查询渲染记录")
    @Operation(summary = "分页查询渲染记录", description = "按模板、状态或业务标识分页查询渲染记录。")
    @Override
    public R<PageResult<TemplateRenderRecordVO>> renderRecordPage(@ParameterObject TemplateRenderRecordPageQuery query) {
        return templateService.renderRecordPage(query);
    }
}
