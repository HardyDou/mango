package io.mango.home.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.home.api.HomeOptionApi;
import io.mango.home.api.query.HomeUserOptionQuery;
import io.mango.home.api.vo.HomeUserOptionVO;
import io.mango.home.core.service.IHomeOptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 首页管理页面窄候选项接口。 */
@Validated
@RestController
@RequestMapping("/home/options")
@RequiredArgsConstructor
@Tag(name = "首页管理候选项", description = "首页管理页面按自身权限读取当前租户最小候选数据")
public class HomeOptionController implements HomeOptionApi {

    private final IHomeOptionService homeOptionService;

    @Override
    @GetMapping("/page-users")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "home:list:view")
    @Operation(summary = "查询首页列表用户候选项", description = "权限接口。查询当前租户启用成员的最小候选字段")
    public R<List<HomeUserOptionVO>> listPageUserOptions(@ParameterObject HomeUserOptionQuery query) {
        return R.ok(homeOptionService.listPageUserOptions(query));
    }

    @Override
    @GetMapping("/visible-users")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "home:user:view")
    @Operation(summary = "查询用户首页候选项", description = "权限接口。按关键字查询当前租户启用成员的最小候选字段")
    public R<List<HomeUserOptionVO>> listVisibleUserOptions(@ParameterObject HomeUserOptionQuery query) {
        return R.ok(homeOptionService.listVisibleUserOptions(query));
    }
}
