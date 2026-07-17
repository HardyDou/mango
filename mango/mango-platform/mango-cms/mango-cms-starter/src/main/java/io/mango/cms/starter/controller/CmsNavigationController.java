package io.mango.cms.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.cms.api.CmsNavigationApi;
import io.mango.cms.api.command.SaveCmsNavigationCommand;
import io.mango.cms.api.command.UpdateCmsStatusCommand;
import io.mango.cms.api.query.CmsNavigationPageQuery;
import io.mango.cms.api.vo.CmsNavigationVO;
import io.mango.cms.core.service.ICmsNavigationService;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
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

/**
 * CMS 导航管理 HTTP 适配器。
 */
@Validated
@RestController
@RequestMapping("/cms")
@RequiredArgsConstructor
@Tag(name = "CMS 导航管理", description = "导航管理接口")
public class CmsNavigationController implements CmsNavigationApi {

    private final ICmsNavigationService navigationService;

    @Override
    @GetMapping("/navigations/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:navigation:list")
    @Operation(summary = "分页查询导航", description = "分页查询导航")
    public R<PageResult<CmsNavigationVO>> pageNavigations(@ParameterObject CmsNavigationPageQuery query) {
        return R.ok(navigationService.pageNavigations(query));
    }

    @Override
    @GetMapping("/navigations/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:navigation:query")
    @Operation(summary = "查询导航详情", description = "查询导航详情")
    public R<CmsNavigationVO> detailNavigation(@Parameter(description = "主键 ID") @RequestParam("id") Long id) {
        return R.ok(navigationService.detailNavigation(id));
    }

    @Override
    @PostMapping("/navigations")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:navigation:add")
    @Operation(summary = "创建导航", description = "创建导航")
    public R<Long> createNavigation(@RequestBody SaveCmsNavigationCommand command) {
        return R.ok(navigationService.createNavigation(command));
    }

    @Override
    @PutMapping("/navigations")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:navigation:edit")
    @Operation(summary = "更新导航", description = "更新导航")
    public R<Boolean> updateNavigation(@RequestBody SaveCmsNavigationCommand command) {
        return R.ok(navigationService.updateNavigation(command));
    }

    @Override
    @PutMapping("/navigations/status")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:navigation:status")
    @Operation(summary = "更新导航状态", description = "更新导航状态")
    public R<Boolean> updateNavigationStatus(@RequestBody UpdateCmsStatusCommand command) {
        return R.ok(navigationService.updateNavigationStatus(command));
    }

    @Override
    @DeleteMapping("/navigations")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:navigation:delete")
    @Operation(summary = "删除导航", description = "删除导航")
    public R<Boolean> deleteNavigation(@Parameter(description = "主键 ID") @RequestParam("id") Long id) {
        return R.ok(navigationService.deleteNavigation(id));
    }
}
