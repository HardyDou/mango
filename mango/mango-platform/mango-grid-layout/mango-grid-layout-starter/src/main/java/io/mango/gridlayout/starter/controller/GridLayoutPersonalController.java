package io.mango.gridlayout.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.gridlayout.api.GridLayoutPersonalApi;
import io.mango.gridlayout.api.command.SaveGridLayoutPersonalCommand;
import io.mango.gridlayout.api.query.GridLayoutPersonalQuery;
import io.mango.gridlayout.api.vo.GridLayoutPersonalVO;
import io.mango.gridlayout.core.service.IGridLayoutPersonalService;
import io.mango.infra.log.annotation.Log;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/grid-layout/personal")
@RequiredArgsConstructor
@Tag(name = "自定义栅格布局", description = "当前登录用户的自定义栅格布局接口")
public class GridLayoutPersonalController implements GridLayoutPersonalApi {

    private final IGridLayoutPersonalService gridLayoutPersonalService;

    @Override
    @GetMapping
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "查询个人栅格布局")
    @Operation(summary = "查询个人栅格布局", description = "登录接口。查询当前用户指定页面的自定义栅格布局")
    public R<GridLayoutPersonalVO> getPersonal(@Valid @ParameterObject GridLayoutPersonalQuery query) {
        return R.ok(gridLayoutPersonalService.getPersonal(query));
    }

    @Override
    @PutMapping
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "保存个人栅格布局")
    @Operation(summary = "保存个人栅格布局", description = "登录接口。按当前租户和当前用户保存指定页面的自定义栅格布局")
    @Log("保存个人栅格布局")
    public R<GridLayoutPersonalVO> savePersonal(@RequestBody @Valid SaveGridLayoutPersonalCommand command) {
        return R.ok(gridLayoutPersonalService.savePersonal(command));
    }

    @Override
    @DeleteMapping
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "删除个人栅格布局")
    @Operation(summary = "删除个人栅格布局", description = "登录接口。删除当前用户指定页面的自定义栅格布局")
    @Log("删除个人栅格布局")
    public R<Boolean> deletePersonal(@Valid @ParameterObject GridLayoutPersonalQuery query) {
        return R.ok(gridLayoutPersonalService.deletePersonal(query));
    }
}
