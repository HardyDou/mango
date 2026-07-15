package io.mango.numgen.starter.controller;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.numgen.api.NumgenSegmentApi;
import io.mango.numgen.api.command.SaveNumgenRuleSegmentCommand;
import io.mango.numgen.api.query.NumgenSegmentPageQuery;
import io.mango.numgen.api.vo.NumgenRuleSegmentVO;
import io.mango.numgen.core.service.INumgenSegmentService;
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

@Validated
@RestController
@RequestMapping("/numgen/segments")
@RequiredArgsConstructor
@Tag(name = "编号规则片段", description = "编号规则片段接口")
public class NumgenSegmentController implements NumgenSegmentApi {

    private final INumgenSegmentService segmentService;

    @Override
    @GetMapping("/page")
    @Operation(summary = "分页查询编号规则片段", description = "按规则分页查询编号规则片段")
    public R<PageResult<NumgenRuleSegmentVO>> pageSegments(@ParameterObject NumgenSegmentPageQuery query) {
        return R.ok(segmentService.pageSegments(query));
    }

    @Override
    @GetMapping("/detail")
    @Operation(summary = "查询编号规则片段详情", description = "按编号规则片段 ID 查询详情")
    public R<NumgenRuleSegmentVO> detailSegment(
            @Parameter(description = "编号规则片段 ID", required = true)
            @RequestParam(name = "id") Long id) {
        return R.ok(segmentService.detailSegment(id));
    }

    @Override
    @PostMapping
    @Operation(summary = "新增编号规则片段", description = "为草稿规则新增一个编号片段")
    public R<Long> createSegment(@RequestBody SaveNumgenRuleSegmentCommand command) {
        return R.ok(segmentService.createSegment(command));
    }

    @Override
    @PutMapping
    @Operation(summary = "修改编号规则片段", description = "修改草稿规则的编号片段")
    public R<Boolean> updateSegment(@RequestBody SaveNumgenRuleSegmentCommand command) {
        return R.ok(segmentService.updateSegment(command));
    }

    @Override
    @DeleteMapping
    @Operation(summary = "删除编号规则片段", description = "按 ID 删除草稿规则的编号片段")
    public R<Boolean> deleteSegment(
            @Parameter(description = "编号规则片段 ID", required = true)
            @RequestParam(name = "id") Long id) {
        return R.ok(segmentService.deleteSegment(id));
    }
}
