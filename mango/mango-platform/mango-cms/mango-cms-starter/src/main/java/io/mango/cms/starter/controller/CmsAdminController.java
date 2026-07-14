package io.mango.cms.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.cms.api.CmsAdminApi;
import io.mango.cms.api.command.BatchCmsContentPublishCommand;
import io.mango.cms.api.command.CmsOfflineCommand;
import io.mango.cms.api.command.SaveCmsAdDeliveryCommand;
import io.mango.cms.api.command.SaveCmsAdvertisementCommand;
import io.mango.cms.api.command.SaveCmsBannerCommand;
import io.mango.cms.api.command.SaveCmsContentCategoryCommand;
import io.mango.cms.api.command.SaveCmsContentCommand;
import io.mango.cms.api.command.SaveCmsContentTagCommand;
import io.mango.cms.api.command.SaveCmsNavigationCommand;
import io.mango.cms.api.command.SaveCmsSiteCategoryCommand;
import io.mango.cms.api.command.SaveCmsSiteCommand;
import io.mango.cms.api.command.SaveCmsSiteSettingCommand;
import io.mango.cms.api.command.UpdateCmsContentReviewCommand;
import io.mango.cms.api.command.UpdateCmsStatusCommand;
import io.mango.cms.api.query.CmsAdDeliveryPageQuery;
import io.mango.cms.api.query.CmsAdvertisementPageQuery;
import io.mango.cms.api.query.CmsBannerPageQuery;
import io.mango.cms.api.query.CmsContentCategoryPageQuery;
import io.mango.cms.api.query.CmsContentPageQuery;
import io.mango.cms.api.query.CmsContentPublishPageQuery;
import io.mango.cms.api.query.CmsContentTagPageQuery;
import io.mango.cms.api.query.CmsNavigationPageQuery;
import io.mango.cms.api.query.CmsSiteCategoryTreeQuery;
import io.mango.cms.api.query.CmsSitePageQuery;
import io.mango.cms.api.vo.CmsAdDeliveryVO;
import io.mango.cms.api.vo.CmsAdvertisementVO;
import io.mango.cms.api.vo.CmsBannerVO;
import io.mango.cms.api.vo.CmsContentCategoryVO;
import io.mango.cms.api.vo.CmsContentPublishVO;
import io.mango.cms.api.vo.CmsContentTagVO;
import io.mango.cms.api.vo.CmsContentVO;
import io.mango.cms.api.vo.CmsNavigationVO;
import io.mango.cms.api.vo.CmsSiteCategoryVO;
import io.mango.cms.api.vo.CmsSiteSettingVO;
import io.mango.cms.api.vo.CmsSiteVO;
import io.mango.cms.core.service.ICmsAdDeliveryService;
import io.mango.cms.core.service.ICmsAdvertisementService;
import io.mango.cms.core.service.ICmsBannerService;
import io.mango.cms.core.service.ICmsContentCategoryService;
import io.mango.cms.core.service.ICmsContentPublishService;
import io.mango.cms.core.service.ICmsContentService;
import io.mango.cms.core.service.ICmsContentTagService;
import io.mango.cms.core.service.ICmsNavigationService;
import io.mango.cms.core.service.ICmsSiteAdminService;
import io.mango.cms.core.service.ICmsSiteCategoryService;
import io.mango.cms.core.service.ICmsSiteSettingService;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

import java.util.List;

@Validated
@RestController
@RequestMapping("/cms")
@RequiredArgsConstructor
@Tag(name = "CMS 管理", description = "内容与站点管理后台接口")
public class CmsAdminController implements CmsAdminApi {

    private final ICmsContentCategoryService contentCategoryService;
    private final ICmsContentTagService contentTagService;
    private final ICmsSiteAdminService siteAdminService;
    private final ICmsSiteCategoryService siteCategoryService;
    private final ICmsContentService contentService;
    private final ICmsContentPublishService contentPublishService;
    private final ICmsNavigationService navigationService;
    private final ICmsBannerService bannerService;
    private final ICmsAdvertisementService advertisementService;
    private final ICmsAdDeliveryService adDeliveryService;
    private final ICmsSiteSettingService siteSettingService;

