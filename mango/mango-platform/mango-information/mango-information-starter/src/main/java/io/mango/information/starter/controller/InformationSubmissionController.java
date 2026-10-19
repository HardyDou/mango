package io.mango.information.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.information.api.InformationSubmissionApi;
import io.mango.information.api.command.ProcessInformationSubmissionCommand;
import io.mango.information.api.query.InformationSubmissionPageQuery;
import io.mango.information.api.vo.InformationSubmissionVO;
import io.mango.information.core.service.IInformationSubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 信息采集采集记录管理接口。 */
@RestController
@RequestMapping("/information/submissions")
@RequiredArgsConstructor
@Validated
@Tag(name = "信息采集采集记录")
public class InformationSubmissionController implements InformationSubmissionApi {
    private final IInformationSubmissionService service;

    @Override
    @GetMapping("/page")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "分页查询信息采集采集记录")
    @Operation(summary = "分页查询采集记录")
    public R<PageResult<InformationSubmissionVO>> page(@ParameterObject InformationSubmissionPageQuery query) { return R.ok(service.page(query)); }

    @Override
    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "查询信息采集采集记录")
    @Operation(summary = "查询采集记录")
    public R<InformationSubmissionVO> detail(@RequestParam Long id) { return R.ok(service.detail(id)); }

    @Override
    @GetMapping("/export")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "导出信息采集采集记录")
    @Operation(summary = "导出采集记录")
    public R<String> export(@ParameterObject InformationSubmissionPageQuery query) { return R.ok(service.exportCsv(query)); }

    @Override
    @PutMapping("/process")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "处理信息采集采集记录")
    @Operation(summary = "更新处理状态")
    public R<Boolean> process(@RequestBody ProcessInformationSubmissionCommand command) { return R.ok(service.process(command)); }
}
