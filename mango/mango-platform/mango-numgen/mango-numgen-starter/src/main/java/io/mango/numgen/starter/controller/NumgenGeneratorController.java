package io.mango.numgen.starter.controller;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.numgen.api.NumgenGeneratorApi;
import io.mango.numgen.api.command.SaveNumgenGeneratorCommand;
import io.mango.numgen.api.command.UpdateNumgenGeneratorStatusCommand;
import io.mango.numgen.api.query.NumgenGeneratorPageQuery;
import io.mango.numgen.api.vo.NumgenGeneratorVO;
import io.mango.numgen.core.service.INumgenGeneratorService;
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

@Validated
@RestController
@RequestMapping("/numgen/generators")
@RequiredArgsConstructor
@Tag(name = "编号生成器", description = "编号生成器台账接口")
public class NumgenGeneratorController implements NumgenGeneratorApi {

    private final INumgenGeneratorService generatorService;

    @Override
    @GetMapping("/page")
    @Operation(summary = "分页查询编号生成器")
    public R<PageResult<NumgenGeneratorVO>> pageGenerators(@ParameterObject NumgenGeneratorPageQuery query) {
        return generatorService.pageGenerators(query);
    }

    @Override
    @GetMapping("/detail")
    @Operation(summary = "查询编号生成器详情")
    public R<NumgenGeneratorVO> detailGenerator(
            @Parameter(description = "编号生成器 ID", required = true)
            @NotNull(message = "编号生成器 ID 不能为空")
            @RequestParam Long id) {
        return generatorService.detailGenerator(id);
    }

    @Override
    @PostMapping
    @Operation(summary = "新增编号生成器")
    public R<Long> createGenerator(@Valid @RequestBody SaveNumgenGeneratorCommand command) {
        return generatorService.createGenerator(command);
    }

    @Override
    @PutMapping
    @Operation(summary = "修改编号生成器")
    public R<Boolean> updateGenerator(@Valid @RequestBody SaveNumgenGeneratorCommand command) {
        return generatorService.updateGenerator(command);
    }

    @Override
    @PutMapping("/status")
    @Operation(summary = "更新编号生成器状态")
    public R<Boolean> updateGeneratorStatus(@Valid @RequestBody UpdateNumgenGeneratorStatusCommand command) {
        return generatorService.updateGeneratorStatus(command);
    }

    @Override
    @DeleteMapping
    @Operation(summary = "删除编号生成器")
    public R<Boolean> deleteGenerator(
            @Parameter(description = "编号生成器 ID", required = true)
            @NotNull(message = "编号生成器 ID 不能为空")
            @RequestParam Long id) {
        return generatorService.deleteGenerator(id);
    }
}