    @Override
    @GetMapping("/content-categories/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content-category:list")
    @Operation(summary = "分页查询内容分类", description = "分页查询内容分类")
    public R<PageResult<CmsContentCategoryVO>> pageContentCategories(@ParameterObject CmsContentCategoryPageQuery query) {
        return R.ok(contentCategoryService.pageContentCategories(query));
    }

    @Override
    @GetMapping("/content-categories/list")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content-category:list")
    @Operation(summary = "查询内容分类列表", description = "查询内容分类列表")
    public R<List<CmsContentCategoryVO>> listContentCategories(@ParameterObject CmsContentCategoryPageQuery query) {
        return R.ok(contentCategoryService.listContentCategories(query));
    }

    @Override
    @GetMapping("/content-categories/tree")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content-category:list")
    @Operation(summary = "查询内容分类树", description = "查询内容分类树")
    public R<List<CmsContentCategoryVO>> treeContentCategories(@ParameterObject CmsContentCategoryPageQuery query) {
        return R.ok(contentCategoryService.treeContentCategories(query));
    }

    @Override
    @GetMapping("/content-categories/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content-category:query")
    @Operation(summary = "查询内容分类详情", description = "查询内容分类详情")
    public R<CmsContentCategoryVO> detailContentCategory(@Parameter(description = "分类 ID") @RequestParam("id") Long id) {
        return R.ok(contentCategoryService.detailContentCategory(id));
    }

    @Override
    @PostMapping("/content-categories")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content-category:add")
    @Operation(summary = "创建内容分类", description = "创建内容分类")
    public R<Long> createContentCategory(@Valid @RequestBody SaveCmsContentCategoryCommand command) {
        return R.ok(contentCategoryService.createContentCategory(command));
    }

    @Override
    @PutMapping("/content-categories")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content-category:edit")
    @Operation(summary = "更新内容分类", description = "更新内容分类")
    public R<Boolean> updateContentCategory(@Valid @RequestBody SaveCmsContentCategoryCommand command) {
        return R.ok(contentCategoryService.updateContentCategory(command));
    }

    @Override
    @PutMapping("/content-categories/status")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content-category:status")
    @Operation(summary = "更新内容分类状态", description = "更新内容分类状态")
    public R<Boolean> updateContentCategoryStatus(@Valid @RequestBody UpdateCmsStatusCommand command) {
        return R.ok(contentCategoryService.updateContentCategoryStatus(command));
    }

    @Override
    @DeleteMapping("/content-categories")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content-category:delete")
    @Operation(summary = "删除内容分类", description = "删除内容分类")
    public R<Boolean> deleteContentCategory(@Parameter(description = "主键 ID") @RequestParam("id") Long id) {
        return R.ok(contentCategoryService.deleteContentCategory(id));
    }

    @Override
    @GetMapping("/content-tags/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content-tag:list")
    @Operation(summary = "分页查询内容标签", description = "分页查询内容标签")
    public R<PageResult<CmsContentTagVO>> pageContentTags(@ParameterObject CmsContentTagPageQuery query) {
        return R.ok(contentTagService.pageContentTags(query));
    }

    @Override
    @GetMapping("/content-tags/list")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content-tag:list")
    @Operation(summary = "查询内容标签列表", description = "查询内容标签列表")
    public R<List<CmsContentTagVO>> listContentTags(@ParameterObject CmsContentTagPageQuery query) {
        return R.ok(contentTagService.listContentTags(query));
    }

    @Override
    @GetMapping("/content-tags/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content-tag:query")
    @Operation(summary = "查询内容标签详情", description = "查询内容标签详情")
    public R<CmsContentTagVO> detailContentTag(@Parameter(description = "主键 ID") @RequestParam("id") Long id) {
        return R.ok(contentTagService.detailContentTag(id));
    }

