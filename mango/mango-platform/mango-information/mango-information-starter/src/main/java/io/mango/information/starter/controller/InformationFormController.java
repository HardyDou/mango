package io.mango.information.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.information.api.InformationFormApi;
import io.mango.information.api.command.SaveInformationFormCommand;
import io.mango.information.api.query.InformationFormPageQuery;
import io.mango.information.api.vo.InformationFormDetailVO;
import io.mango.information.api.vo.InformationFormVO;
import io.mango.information.core.service.IInformationFormService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 信息采集表单管理接口。 */
@RestController
@RequestMapping("/information/forms")
@RequiredArgsConstructor
@Validated
@Tag(name = "信息采集表单")
public class InformationFormController implements InformationFormApi {
    private final IInformationFormService service;

    @Override
    @GetMapping("/page")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "分页查询信息采集表单")
    @Operation(summary = "分页查询表单")
    public R<PageResult<InformationFormVO>> page(@ParameterObject InformationFormPageQuery query) { return R.ok(service.page(query)); }

    @Override
    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "查询信息采集表单详情")
    @Operation(summary = "查询表单详情")
    public R<InformationFormDetailVO> detail(@RequestParam Long id) { return R.ok(service.detail(id)); }

    @Override
    @PostMapping("/save")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "保存信息采集表单")
    @Operation(summary = "保存表单")
    public R<Long> save(@RequestBody SaveInformationFormCommand command) { return R.ok(service.save(command)); }

    @Override
    @DeleteMapping
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "删除信息采集表单")
    @Operation(summary = "删除表单")
    public R<Boolean> delete(@RequestParam Long id) { return R.ok(service.delete(id)); }
}
