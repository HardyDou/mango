package io.mango.numgen.starter.controller;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.numgen.api.NumgenHistoryApi;
import io.mango.numgen.api.query.NumgenHistoryPageQuery;
import io.mango.numgen.api.vo.NumgenHistoryVO;
import io.mango.numgen.core.service.INumgenHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 发号历史接口。
 */
@Validated
@RestController
@RequestMapping("/numgen/histories")
@RequiredArgsConstructor
@Tag(name = "发号历史", description = "发号历史数据接口")
public class NumgenHistoryController implements NumgenHistoryApi {

    private final INumgenHistoryService numgenHistoryService;

    @Override
    @GetMapping("/page")
    @Operation(summary = "分页查询发号历史", description = "分页查询发号历史数据")
    public R<PageResult<NumgenHistoryVO>> pageHistories(@ParameterObject NumgenHistoryPageQuery query) {
        return numgenHistoryService.pageHistories(query);
    }
}