    @Override
    @PostMapping("/content-tags")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content-tag:add")
    @Operation(summary = "创建内容标签", description = "创建内容标签")
    public R<Long> createContentTag(@Valid @RequestBody SaveCmsContentTagCommand command) {
        return R.ok(contentTagService.createContentTag(command));
    }

    @Override
    @PutMapping("/content-tags")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content-tag:edit")
    @Operation(summary = "更新内容标签", description = "更新内容标签")
    public R<Boolean> updateContentTag(@Valid @RequestBody SaveCmsContentTagCommand command) {
        return R.ok(contentTagService.updateContentTag(command));
    }

    @Override
    @PutMapping("/content-tags/status")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content-tag:status")
    @Operation(summary = "更新内容标签状态", description = "更新内容标签状态")
    public R<Boolean> updateContentTagStatus(@Valid @RequestBody UpdateCmsStatusCommand command) {
        return R.ok(contentTagService.updateContentTagStatus(command));
    }

    @Override
    @DeleteMapping("/content-tags")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content-tag:delete")
    @Operation(summary = "删除内容标签", description = "删除内容标签")
    public R<Boolean> deleteContentTag(@Parameter(description = "主键 ID") @RequestParam("id") Long id) {
        return R.ok(contentTagService.deleteContentTag(id));
    }

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
    public R<Long> createSite(@Valid @RequestBody SaveCmsSiteCommand command) {
        return R.ok(siteAdminService.createSite(command));
    }

    @Override
    @PutMapping("/sites")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:site:edit")
    @Operation(summary = "更新站点", description = "更新站点")
    public R<Boolean> updateSite(@Valid @RequestBody SaveCmsSiteCommand command) {
        return R.ok(siteAdminService.updateSite(command));
    }

    @Override
    @PutMapping("/sites/status")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:site:status")
    @Operation(summary = "更新站点状态", description = "更新站点状态")
    public R<Boolean> updateSiteStatus(@Valid @RequestBody UpdateCmsStatusCommand command) {
        return R.ok(siteAdminService.updateSiteStatus(command));
    }

    @Override
    @DeleteMapping("/sites")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:site:delete")
    @Operation(summary = "删除站点", description = "删除站点")
    public R<Boolean> deleteSite(@Parameter(description = "主键 ID") @RequestParam("id") Long id) {
        return R.ok(siteAdminService.deleteSite(id));
    }

    @Override
    @GetMapping("/site-categories/tree")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:site-category:list")
    @Operation(summary = "查询站点栏目树", description = "查询站点栏目树")
    public R<List<CmsSiteCategoryVO>> treeSiteCategories(@ParameterObject CmsSiteCategoryTreeQuery query) {
        return R.ok(siteCategoryService.treeSiteCategories(query));
    }

    @Override
    @GetMapping("/site-categories/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:site-category:query")
    @Operation(summary = "查询站点栏目详情", description = "查询站点栏目详情")
    public R<CmsSiteCategoryVO> detailSiteCategory(@Parameter(description = "主键 ID") @RequestParam("id") Long id) {
        return R.ok(siteCategoryService.detailSiteCategory(id));
    }

    @Override
    @PostMapping("/site-categories")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:site-category:add")
    @Operation(summary = "创建站点栏目", description = "创建站点栏目")
    public R<Long> createSiteCategory(@Valid @RequestBody SaveCmsSiteCategoryCommand command) {
        return R.ok(siteCategoryService.createSiteCategory(command));
    }

    @Override
    @PutMapping("/site-categories")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:site-category:edit")
    @Operation(summary = "更新站点栏目", description = "更新站点栏目")
    public R<Boolean> updateSiteCategory(@Valid @RequestBody SaveCmsSiteCategoryCommand command) {
        return R.ok(siteCategoryService.updateSiteCategory(command));
    }

