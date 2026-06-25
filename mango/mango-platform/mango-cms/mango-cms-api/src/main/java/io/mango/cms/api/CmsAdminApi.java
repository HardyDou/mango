package io.mango.cms.api;

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
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
public interface CmsAdminApi {

    R<PageResult<CmsContentCategoryVO>> pageContentCategories(@Valid CmsContentCategoryPageQuery query);

    R<List<CmsContentCategoryVO>> listContentCategories(@Valid CmsContentCategoryPageQuery query);

    R<List<CmsContentCategoryVO>> treeContentCategories(@Valid CmsContentCategoryPageQuery query);

    R<CmsContentCategoryVO> detailContentCategory(@NotNull(message = "分类 ID 不能为空") Long id);

    R<Long> createContentCategory(@Valid SaveCmsContentCategoryCommand command);

    R<Boolean> updateContentCategory(@Valid SaveCmsContentCategoryCommand command);

    R<Boolean> updateContentCategoryStatus(@Valid UpdateCmsStatusCommand command);

    R<Boolean> deleteContentCategory(@NotNull(message = "分类 ID 不能为空") Long id);

    R<PageResult<CmsContentTagVO>> pageContentTags(@Valid CmsContentTagPageQuery query);

    R<List<CmsContentTagVO>> listContentTags(@Valid CmsContentTagPageQuery query);

    R<CmsContentTagVO> detailContentTag(@NotNull(message = "标签 ID 不能为空") Long id);

    R<Long> createContentTag(@Valid SaveCmsContentTagCommand command);

    R<Boolean> updateContentTag(@Valid SaveCmsContentTagCommand command);

    R<Boolean> updateContentTagStatus(@Valid UpdateCmsStatusCommand command);

    R<Boolean> deleteContentTag(@NotNull(message = "标签 ID 不能为空") Long id);

    R<PageResult<CmsSiteVO>> pageSites(@Valid CmsSitePageQuery query);

    R<CmsSiteVO> detailSite(@NotNull(message = "站点 ID 不能为空") Long id);

    R<Long> createSite(@Valid SaveCmsSiteCommand command);

    R<Boolean> updateSite(@Valid SaveCmsSiteCommand command);

    R<Boolean> updateSiteStatus(@Valid UpdateCmsStatusCommand command);

    R<Boolean> deleteSite(@NotNull(message = "站点 ID 不能为空") Long id);

    R<List<CmsSiteCategoryVO>> treeSiteCategories(@Valid CmsSiteCategoryTreeQuery query);

    R<CmsSiteCategoryVO> detailSiteCategory(@NotNull(message = "栏目 ID 不能为空") Long id);

    R<Long> createSiteCategory(@Valid SaveCmsSiteCategoryCommand command);

    R<Boolean> updateSiteCategory(@Valid SaveCmsSiteCategoryCommand command);

    R<Boolean> updateSiteCategoryStatus(@Valid UpdateCmsStatusCommand command);

    R<Boolean> deleteSiteCategory(@NotNull(message = "栏目 ID 不能为空") Long id);

    R<PageResult<CmsContentVO>> pageContents(@Valid CmsContentPageQuery query);

    R<CmsContentVO> detailContent(@NotNull(message = "内容 ID 不能为空") Long id);

    R<Long> createContent(@Valid SaveCmsContentCommand command);

    R<Boolean> updateContent(@Valid SaveCmsContentCommand command);

    R<Boolean> submitContent(@Valid CmsOfflineCommand command);

    R<Boolean> approveContent(@Valid UpdateCmsContentReviewCommand command);

    R<Boolean> rejectContent(@Valid UpdateCmsContentReviewCommand command);

    R<Boolean> offlineContent(@Valid CmsOfflineCommand command);

    R<Boolean> deleteContent(@NotNull(message = "内容 ID 不能为空") Long id);

    R<PageResult<CmsContentPublishVO>> pagePublishes(@Valid CmsContentPublishPageQuery query);

    R<Boolean> publishContents(@Valid BatchCmsContentPublishCommand command);

    R<Boolean> offlinePublish(@Valid CmsOfflineCommand command);

    R<Boolean> deletePublish(@NotNull(message = "发布关系 ID 不能为空") Long id);

    R<PageResult<CmsNavigationVO>> pageNavigations(@Valid CmsNavigationPageQuery query);

    R<CmsNavigationVO> detailNavigation(@NotNull(message = "导航 ID 不能为空") Long id);

    R<Long> createNavigation(@Valid SaveCmsNavigationCommand command);

    R<Boolean> updateNavigation(@Valid SaveCmsNavigationCommand command);

    R<Boolean> updateNavigationStatus(@Valid UpdateCmsStatusCommand command);

    R<Boolean> deleteNavigation(@NotNull(message = "导航 ID 不能为空") Long id);

    R<PageResult<CmsBannerVO>> pageBanners(@Valid CmsBannerPageQuery query);

    R<CmsBannerVO> detailBanner(@NotNull(message = "Banner ID 不能为空") Long id);

    R<Long> createBanner(@Valid SaveCmsBannerCommand command);

    R<Boolean> updateBanner(@Valid SaveCmsBannerCommand command);

    R<Boolean> updateBannerStatus(@Valid UpdateCmsStatusCommand command);

    R<Boolean> deleteBanner(@NotNull(message = "Banner ID 不能为空") Long id);

    R<PageResult<CmsAdvertisementVO>> pageAdvertisements(@Valid CmsAdvertisementPageQuery query);

    R<CmsAdvertisementVO> detailAdvertisement(@NotNull(message = "广告 ID 不能为空") Long id);

    R<Long> createAdvertisement(@Valid SaveCmsAdvertisementCommand command);

    R<Boolean> updateAdvertisement(@Valid SaveCmsAdvertisementCommand command);

    R<Boolean> updateAdvertisementStatus(@Valid UpdateCmsStatusCommand command);

    R<Boolean> deleteAdvertisement(@NotNull(message = "广告 ID 不能为空") Long id);

    R<PageResult<CmsAdDeliveryVO>> pageAdDeliveries(@Valid CmsAdDeliveryPageQuery query);

    R<CmsAdDeliveryVO> detailAdDelivery(@NotNull(message = "广告投放 ID 不能为空") Long id);

    R<Long> createAdDelivery(@Valid SaveCmsAdDeliveryCommand command);

    R<Boolean> updateAdDelivery(@Valid SaveCmsAdDeliveryCommand command);

    R<Boolean> updateAdDeliveryStatus(@Valid UpdateCmsStatusCommand command);

    R<Boolean> deleteAdDelivery(@NotNull(message = "广告投放 ID 不能为空") Long id);

    R<CmsSiteSettingVO> detailSiteSetting(@NotNull(message = "站点 ID 不能为空") Long siteId);

    R<Boolean> saveSiteSetting(@Valid SaveCmsSiteSettingCommand command);
}
