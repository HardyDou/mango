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
import io.mango.cms.core.service.ICmsAdminService;
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

import java.util.List;

@Validated
@RestController
@RequestMapping("/cms")
@RequiredArgsConstructor
@Tag(name = "CMS 管理", description = "内容与站点管理后台接口")
public class CmsAdminController implements CmsAdminApi {

    private final ICmsAdminService service;

    @Override
    @GetMapping("/content-categories/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content-category:list")
    @Operation(summary = "分页查询内容分类")
    public R<PageResult<CmsContentCategoryVO>> pageContentCategories(@ParameterObject CmsContentCategoryPageQuery query) {
        return service.pageContentCategories(query);
    }

    @Override
    @GetMapping("/content-categories/list")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content-category:list")
    @Operation(summary = "查询内容分类列表")
    public R<List<CmsContentCategoryVO>> listContentCategories(@ParameterObject CmsContentCategoryPageQuery query) {
        return service.listContentCategories(query);
    }

    @Override
    @GetMapping("/content-categories/tree")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content-category:list")
    @Operation(summary = "查询内容分类树")
    public R<List<CmsContentCategoryVO>> treeContentCategories(@ParameterObject CmsContentCategoryPageQuery query) {
        return service.treeContentCategories(query);
    }

    @Override
    @GetMapping("/content-categories/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content-category:query")
    @Operation(summary = "查询内容分类详情")
    public R<CmsContentCategoryVO> detailContentCategory(@Parameter(description = "分类 ID") @RequestParam Long id) {
        return service.detailContentCategory(id);
    }

    @Override
    @PostMapping("/content-categories")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content-category:add")
    public R<Long> createContentCategory(@RequestBody SaveCmsContentCategoryCommand command) {
        return service.createContentCategory(command);
    }

    @Override
    @PutMapping("/content-categories")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content-category:edit")
    public R<Boolean> updateContentCategory(@RequestBody SaveCmsContentCategoryCommand command) {
        return service.updateContentCategory(command);
    }

    @Override
    @PutMapping("/content-categories/status")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content-category:status")
    public R<Boolean> updateContentCategoryStatus(@RequestBody UpdateCmsStatusCommand command) {
        return service.updateContentCategoryStatus(command);
    }

    @Override
    @DeleteMapping("/content-categories")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content-category:delete")
    public R<Boolean> deleteContentCategory(@RequestParam Long id) {
        return service.deleteContentCategory(id);
    }

    @Override
    @GetMapping("/content-tags/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content-tag:list")
    public R<PageResult<CmsContentTagVO>> pageContentTags(@ParameterObject CmsContentTagPageQuery query) {
        return service.pageContentTags(query);
    }

    @Override
    @GetMapping("/content-tags/list")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content-tag:list")
    public R<List<CmsContentTagVO>> listContentTags(@ParameterObject CmsContentTagPageQuery query) {
        return service.listContentTags(query);
    }

    @Override
    @GetMapping("/content-tags/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content-tag:query")
    public R<CmsContentTagVO> detailContentTag(@RequestParam Long id) {
        return service.detailContentTag(id);
    }

    @Override
    @PostMapping("/content-tags")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content-tag:add")
    public R<Long> createContentTag(@RequestBody SaveCmsContentTagCommand command) {
        return service.createContentTag(command);
    }

    @Override
    @PutMapping("/content-tags")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content-tag:edit")
    public R<Boolean> updateContentTag(@RequestBody SaveCmsContentTagCommand command) {
        return service.updateContentTag(command);
    }

    @Override
    @PutMapping("/content-tags/status")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content-tag:status")
    public R<Boolean> updateContentTagStatus(@RequestBody UpdateCmsStatusCommand command) {
        return service.updateContentTagStatus(command);
    }

    @Override
    @DeleteMapping("/content-tags")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content-tag:delete")
    public R<Boolean> deleteContentTag(@RequestParam Long id) {
        return service.deleteContentTag(id);
    }

    @Override
    @GetMapping("/sites/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:site:list")
    public R<PageResult<CmsSiteVO>> pageSites(@ParameterObject CmsSitePageQuery query) {
        return service.pageSites(query);
    }

    @Override
    @GetMapping("/sites/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:site:query")
    public R<CmsSiteVO> detailSite(@RequestParam Long id) {
        return service.detailSite(id);
    }

    @Override
    @PostMapping("/sites")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:site:add")
    public R<Long> createSite(@RequestBody SaveCmsSiteCommand command) {
        return service.createSite(command);
    }

    @Override
    @PutMapping("/sites")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:site:edit")
    public R<Boolean> updateSite(@RequestBody SaveCmsSiteCommand command) {
        return service.updateSite(command);
    }

    @Override
    @PutMapping("/sites/status")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:site:status")
    public R<Boolean> updateSiteStatus(@RequestBody UpdateCmsStatusCommand command) {
        return service.updateSiteStatus(command);
    }