    @Override
    @PutMapping("/site-categories/status")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:site-category:status")
    @Operation(summary = "更新站点栏目状态", description = "更新站点栏目状态")
    public R<Boolean> updateSiteCategoryStatus(@Valid @RequestBody UpdateCmsStatusCommand command) {
        return R.ok(siteCategoryService.updateSiteCategoryStatus(command));
    }

    @Override
    @DeleteMapping("/site-categories")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:site-category:delete")
    @Operation(summary = "删除站点栏目", description = "删除站点栏目")
    public R<Boolean> deleteSiteCategory(@Parameter(description = "主键 ID") @RequestParam("id") Long id) {
        return R.ok(siteCategoryService.deleteSiteCategory(id));
    }

    @Override
    @GetMapping("/contents/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content:list")
    @Operation(summary = "分页查询内容", description = "分页查询内容")
    public R<PageResult<CmsContentVO>> pageContents(@ParameterObject CmsContentPageQuery query) {
        return R.ok(contentService.pageContents(query));
    }

    @Override
    @GetMapping("/contents/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content:query")
    @Operation(summary = "查询内容详情", description = "查询内容详情")
    public R<CmsContentVO> detailContent(@Parameter(description = "主键 ID") @RequestParam("id") Long id) {
        return R.ok(contentService.detailContent(id));
    }

    @Override
    @PostMapping("/contents")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content:add")
    @Operation(summary = "创建内容", description = "创建内容")
    public R<Long> createContent(@Valid @RequestBody SaveCmsContentCommand command) {
        return R.ok(contentService.createContent(command));
    }

    @Override
    @PutMapping("/contents")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content:edit")
    @Operation(summary = "更新内容", description = "更新内容")
    public R<Boolean> updateContent(@Valid @RequestBody SaveCmsContentCommand command) {
        return R.ok(contentService.updateContent(command));
    }

    @Override
    @PostMapping("/contents/submit")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content:submit")
    @Operation(summary = "提交内容审核", description = "提交内容审核")
    public R<Boolean> submitContent(@Valid @RequestBody CmsOfflineCommand command) {
        return R.ok(contentService.submitContent(command));
    }

    @Override
    @PostMapping("/contents/approve")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content:approve")
    @Operation(summary = "审核通过内容", description = "审核通过内容")
    public R<Boolean> approveContent(@Valid @RequestBody UpdateCmsContentReviewCommand command) {
        return R.ok(contentService.approveContent(command));
    }

    @Override
    @PostMapping("/contents/reject")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content:reject")
    @Operation(summary = "驳回内容", description = "驳回内容")
    public R<Boolean> rejectContent(@Valid @RequestBody UpdateCmsContentReviewCommand command) {
        return R.ok(contentService.rejectContent(command));
    }

    @Override
    @PostMapping("/contents/offline")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content:offline")
    @Operation(summary = "下线内容", description = "下线内容")
    public R<Boolean> offlineContent(@Valid @RequestBody CmsOfflineCommand command) {
        return R.ok(contentService.offlineContent(command));
    }

    @Override
    @DeleteMapping("/contents")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content:delete")
    @Operation(summary = "删除内容", description = "删除内容")
    public R<Boolean> deleteContent(@Parameter(description = "主键 ID") @RequestParam("id") Long id) {
        return R.ok(contentService.deleteContent(id));
    }

    @Override
    @GetMapping("/content-publishes/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:publish:list")
    @Operation(summary = "分页查询发布关系", description = "分页查询发布关系")
    public R<PageResult<CmsContentPublishVO>> pagePublishes(@ParameterObject CmsContentPublishPageQuery query) {
        return R.ok(contentPublishService.pagePublishes(query));
    }

    @Override
    @PostMapping("/content-publishes/publish")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:publish:publish")
    @Operation(summary = "发布内容", description = "发布内容")
    public R<Boolean> publishContents(@Valid @RequestBody BatchCmsContentPublishCommand command) {
        return R.ok(contentPublishService.publishContents(command));
    }

