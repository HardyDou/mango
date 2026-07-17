package io.mango.cms.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.cms.api.CmsSiteAdminApi;
import io.mango.cms.api.command.SaveCmsSiteCommand;
import io.mango.cms.api.command.UpdateCmsStatusCommand;
import io.mango.cms.api.query.CmsSitePageQuery;
import io.mango.cms.api.vo.CmsSiteVO;
import io.mango.cms.core.service.ICmsSiteAdminService;
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
 * CMS 站点管理 HTTP 适配器。
 */
@Validated
@RestController
@RequestMapping("/cms")
@RequiredArgsConstructor
@Tag(name = "CMS 站点管理", description = "站点管理接口")
public class CmsSiteAdminController implements CmsSiteAdminApi {

    private final ICmsSiteAdminService siteAdminService;

    @Override
    @GetMapping("/sites/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:site:list")
    @Operation(summary = "分页查询站点", description = "分页查询站点")
    public R<PageResult<CmsSiteVO>> pageSites(@ParameterObject CmsSitePageQuery query) {
        return R.ok(siteAdminService.pageSites(query));
    }

    @Override
    @GetMapping("/sites/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:site:query")
    @Operation(summary = "查询站点详情", description = "查询站点详情")
    public R<CmsSiteVO> detailSite(@Parameter(description = "主键 ID") @RequestParam("id") Long id) {
        return R.ok(siteAdminService.detailSite(id));
    }

    @Override
    @PostMapping("/sites")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:site:add")
    @Operation(summary = "创建站点", description = "创建站点")
    public R<Long> createSite(@RequestBody SaveCmsSiteCommand command) {
        return R.ok(siteAdminService.createSite(command));
    }

    @Override
    @PutMapping("/sites")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:site:edit")
    @Operation(summary = "更新站点", description = "更新站点")
    public R<Boolean> updateSite(@RequestBody SaveCmsSiteCommand command) {
        return R.ok(siteAdminService.updateSite(command));
    }

    @Override
    @PutMapping("/sites/status")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:site:status")
    @Operation(summary = "更新站点状态", description = "更新站点状态")
    public R<Boolean> updateSiteStatus(@RequestBody UpdateCmsStatusCommand command) {
        return R.ok(siteAdminService.updateSiteStatus(command));
    }

    @Override
    @DeleteMapping("/sites")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:site:delete")
    @Operation(summary = "删除站点", description = "删除站点")
    public R<Boolean> deleteSite(@Parameter(description = "主键 ID") @RequestParam("id") Long id) {
        return R.ok(siteAdminService.deleteSite(id));
    }
}
