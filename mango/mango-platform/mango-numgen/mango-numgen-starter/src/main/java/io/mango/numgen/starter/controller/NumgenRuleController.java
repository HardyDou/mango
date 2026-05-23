package io.mango.numgen.starter.controller;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.numgen.api.NumgenRuleApi;
import io.mango.numgen.api.command.NumgenPreviewCommand;
import io.mango.numgen.api.command.NumgenPublishCommand;
import io.mango.numgen.api.command.SaveNumgenRuleCommand;
import io.mango.numgen.api.command.UpdateNumgenRuleStatusCommand;
import io.mango.numgen.api.query.NumgenRulePageQuery;
import io.mango.numgen.api.vo.NumgenPreviewVO;
import io.mango.numgen.api.vo.NumgenRuleVO;
import io.mango.numgen.core.service.INumgenRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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

/**
 * 编号规则接口。
 */
@Validated
@RestController
@RequestMapping("/numgen/rules")
@RequiredArgsConstructor
@Tag(name = "编号规则", description = "编号规则数据接口")
public class NumgenRuleController implements NumgenRuleApi {

    private final INumgenRuleService numgenRuleService;

    @Override
    @GetMapping("/page")
    @Operation(summary = "分页查询编号规则", description = "分页查询编号规则数据")
    public R<PageResult<NumgenRuleVO>> pageRules(@ParameterObject NumgenRulePageQuery query) {
        return numgenRuleService.pageRules(query);
    }

    @Override
    @GetMapping("/detail")
    @Operation(summary = "查询编号规则详情", description = "按编号规则 ID 查询详情")
    public R<NumgenRuleVO> detailRule(
            @Parameter(description = "编号规则 ID", required = true)
            @NotNull(message = "编号规则 ID 不能为空")
            @RequestParam Long id) {
        return numgenRuleService.detailRule(id);
    }

    @Override
    @PostMapping
    @Operation(summary = "新增编号规则", description = "创建编号规则")
    public R<Long> createRule(@Valid @RequestBody SaveNumgenRuleCommand command) {
        return numgenRuleService.createRule(command);
    }

    @Override
    @PutMapping
    @Operation(summary = "修改编号规则", description = "更新编号规则")
    public R<Boolean> updateRule(@Valid @RequestBody SaveNumgenRuleCommand command) {
        return numgenRuleService.updateRule(command);
    }

    @Override
    @PutMapping("/status")
    @Operation(summary = "更新编号规则状态", description = "启用或停用编号规则")
    public R<Boolean> updateRuleStatus(@Valid @RequestBody UpdateNumgenRuleStatusCommand command) {
        return numgenRuleService.updateRuleStatus(command);
    }

    @Override
    @DeleteMapping
    @Operation(summary = "删除编号规则", description = "按 ID 删除编号规则")
    public R<Boolean> deleteRule(
            @Parameter(description = "编号规则 ID", required = true)
            @NotNull(message = "编号规则 ID 不能为空")
            @RequestParam Long id) {
        return numgenRuleService.deleteRule(id);
    }

    @Override
    @PostMapping("/publish")
    @Operation(summary = "发布编号规则", description = "发布后成为当前生效规则")
    public R<Boolean> publishRule(@Valid @RequestBody NumgenPublishCommand command) {
        return numgenRuleService.publishRule(command);
    }

    @Override
    @PostMapping("/preview")
    @Operation(summary = "预览编号规则", description = "预览当前已发布规则，不消耗真实流水")
    public R<NumgenPreviewVO> previewRule(@Valid @RequestBody NumgenPreviewCommand command) {
        return numgenRuleService.previewRule(command);
    }
}