    @Override
    @PostMapping("/content-publishes/offline")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:publish:offline")
    @Operation(summary = "下线发布关系", description = "下线发布关系")
    public R<Boolean> offlinePublish(@Valid @RequestBody CmsOfflineCommand command) {
        return R.ok(contentPublishService.offlinePublish(command));
    }

    @Override
    @DeleteMapping("/content-publishes")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:publish:delete")
    @Operation(summary = "删除发布关系", description = "删除发布关系")
    public R<Boolean> deletePublish(@Parameter(description = "主键 ID") @RequestParam("id") Long id) {
        return R.ok(contentPublishService.deletePublish(id));
    }

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
    public R<Long> createNavigation(@Valid @RequestBody SaveCmsNavigationCommand command) {
        return R.ok(navigationService.createNavigation(command));
    }

    @Override
    @PutMapping("/navigations")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:navigation:edit")
    @Operation(summary = "更新导航", description = "更新导航")
    public R<Boolean> updateNavigation(@Valid @RequestBody SaveCmsNavigationCommand command) {
        return R.ok(navigationService.updateNavigation(command));
    }

    @Override
    @PutMapping("/navigations/status")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:navigation:status")
    @Operation(summary = "更新导航状态", description = "更新导航状态")
    public R<Boolean> updateNavigationStatus(@Valid @RequestBody UpdateCmsStatusCommand command) {
        return R.ok(navigationService.updateNavigationStatus(command));
    }

    @Override
    @DeleteMapping("/navigations")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:navigation:delete")
    @Operation(summary = "删除导航", description = "删除导航")
    public R<Boolean> deleteNavigation(@Parameter(description = "主键 ID") @RequestParam("id") Long id) {
        return R.ok(navigationService.deleteNavigation(id));
    }

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
    public R<Long> createBanner(@Valid @RequestBody SaveCmsBannerCommand command) {
        return R.ok(bannerService.createBanner(command));
    }

    @Override
    @PutMapping("/banners")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:banner:edit")
    @Operation(summary = "更新Banner", description = "更新Banner")
    public R<Boolean> updateBanner(@Valid @RequestBody SaveCmsBannerCommand command) {
        return R.ok(bannerService.updateBanner(command));
    }

    @Override
    @PutMapping("/banners/status")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:banner:status")
    @Operation(summary = "更新Banner状态", description = "更新Banner状态")
    public R<Boolean> updateBannerStatus(@Valid @RequestBody UpdateCmsStatusCommand command) {
        return R.ok(bannerService.updateBannerStatus(command));
    }

    @Override
    @DeleteMapping("/banners")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:banner:delete")
    @Operation(summary = "删除Banner", description = "删除Banner")
    public R<Boolean> deleteBanner(@Parameter(description = "主键 ID") @RequestParam("id") Long id) {
        return R.ok(bannerService.deleteBanner(id));
    }

    @Override
    @GetMapping("/advertisements/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:advertisement:list")
    @Operation(summary = "分页查询广告位", description = "分页查询广告位")
    public R<PageResult<CmsAdvertisementVO>> pageAdvertisements(@ParameterObject CmsAdvertisementPageQuery query) {
        return R.ok(advertisementService.pageAdvertisements(query));
    }

    @Override
    @GetMapping("/advertisements/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:advertisement:query")
    @Operation(summary = "查询广告位详情", description = "查询广告位详情")
    public R<CmsAdvertisementVO> detailAdvertisement(@Parameter(description = "主键 ID") @RequestParam("id") Long id) {
        return R.ok(advertisementService.detailAdvertisement(id));
    }

    @Override
    @PostMapping("/advertisements")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:advertisement:add")
    @Operation(summary = "创建广告位", description = "创建广告位")
    public R<Long> createAdvertisement(@Valid @RequestBody SaveCmsAdvertisementCommand command) {
        return R.ok(advertisementService.createAdvertisement(command));
    }