    @Override
    @DeleteMapping("/sites")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:site:delete")
    public R<Boolean> deleteSite(@RequestParam Long id) {
        return service.deleteSite(id);
    }

    @Override
    @GetMapping("/site-categories/tree")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:site-category:list")
    public R<List<CmsSiteCategoryVO>> treeSiteCategories(@ParameterObject CmsSiteCategoryTreeQuery query) {
        return service.treeSiteCategories(query);
    }

    @Override
    @GetMapping("/site-categories/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:site-category:query")
    public R<CmsSiteCategoryVO> detailSiteCategory(@RequestParam Long id) {
        return service.detailSiteCategory(id);
    }

    @Override
    @PostMapping("/site-categories")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:site-category:add")
    public R<Long> createSiteCategory(@RequestBody SaveCmsSiteCategoryCommand command) {
        return service.createSiteCategory(command);
    }

    @Override
    @PutMapping("/site-categories")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:site-category:edit")
    public R<Boolean> updateSiteCategory(@RequestBody SaveCmsSiteCategoryCommand command) {
        return service.updateSiteCategory(command);
    }

    @Override
    @PutMapping("/site-categories/status")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:site-category:status")
    public R<Boolean> updateSiteCategoryStatus(@RequestBody UpdateCmsStatusCommand command) {
        return service.updateSiteCategoryStatus(command);
    }

    @Override
    @DeleteMapping("/site-categories")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:site-category:delete")
    public R<Boolean> deleteSiteCategory(@RequestParam Long id) {
        return service.deleteSiteCategory(id);
    }

    @Override
    @GetMapping("/contents/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content:list")
    public R<PageResult<CmsContentVO>> pageContents(@ParameterObject CmsContentPageQuery query) {
        return service.pageContents(query);
    }

    @Override
    @GetMapping("/contents/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content:query")
    public R<CmsContentVO> detailContent(@RequestParam Long id) {
        return service.detailContent(id);
    }

    @Override
    @PostMapping("/contents")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content:add")
    public R<Long> createContent(@RequestBody SaveCmsContentCommand command) {
        return service.createContent(command);
    }

    @Override
    @PutMapping("/contents")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content:edit")
    public R<Boolean> updateContent(@RequestBody SaveCmsContentCommand command) {
        return service.updateContent(command);
    }

    @Override
    @PostMapping("/contents/submit")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content:submit")
    public R<Boolean> submitContent(@RequestBody CmsOfflineCommand command) {
        return service.submitContent(command);
    }

    @Override
    @PostMapping("/contents/approve")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content:approve")
    public R<Boolean> approveContent(@RequestBody UpdateCmsContentReviewCommand command) {
        return service.approveContent(command);
    }

    @Override
    @PostMapping("/contents/reject")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content:reject")
    public R<Boolean> rejectContent(@RequestBody UpdateCmsContentReviewCommand command) {
        return service.rejectContent(command);
    }

    @Override
    @PostMapping("/contents/offline")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content:offline")
    public R<Boolean> offlineContent(@RequestBody CmsOfflineCommand command) {
        return service.offlineContent(command);
    }

    @Override
    @DeleteMapping("/contents")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content:delete")
    public R<Boolean> deleteContent(@RequestParam Long id) {
        return service.deleteContent(id);
    }

    @Override
    @GetMapping("/content-publishes/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:publish:list")
    public R<PageResult<CmsContentPublishVO>> pagePublishes(@ParameterObject CmsContentPublishPageQuery query) {
        return service.pagePublishes(query);
    }

    @Override
    @PostMapping("/content-publishes/publish")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:publish:publish")
    public R<Boolean> publishContents(@RequestBody BatchCmsContentPublishCommand command) {
        return service.publishContents(command);
    }

    @Override
    @PostMapping("/content-publishes/offline")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:publish:offline")
    public R<Boolean> offlinePublish(@RequestBody CmsOfflineCommand command) {
        return service.offlinePublish(command);
    }

    @Override
    @DeleteMapping("/content-publishes")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:publish:delete")
    public R<Boolean> deletePublish(@RequestParam Long id) {
        return service.deletePublish(id);
    }

    @Override
    @GetMapping("/navigations/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:navigation:list")
    public R<PageResult<CmsNavigationVO>> pageNavigations(@ParameterObject CmsNavigationPageQuery query) {
        return service.pageNavigations(query);
    }

    @Override
    @GetMapping("/navigations/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:navigation:query")
    public R<CmsNavigationVO> detailNavigation(@RequestParam Long id) {
        return service.detailNavigation(id);
    }

    @Override
    @PostMapping("/navigations")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:navigation:add")
    public R<Long> createNavigation(@RequestBody SaveCmsNavigationCommand command) {
        return service.createNavigation(command);
    }

    @Override
    @PutMapping("/navigations")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:navigation:edit")
    public R<Boolean> updateNavigation(@RequestBody SaveCmsNavigationCommand command) {
        return service.updateNavigation(command);
    }

