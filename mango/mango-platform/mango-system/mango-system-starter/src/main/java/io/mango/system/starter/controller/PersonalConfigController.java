package io.mango.system.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.infra.log.annotation.Log;
import io.mango.system.api.PersonalConfigApi;
import io.mango.system.api.command.SavePersonalConfigCommand;
import io.mango.system.api.query.PersonalConfigQuery;
import io.mango.system.api.vo.PersonalConfigVO;
import io.mango.system.core.service.IPersonalConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/system/personal-configs")
@RequiredArgsConstructor
@Tag(name = "个人参数配置", description = "当前登录用户的个性化参数配置接口")
public class PersonalConfigController implements PersonalConfigApi {

    private final IPersonalConfigService personalConfigService;

    @Override
    @GetMapping
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "查询个人参数配置列表")
    @Operation(summary = "查询个人参数配置列表", description = "登录接口。按分组、业务类型和配置键查询当前用户个人配置")
    public R<List<PersonalConfigVO>> list(@Valid @ParameterObject PersonalConfigQuery query) {
        return R.ok(personalConfigService.listCurrentUser(query));
    }

    @Override
    @GetMapping("/value")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "查询个人参数配置")
    @Operation(summary = "查询个人参数配置", description = "登录接口。查询当前用户单个个人参数配置")
    public R<PersonalConfigVO> getValue(@Valid @ParameterObject PersonalConfigQuery query) {
        return R.ok(personalConfigService.getCurrentUserValue(query));
    }

    @Override
    @PostMapping
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "保存个人参数配置")
    @Operation(summary = "保存个人参数配置", description = "登录接口。按当前租户和当前用户保存个人参数配置")
    @Log("保存个人参数配置")
    public R<PersonalConfigVO> save(@RequestBody @Valid SavePersonalConfigCommand command) {
        return R.ok(personalConfigService.saveCurrentUser(command));
    }

    @Override
    @DeleteMapping
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "删除个人参数配置")
    @Operation(summary = "删除个人参数配置", description = "登录接口。删除当前用户单个个人参数配置")
    @Log("删除个人参数配置")
    public R<Boolean> delete(@Valid @ParameterObject PersonalConfigQuery query) {
        return R.ok(personalConfigService.deleteCurrentUser(query));
    }
}
