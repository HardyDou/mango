package io.mango.numgen.starter.controller;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.numgen.api.NumgenSequenceApi;
import io.mango.numgen.api.query.NumgenSequencePageQuery;
import io.mango.numgen.api.vo.NumgenSequenceVO;
import io.mango.numgen.core.service.INumgenSequenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 编号序列接口。
 */
@Validated
@RestController
@RequestMapping("/numgen/sequences")
@RequiredArgsConstructor
@Tag(name = "编号序列", description = "编号序列数据接口")
public class NumgenSequenceController implements NumgenSequenceApi {

    private final INumgenSequenceService numgenSequenceService;

    @Override
    @GetMapping("/page")
    @Operation(summary = "分页查询编号序列", description = "分页查询编号序列数据")
    public R<PageResult<NumgenSequenceVO>> pageSequences(@ParameterObject NumgenSequencePageQuery query) {
        return numgenSequenceService.pageSequences(query);
    }
}