    @Override
    @PutMapping("/navigations/status")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:navigation:status")
    public R<Boolean> updateNavigationStatus(@RequestBody UpdateCmsStatusCommand command) {
        return service.updateNavigationStatus(command);
    }

    @Override
    @DeleteMapping("/navigations")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:navigation:delete")
    public R<Boolean> deleteNavigation(@RequestParam Long id) {
        return service.deleteNavigation(id);
    }

    @Override
    @GetMapping("/banners/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:banner:list")
    public R<PageResult<CmsBannerVO>> pageBanners(@ParameterObject CmsBannerPageQuery query) {
        return service.pageBanners(query);
    }

    @Override
    @GetMapping("/banners/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:banner:query")
    public R<CmsBannerVO> detailBanner(@RequestParam Long id) {
        return service.detailBanner(id);
    }

    @Override
    @PostMapping("/banners")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:banner:add")
    public R<Long> createBanner(@RequestBody SaveCmsBannerCommand command) {
        return service.createBanner(command);
    }

    @Override
    @PutMapping("/banners")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:banner:edit")
    public R<Boolean> updateBanner(@RequestBody SaveCmsBannerCommand command) {
        return service.updateBanner(command);
    }

    @Override
    @PutMapping("/banners/status")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:banner:status")
    public R<Boolean> updateBannerStatus(@RequestBody UpdateCmsStatusCommand command) {
        return service.updateBannerStatus(command);
    }

    @Override
    @DeleteMapping("/banners")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:banner:delete")
    public R<Boolean> deleteBanner(@RequestParam Long id) {
        return service.deleteBanner(id);
    }

    @Override
    @GetMapping("/advertisements/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:advertisement:list")
    public R<PageResult<CmsAdvertisementVO>> pageAdvertisements(@ParameterObject CmsAdvertisementPageQuery query) {
        return service.pageAdvertisements(query);
    }

    @Override
    @GetMapping("/advertisements/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:advertisement:query")
    public R<CmsAdvertisementVO> detailAdvertisement(@RequestParam Long id) {
        return service.detailAdvertisement(id);
    }

    @Override
    @PostMapping("/advertisements")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:advertisement:add")
    public R<Long> createAdvertisement(@RequestBody SaveCmsAdvertisementCommand command) {
        return service.createAdvertisement(command);
    }

    @Override
    @PutMapping("/advertisements")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:advertisement:edit")
    public R<Boolean> updateAdvertisement(@RequestBody SaveCmsAdvertisementCommand command) {
        return service.updateAdvertisement(command);
    }

    @Override
    @PutMapping("/advertisements/status")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:advertisement:status")
    public R<Boolean> updateAdvertisementStatus(@RequestBody UpdateCmsStatusCommand command) {
        return service.updateAdvertisementStatus(command);
    }

    @Override
    @DeleteMapping("/advertisements")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:advertisement:delete")
    public R<Boolean> deleteAdvertisement(@RequestParam Long id) {
        return service.deleteAdvertisement(id);
    }

    @Override
    @GetMapping("/ad-deliveries/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:ad-delivery:list")
    public R<PageResult<CmsAdDeliveryVO>> pageAdDeliveries(@ParameterObject CmsAdDeliveryPageQuery query) {
        return service.pageAdDeliveries(query);
    }

    @Override
    @GetMapping("/ad-deliveries/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:ad-delivery:query")
    public R<CmsAdDeliveryVO> detailAdDelivery(@RequestParam Long id) {
        return service.detailAdDelivery(id);
    }

    @Override
    @PostMapping("/ad-deliveries")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:ad-delivery:add")
    public R<Long> createAdDelivery(@RequestBody SaveCmsAdDeliveryCommand command) {
        return service.createAdDelivery(command);
    }

    @Override
    @PutMapping("/ad-deliveries")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:ad-delivery:edit")
    public R<Boolean> updateAdDelivery(@RequestBody SaveCmsAdDeliveryCommand command) {
        return service.updateAdDelivery(command);
    }

    @Override
    @PutMapping("/ad-deliveries/status")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:ad-delivery:status")
    public R<Boolean> updateAdDeliveryStatus(@RequestBody UpdateCmsStatusCommand command) {
        return service.updateAdDeliveryStatus(command);
    }

    @Override
    @DeleteMapping("/ad-deliveries")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:ad-delivery:delete")
    public R<Boolean> deleteAdDelivery(@RequestParam Long id) {
        return service.deleteAdDelivery(id);
    }

    @Override
    @GetMapping("/site-settings/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:site-setting:query")
    public R<CmsSiteSettingVO> detailSiteSetting(@RequestParam Long siteId) {
        return service.detailSiteSetting(siteId);
    }

    @Override
    @PutMapping("/site-settings")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:site-setting:edit")
    public R<Boolean> saveSiteSetting(@RequestBody SaveCmsSiteSettingCommand command) {
        return service.saveSiteSetting(command);
    }
}
