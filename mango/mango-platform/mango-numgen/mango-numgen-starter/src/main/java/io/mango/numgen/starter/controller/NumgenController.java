package io.mango.numgen.starter.controller;

import io.mango.common.result.R;
import io.mango.numgen.api.NumgenApi;
import io.mango.numgen.api.command.NumgenBatchCommand;
import io.mango.numgen.api.command.NumgenNextCommand;
import io.mango.numgen.api.command.NumgenValidateRuleCommand;
import io.mango.numgen.api.vo.NumgenRuleValidationVO;
import io.mango.numgen.core.service.INumgenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@RestController
@RequestMapping("/numgen")
@RequiredArgsConstructor
@Tag(name = "编号生成", description = "编号生成、批量生成和规则校验接口")
public class NumgenController implements NumgenApi {

    private final INumgenService numgenService;

    @Override
    @PostMapping("/next")
    @Operation(summary = "生成单个编号", description = "按照已发布规则生成一个新编号")
    public R<String> nextValue(@RequestBody NumgenNextCommand command) {
        return R.ok(numgenService.nextValue(command));
    }

    @Override
    @PostMapping("/batch")
    @Operation(summary = "批量生成编号", description = "按照已发布规则批量生成连续编号")
    public R<List<String>> batchValue(@RequestBody NumgenBatchCommand command) {
        return R.ok(numgenService.batchValue(command));
    }

    @Override
    @PostMapping("/rules/validate")
    @Operation(summary = "校验编号规则", description = "校验规则及其片段配置是否完整有效")
    public R<NumgenRuleValidationVO> validateRule(@RequestBody NumgenValidateRuleCommand command) {
        return R.ok(numgenService.validateRule(command));
    }
}