    @Override
    @PutMapping("/advertisements")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:advertisement:edit")
    @Operation(summary = "更新广告位", description = "更新广告位")
    public R<Boolean> updateAdvertisement(@Valid @RequestBody SaveCmsAdvertisementCommand command) {
        return R.ok(advertisementService.updateAdvertisement(command));
    }

    @Override
    @PutMapping("/advertisements/status")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:advertisement:status")
    @Operation(summary = "更新广告位状态", description = "更新广告位状态")
    public R<Boolean> updateAdvertisementStatus(@Valid @RequestBody UpdateCmsStatusCommand command) {
        return R.ok(advertisementService.updateAdvertisementStatus(command));
    }

    @Override
    @DeleteMapping("/advertisements")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:advertisement:delete")
    @Operation(summary = "删除广告位", description = "删除广告位")
    public R<Boolean> deleteAdvertisement(@Parameter(description = "主键 ID") @RequestParam("id") Long id) {
        return R.ok(advertisementService.deleteAdvertisement(id));
    }

    @Override
    @GetMapping("/ad-deliveries/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:ad-delivery:list")
    @Operation(summary = "分页查询广告投放", description = "分页查询广告投放")
    public R<PageResult<CmsAdDeliveryVO>> pageAdDeliveries(@ParameterObject CmsAdDeliveryPageQuery query) {
        return R.ok(adDeliveryService.pageAdDeliveries(query));
    }

    @Override
    @GetMapping("/ad-deliveries/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:ad-delivery:query")
    @Operation(summary = "查询广告投放详情", description = "查询广告投放详情")
    public R<CmsAdDeliveryVO> detailAdDelivery(@Parameter(description = "主键 ID") @RequestParam("id") Long id) {
        return R.ok(adDeliveryService.detailAdDelivery(id));
    }

    @Override
    @PostMapping("/ad-deliveries")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:ad-delivery:add")
    @Operation(summary = "创建广告投放", description = "创建广告投放")
    public R<Long> createAdDelivery(@Valid @RequestBody SaveCmsAdDeliveryCommand command) {
        return R.ok(adDeliveryService.createAdDelivery(command));
    }

    @Override
    @PutMapping("/ad-deliveries")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:ad-delivery:edit")
    @Operation(summary = "更新广告投放", description = "更新广告投放")
    public R<Boolean> updateAdDelivery(@Valid @RequestBody SaveCmsAdDeliveryCommand command) {
        return R.ok(adDeliveryService.updateAdDelivery(command));
    }

    @Override
    @PutMapping("/ad-deliveries/status")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:ad-delivery:status")
    @Operation(summary = "更新广告投放状态", description = "更新广告投放状态")
    public R<Boolean> updateAdDeliveryStatus(@Valid @RequestBody UpdateCmsStatusCommand command) {
        return R.ok(adDeliveryService.updateAdDeliveryStatus(command));
    }

    @Override
    @DeleteMapping("/ad-deliveries")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:ad-delivery:delete")
    @Operation(summary = "删除广告投放", description = "删除广告投放")
    public R<Boolean> deleteAdDelivery(@Parameter(description = "主键 ID") @RequestParam("id") Long id) {
        return R.ok(adDeliveryService.deleteAdDelivery(id));
    }

    @Override
    @GetMapping("/site-settings/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:site-setting:query")
    @Operation(summary = "查询站点设置详情", description = "查询站点设置详情")
    public R<CmsSiteSettingVO> detailSiteSetting(@Parameter(description = "站点 ID") @RequestParam("siteId") Long siteId) {
        return R.ok(siteSettingService.detailSiteSetting(siteId));
    }

    @Override
    @PutMapping("/site-settings")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:site-setting:edit")
    @Operation(summary = "保存站点设置", description = "保存站点设置")
    public R<Boolean> saveSiteSetting(@Valid @RequestBody SaveCmsSiteSettingCommand command) {
        return R.ok(siteSettingService.saveSiteSetting(command));
    }
}
