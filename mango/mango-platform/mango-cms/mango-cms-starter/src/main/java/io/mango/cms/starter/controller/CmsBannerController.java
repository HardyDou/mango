package io.mango.cms.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.cms.api.CmsBannerApi;
import io.mango.cms.api.command.SaveCmsBannerCommand;
import io.mango.cms.api.command.UpdateCmsStatusCommand;
import io.mango.cms.api.query.CmsBannerPageQuery;
import io.mango.cms.api.vo.CmsBannerVO;
import io.mango.cms.core.service.ICmsBannerService;
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
 * CMS Banner 管理 HTTP 适配器。
 */
@Validated
@RestController
@RequestMapping("/cms")
@RequiredArgsConstructor
@Tag(name = "CMS Banner 管理", description = "Banner 管理接口")
public class CmsBannerController implements CmsBannerApi {

    private final ICmsBannerService bannerService;

    @Override
    @GetMapping("/banners/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:banner:list")
    @Operation(summary = "分页查询Banner", description = "分页查询Banner")
    public R<PageResult<CmsBannerVO>> pageBanners(@ParameterObject CmsBannerPageQuery query) {
        return R.ok(bannerService.pageBanners(query));
    }

    @Override
    @GetMapping("/banners/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:banner:query")
    @Operation(summary = "查询Banner详情", description = "查询Banner详情")
    public R<CmsBannerVO> detailBanner(@Parameter(description = "主键 ID") @RequestParam("id") Long id) {
        return R.ok(bannerService.detailBanner(id));
    }

    @Override
    @PostMapping("/banners")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:banner:add")
    @Operation(summary = "创建Banner", description = "创建Banner")
    public R<Long> createBanner(@RequestBody SaveCmsBannerCommand command) {
        return R.ok(bannerService.createBanner(command));
    }

    @Override
    @PutMapping("/banners")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:banner:edit")
    @Operation(summary = "更新Banner", description = "更新Banner")
    public R<Boolean> updateBanner(@RequestBody SaveCmsBannerCommand command) {
        return R.ok(bannerService.updateBanner(command));
    }

    @Override
    @PutMapping("/banners/status")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:banner:status")
    @Operation(summary = "更新Banner状态", description = "更新Banner状态")
    public R<Boolean> updateBannerStatus(@RequestBody UpdateCmsStatusCommand command) {
        return R.ok(bannerService.updateBannerStatus(command));
    }

    @Override
    @DeleteMapping("/banners")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:banner:delete")
    @Operation(summary = "删除Banner", description = "删除Banner")
    public R<Boolean> deleteBanner(@Parameter(description = "主键 ID") @RequestParam("id") Long id) {
        return R.ok(bannerService.deleteBanner(id));
    }
}
