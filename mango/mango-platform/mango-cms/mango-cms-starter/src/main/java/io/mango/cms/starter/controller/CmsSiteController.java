package io.mango.cms.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.cms.api.CmsSiteApi;
import io.mango.cms.api.query.SiteAdvertisementQuery;
import io.mango.cms.api.query.SiteBannerQuery;
import io.mango.cms.api.query.SiteCategoryQuery;
import io.mango.cms.api.query.SiteContentDetailQuery;
import io.mango.cms.api.query.SiteContentPageQuery;
import io.mango.cms.api.query.SiteNavigationQuery;
import io.mango.cms.api.query.SiteResolveQuery;
import io.mango.cms.api.vo.SiteAdvertisementVO;
import io.mango.cms.api.vo.SiteBannerVO;
import io.mango.cms.api.vo.SiteCategoryVO;
import io.mango.cms.api.vo.SiteContentVO;
import io.mango.cms.api.vo.SiteNavigationVO;
import io.mango.cms.api.vo.SiteResolveVO;
import io.mango.cms.api.vo.SiteVO;
import io.mango.cms.core.service.ICmsSiteService;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@Validated
@RestController
@RequestMapping("/cms/open")
@RequiredArgsConstructor
@ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "CMS 站点消费公共接口")
@Tag(name = "CMS 站点消费", description = "官网、帮助中心和门户站点只读接口")
public class CmsSiteController implements CmsSiteApi {

    private final ICmsSiteService service;

    @Override
    @GetMapping("/sites/resolve")
    @Operation(summary = "解析站点", description = "解析站点")
    public R<SiteResolveVO> resolveSite(@ParameterObject SiteResolveQuery query) {
        return R.ok(service.resolveSite(query));
    }

    @Override
    @GetMapping("/sites/detail")
    @Operation(summary = "查询站点信息", description = "查询站点信息")
    public R<SiteVO> detailSite(@ParameterObject SiteResolveQuery query) {
        return R.ok(service.detailSite(query));
    }

    @Override
    @GetMapping("/site-categories/tree")
    @Operation(summary = "查询公开栏目树", description = "查询公开栏目树")
    public R<List<SiteCategoryVO>> treeCategories(@ParameterObject SiteCategoryQuery query) {
        return R.ok(service.treeCategories(query));
    }

    @Override
    @GetMapping("/navigations/list")
    @Operation(summary = "查询公开导航", description = "查询公开导航")
    public R<List<SiteNavigationVO>> listNavigations(@ParameterObject SiteNavigationQuery query) {
        return R.ok(service.listNavigations(query));
    }

    @Override
    @GetMapping("/banners/list")
    @Operation(summary = "查询有效 Banner", description = "查询有效 Banner")
    public R<List<SiteBannerVO>> listBanners(@ParameterObject SiteBannerQuery query) {
        return R.ok(service.listBanners(query));
    }

    @Override
    @GetMapping("/advertisements/list")
    @Operation(summary = "查询有效广告", description = "查询有效广告")
    public R<List<SiteAdvertisementVO>> listAdvertisements(@ParameterObject SiteAdvertisementQuery query) {
        return R.ok(service.listAdvertisements(query));
    }

    @Override
    @GetMapping("/contents/page")
    @Operation(summary = "分页查询已发布内容", description = "分页查询已发布内容")
    public R<PageResult<SiteContentVO>> pageContents(@ParameterObject SiteContentPageQuery query) {
        return R.ok(service.pageContents(query));
    }

    @Override
    @GetMapping("/contents/detail")
    @Operation(summary = "查询已发布内容详情", description = "查询已发布内容详情")
    public R<SiteContentVO> detailContent(@ParameterObject SiteContentDetailQuery query) {
        return R.ok(service.detailContent(query));
    }

}
