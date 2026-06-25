package io.mango.cms.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import io.mango.cms.api.enums.CmsAdPositionType;
import io.mango.cms.api.enums.CmsAccessType;
import io.mango.cms.api.enums.CmsAdvertisementType;
import io.mango.cms.api.enums.CmsBannerMediaType;
import io.mango.cms.api.enums.CmsCategoryType;
import io.mango.cms.api.enums.CmsContentStatus;
import io.mango.cms.api.enums.CmsContentType;
import io.mango.cms.api.enums.CmsJumpType;
import io.mango.cms.api.enums.CmsNavigationType;
import io.mango.cms.api.enums.CmsOpenTarget;
import io.mango.cms.api.enums.CmsPublishStatus;
import io.mango.cms.api.enums.CmsRecommendationType;
import io.mango.cms.api.enums.CmsStatus;
import io.mango.cms.api.enums.CmsTopScope;
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
import io.mango.cms.core.entity.CmsAdDeliveryEntity;
import io.mango.cms.core.entity.CmsAdvertisementEntity;
import io.mango.cms.core.entity.CmsBannerEntity;
import io.mango.cms.core.entity.CmsContentCategoryEntity;
import io.mango.cms.core.entity.CmsContentEntity;
import io.mango.cms.core.entity.CmsContentPublishEntity;
import io.mango.cms.core.entity.CmsContentTagEntity;
import io.mango.cms.core.entity.CmsContentTagRelEntity;
import io.mango.cms.core.entity.CmsNavigationEntity;
import io.mango.cms.core.entity.CmsSiteCategoryEntity;
import io.mango.cms.core.entity.CmsSiteEntity;
import io.mango.cms.core.entity.CmsSiteSettingEntity;
import io.mango.cms.core.mapper.CmsAdDeliveryMapper;
import io.mango.cms.core.mapper.CmsAdvertisementMapper;
import io.mango.cms.core.mapper.CmsBannerMapper;
import io.mango.cms.core.mapper.CmsContentCategoryMapper;
import io.mango.cms.core.mapper.CmsContentMapper;
import io.mango.cms.core.mapper.CmsContentPublishMapper;
import io.mango.cms.core.mapper.CmsContentTagMapper;
import io.mango.cms.core.mapper.CmsContentTagRelMapper;
import io.mango.cms.core.mapper.CmsNavigationMapper;
import io.mango.cms.core.mapper.CmsSiteCategoryMapper;
import io.mango.cms.core.mapper.CmsSiteMapper;
import io.mango.cms.core.mapper.CmsSiteSettingMapper;
import io.mango.cms.core.service.ICmsAdminService;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.file.api.FileApi;
import io.mango.file.api.enums.FileRecordStatus;
import io.mango.file.api.vo.FileRecordVO;
import io.mango.infra.persistence.api.scope.DataScopeApplier;
import io.mango.infra.persistence.api.scope.DataScopeMapping;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CmsAdminService implements ICmsAdminService {

    private static final String LIST_RESOURCE_SUFFIX = ":list";
    private static final DataScopeMapping CONTENT_CATEGORY_SCOPE = cmsScope("cms_content_category");
    private static final DataScopeMapping CONTENT_TAG_SCOPE = cmsScope("cms_content_tag");
    private static final DataScopeMapping SITE_SCOPE = cmsScope("cms_site");
    private static final DataScopeMapping SITE_CATEGORY_SCOPE = cmsScope("cms_site_category");
    private static final DataScopeMapping CONTENT_SCOPE = cmsScope("cms_content");
    private static final DataScopeMapping PUBLISH_SCOPE = cmsScope("cms_content_publish");
    private static final DataScopeMapping NAVIGATION_SCOPE = cmsScope("cms_navigation");
    private static final DataScopeMapping BANNER_SCOPE = cmsScope("cms_banner");
    private static final DataScopeMapping ADVERTISEMENT_SCOPE = cmsScope("cms_advertisement");
    private static final DataScopeMapping AD_DELIVERY_SCOPE = cmsScope("cms_ad_delivery");

    private final CmsContentCategoryMapper contentCategoryMapper;
    private final CmsContentTagMapper contentTagMapper;
    private final CmsContentTagRelMapper contentTagRelMapper;
    private final CmsSiteMapper siteMapper;
    private final CmsSiteCategoryMapper siteCategoryMapper;
    private final CmsContentMapper contentMapper;
    private final CmsContentPublishMapper publishMapper;
    private final CmsNavigationMapper navigationMapper;
    private final CmsBannerMapper bannerMapper;
    private final CmsAdvertisementMapper advertisementMapper;
    private final CmsAdDeliveryMapper adDeliveryMapper;
    private final CmsSiteSettingMapper siteSettingMapper;
    private final ObjectProvider<DataScopeApplier> dataScopeApplierProvider;
    private final ObjectProvider<FileApi> fileApiProvider;

    @Override
    public R<PageResult<CmsContentCategoryVO>> pageContentCategories(CmsContentCategoryPageQuery query) {
        CmsContentCategoryPageQuery resolved = query == null ? new CmsContentCategoryPageQuery() : query;
        IPage<CmsContentCategoryEntity> page = contentCategoryMapper.selectPage(
                new Page<>(resolved.getPage(), resolved.getSize()),
                contentCategoryWrapper(resolved));
        return R.ok(PageResult.of(page.getRecords().stream().map(this::toContentCategoryVO).toList(),
                page.getTotal(), page.getCurrent(), page.getSize()));
    }

    @Override
    public R<List<CmsContentCategoryVO>> listContentCategories(CmsContentCategoryPageQuery query) {
        CmsContentCategoryPageQuery resolved = query == null ? new CmsContentCategoryPageQuery() : query;
        if (!StringUtils.hasText(resolved.getStatus())) {
            resolved.setStatus(CmsSupport.ENABLED);
        }
        return R.ok(contentCategoryMapper.selectList(contentCategoryWrapper(resolved))
                .stream().map(this::toContentCategoryVO).toList());
    }

    @Override
    public R<List<CmsContentCategoryVO>> treeContentCategories(CmsContentCategoryPageQuery query) {
        CmsContentCategoryPageQuery resolved = query == null ? new CmsContentCategoryPageQuery() : query;
        List<CmsContentCategoryEntity> records = contentCategoryMapper.selectList(contentCategoryWrapper(resolved));
        return R.ok(buildContentCategoryTree(records.stream().map(this::toContentCategoryVO).toList()));
    }

    @Override
    public R<CmsContentCategoryVO> detailContentCategory(Long id) {
        return R.ok(toContentCategoryVO(requireContentCategory(id)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Long> createContentCategory(SaveCmsContentCategoryCommand command) {
        CmsContentCategoryEntity entity = new CmsContentCategoryEntity();
        applyContentCategory(entity, command, false);
        entity.setTenantId(CmsSupport.currentTenantId());
        contentCategoryMapper.insert(entity);
        return R.ok(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> updateContentCategory(SaveCmsContentCategoryCommand command) {
        CmsContentCategoryEntity entity = requireContentCategory(command.getId());
        applyContentCategory(entity, command, true);
        return R.ok(contentCategoryMapper.updateById(entity) > 0);
    }

    @Override
    public R<Boolean> updateContentCategoryStatus(UpdateCmsStatusCommand command) {
        CmsContentCategoryEntity entity = requireContentCategory(command.getId());
        entity.setStatus(CmsSupport.enumName(CmsStatus.class, command.getStatus(), "分类状态非法"));
        return R.ok(contentCategoryMapper.updateById(entity) > 0);
    }

    @Override
    public R<Boolean> deleteContentCategory(Long id) {
        CmsContentCategoryEntity entity = requireContentCategory(id);
        Long count = contentMapper.selectCount(new LambdaQueryWrapper<CmsContentEntity>()
                .eq(CmsContentEntity::getTenantId, CmsSupport.currentTenantId())
                .eq(CmsContentEntity::getCategoryId, id));
        Require.isTrue(count == 0, "分类已被内容引用，不能删除");
        return R.ok(contentCategoryMapper.deleteById(entity.getId()) > 0);
    }

    @Override
    public R<PageResult<CmsContentTagVO>> pageContentTags(CmsContentTagPageQuery query) {
        CmsContentTagPageQuery resolved = query == null ? new CmsContentTagPageQuery() : query;
        IPage<CmsContentTagEntity> page = contentTagMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()),
                contentTagWrapper(resolved));
        return R.ok(PageResult.of(page.getRecords().stream().map(this::toContentTagVO).toList(),
                page.getTotal(), page.getCurrent(), page.getSize()));
    }

    @Override
    public R<List<CmsContentTagVO>> listContentTags(CmsContentTagPageQuery query) {
        CmsContentTagPageQuery resolved = query == null ? new CmsContentTagPageQuery() : query;
        if (!StringUtils.hasText(resolved.getStatus())) {
            resolved.setStatus(CmsSupport.ENABLED);
        }
        return R.ok(contentTagMapper.selectList(contentTagWrapper(resolved)).stream().map(this::toContentTagVO).toList());
    }

    @Override
    public R<CmsContentTagVO> detailContentTag(Long id) {
        return R.ok(toContentTagVO(requireContentTag(id)));
    }

    @Override
    public R<Long> createContentTag(SaveCmsContentTagCommand command) {
        CmsContentTagEntity entity = new CmsContentTagEntity();
        applyContentTag(entity, command, false);
        entity.setTenantId(CmsSupport.currentTenantId());
        contentTagMapper.insert(entity);
        return R.ok(entity.getId());
    }

    @Override
    public R<Boolean> updateContentTag(SaveCmsContentTagCommand command) {
        CmsContentTagEntity entity = requireContentTag(command.getId());
        applyContentTag(entity, command, true);
        return R.ok(contentTagMapper.updateById(entity) > 0);
    }

    @Override
    public R<Boolean> updateContentTagStatus(UpdateCmsStatusCommand command) {
        CmsContentTagEntity entity = requireContentTag(command.getId());
        entity.setStatus(CmsSupport.enumName(CmsStatus.class, command.getStatus(), "标签状态非法"));
        return R.ok(contentTagMapper.updateById(entity) > 0);
    }

    @Override
    public R<Boolean> deleteContentTag(Long id) {
        CmsContentTagEntity entity = requireContentTag(id);
        Long count = contentTagRelMapper.selectCount(new LambdaQueryWrapper<CmsContentTagRelEntity>()
                .eq(CmsContentTagRelEntity::getTenantId, CmsSupport.currentTenantId())
                .eq(CmsContentTagRelEntity::getTagId, id));
        Require.isTrue(count == 0, "标签已被内容引用，不能删除");
        return R.ok(contentTagMapper.deleteById(entity.getId()) > 0);
    }

    @Override
    public R<PageResult<CmsSiteVO>> pageSites(CmsSitePageQuery query) {
        CmsSitePageQuery resolved = query == null ? new CmsSitePageQuery() : query;
        IPage<CmsSiteEntity> page = siteMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()), siteWrapper(resolved));
        return R.ok(PageResult.of(page.getRecords().stream().map(this::toSiteVO).toList(),
                page.getTotal(), page.getCurrent(), page.getSize()));
    }

    @Override
    public R<CmsSiteVO> detailSite(Long id) {
        return R.ok(toSiteVO(requireSite(id)));
    }

    @Override
    public R<Long> createSite(SaveCmsSiteCommand command) {
        CmsSiteEntity entity = new CmsSiteEntity();
        applySite(entity, command, false);
        entity.setTenantId(CmsSupport.currentTenantId());
        siteMapper.insert(entity);
        saveSiteSettingFromSite(entity);
        return R.ok(entity.getId());
    }

    @Override
    public R<Boolean> updateSite(SaveCmsSiteCommand command) {
        CmsSiteEntity entity = requireSite(command.getId());
        applySite(entity, command, true);
        boolean updated = siteMapper.updateById(entity) > 0;
        saveSiteSettingFromSite(entity);
        return R.ok(updated);
    }

    @Override
    public R<Boolean> updateSiteStatus(UpdateCmsStatusCommand command) {
        CmsSiteEntity entity = requireSite(command.getId());
        entity.setStatus(CmsSupport.enumName(CmsStatus.class, command.getStatus(), "站点状态非法"));
        return R.ok(siteMapper.updateById(entity) > 0);
    }

    @Override
    public R<Boolean> deleteSite(Long id) {
        CmsSiteEntity entity = requireSite(id);
        Require.isTrue(countBySite(id) == 0, "站点已被栏目、导航、Banner、广告或发布关系引用，不能删除");
        return R.ok(siteMapper.deleteById(entity.getId()) > 0);
    }

    @Override
    public R<List<CmsSiteCategoryVO>> treeSiteCategories(CmsSiteCategoryTreeQuery query) {
        Require.notNull(query, "栏目查询不能为空");
        requireSite(query.getSiteId());
        List<CmsSiteCategoryEntity> records = siteCategoryMapper.selectList(siteCategoryWrapper(query));
        return R.ok(buildCategoryTree(records.stream().map(this::toSiteCategoryVO).toList()));
    }

    @Override
    public R<CmsSiteCategoryVO> detailSiteCategory(Long id) {
        return R.ok(toSiteCategoryVO(requireSiteCategory(id)));
    }

    @Override
    public R<Long> createSiteCategory(SaveCmsSiteCategoryCommand command) {
        requireSite(command.getSiteId());
        CmsSiteCategoryEntity entity = new CmsSiteCategoryEntity();
        applySiteCategory(entity, command, false);
        entity.setTenantId(CmsSupport.currentTenantId());
        siteCategoryMapper.insert(entity);
        return R.ok(entity.getId());
    }

    @Override
    public R<Boolean> updateSiteCategory(SaveCmsSiteCategoryCommand command) {
        CmsSiteCategoryEntity entity = requireSiteCategory(command.getId());
        applySiteCategory(entity, command, true);
        return R.ok(siteCategoryMapper.updateById(entity) > 0);
    }

    @Override
    public R<Boolean> updateSiteCategoryStatus(UpdateCmsStatusCommand command) {
        CmsSiteCategoryEntity entity = requireSiteCategory(command.getId());
        entity.setVisibleStatus(CmsSupport.enumName(CmsStatus.class, command.getStatus(), "栏目状态非法"));
        return R.ok(siteCategoryMapper.updateById(entity) > 0);
    }

    @Override
    public R<Boolean> deleteSiteCategory(Long id) {
        CmsSiteCategoryEntity entity = requireSiteCategory(id);
        Long childCount = siteCategoryMapper.selectCount(new LambdaQueryWrapper<CmsSiteCategoryEntity>()
                .eq(CmsSiteCategoryEntity::getTenantId, CmsSupport.currentTenantId())
                .eq(CmsSiteCategoryEntity::getParentId, id));
        Long publishCount = publishMapper.selectCount(new LambdaQueryWrapper<CmsContentPublishEntity>()
                .eq(CmsContentPublishEntity::getTenantId, CmsSupport.currentTenantId())
                .eq(CmsContentPublishEntity::getCategoryId, id));
        Require.isTrue(childCount == 0 && publishCount == 0, "栏目存在子栏目或发布关系，不能删除");
        return R.ok(siteCategoryMapper.deleteById(entity.getId()) > 0);
    }

    @Override
    public R<PageResult<CmsContentVO>> pageContents(CmsContentPageQuery query) {
        CmsContentPageQuery resolved = query == null ? new CmsContentPageQuery() : query;
        IPage<CmsContentEntity> page = contentMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()),
                contentWrapper(resolved));
        return R.ok(PageResult.of(page.getRecords().stream().map(this::toContentVO).toList(),
                page.getTotal(), page.getCurrent(), page.getSize()));
    }

    @Override
    public R<CmsContentVO> detailContent(Long id) {
        return R.ok(toContentVO(requireContent(id)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Long> createContent(SaveCmsContentCommand command) {
        CmsContentEntity entity = new CmsContentEntity();
        applyContent(entity, command);
        entity.setTenantId(CmsSupport.currentTenantId());
        entity.setStatus(CmsContentStatus.DRAFT.name());
        contentMapper.insert(entity);
        saveContentTags(entity.getId(), command.getTagIds());
        return R.ok(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> updateContent(SaveCmsContentCommand command) {
        CmsContentEntity entity = requireContent(command.getId());
        Require.isTrue(CmsContentStatus.DRAFT.name().equals(entity.getStatus())
                || CmsContentStatus.REJECTED.name().equals(entity.getStatus())
                || CmsContentStatus.PUBLISHED.name().equals(entity.getStatus()), "当前内容状态不能编辑");
        applyContent(entity, command);
        boolean updated = contentMapper.updateById(entity) > 0;
        saveContentTags(entity.getId(), command.getTagIds());
        return R.ok(updated);
    }

    @Override
    public R<Boolean> submitContent(CmsOfflineCommand command) {
        CmsContentEntity entity = requireContent(command.getId());
        Require.isTrue(CmsContentStatus.DRAFT.name().equals(entity.getStatus())
                || CmsContentStatus.REJECTED.name().equals(entity.getStatus()), "只有草稿或驳回内容可以提交审核");
        entity.setStatus(CmsContentStatus.PENDING_REVIEW.name());
        return R.ok(contentMapper.updateById(entity) > 0);
    }

    @Override
    public R<Boolean> approveContent(UpdateCmsContentReviewCommand command) {
        CmsContentEntity entity = requireContent(command.getId());
        Require.isTrue(CmsContentStatus.PENDING_REVIEW.name().equals(entity.getStatus()), "只有待审核内容可以审核通过");
        entity.setStatus(CmsContentStatus.PUBLISHED.name());
        entity.setReviewComment(CmsSupport.trimToNull(command.getReviewComment()));
        entity.setPublishTime(LocalDateTime.now());
        return R.ok(contentMapper.updateById(entity) > 0);
    }

    @Override
    public R<Boolean> rejectContent(UpdateCmsContentReviewCommand command) {
        CmsContentEntity entity = requireContent(command.getId());
        Require.isTrue(CmsContentStatus.PENDING_REVIEW.name().equals(entity.getStatus()), "只有待审核内容可以驳回");
        entity.setStatus(CmsContentStatus.REJECTED.name());
        entity.setReviewComment(CmsSupport.trimToNull(command.getReviewComment()));
        return R.ok(contentMapper.updateById(entity) > 0);
    }

    @Override
    public R<Boolean> offlineContent(CmsOfflineCommand command) {
        CmsContentEntity entity = requireContent(command.getId());
        entity.setStatus(CmsContentStatus.OFFLINE.name());
        entity.setOfflineTime(LocalDateTime.now());
        return R.ok(contentMapper.updateById(entity) > 0);
    }

    @Override
    public R<Boolean> deleteContent(Long id) {
        CmsContentEntity entity = requireContent(id);
        Require.isTrue(!CmsContentStatus.PUBLISHED.name().equals(entity.getStatus()), "已发布内容需先下线再删除");
        return R.ok(contentMapper.deleteById(entity.getId()) > 0);
    }

    @Override
    public R<PageResult<CmsContentPublishVO>> pagePublishes(CmsContentPublishPageQuery query) {
        CmsContentPublishPageQuery resolved = query == null ? new CmsContentPublishPageQuery() : query;
        IPage<CmsContentPublishEntity> page = publishMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()),
                publishWrapper(resolved));
        return R.ok(PageResult.of(page.getRecords().stream().map(this::toPublishVO).toList(),
                page.getTotal(), page.getCurrent(), page.getSize()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> publishContents(BatchCmsContentPublishCommand command) {
        Require.notNull(command, "发布命令不能为空");
        requireSite(command.getSiteId());
        Require.isTrue(command.getContentIds() != null && !command.getContentIds().isEmpty(), "发布内容不能为空");
        Require.isTrue(command.getCategoryIds() != null && !command.getCategoryIds().isEmpty(), "发布栏目不能为空");
        for (Long categoryId : command.getCategoryIds()) {
            CmsSiteCategoryEntity category = requireSiteCategory(categoryId);
            Require.isTrue(command.getSiteId().equals(category.getSiteId()), "发布栏目不属于目标站点");
        }
        for (Long contentId : command.getContentIds()) {
            CmsContentEntity content = requireContent(contentId);
            Require.isTrue(CmsContentStatus.PUBLISHED.name().equals(content.getStatus()), "只有审核通过内容可以发布");
            for (Long categoryId : command.getCategoryIds()) {
                upsertPublish(command, contentId, categoryId);
            }
        }
        return R.ok(true);
    }

    @Override
    public R<Boolean> offlinePublish(CmsOfflineCommand command) {
        CmsContentPublishEntity entity = requirePublish(command.getId());
        entity.setPublishStatus(CmsPublishStatus.OFFLINE.name());
        entity.setOfflineTime(LocalDateTime.now());
        return R.ok(publishMapper.updateById(entity) > 0);
    }

    @Override
    public R<Boolean> deletePublish(Long id) {
        CmsContentPublishEntity entity = requirePublish(id);
        return R.ok(publishMapper.deleteById(entity.getId()) > 0);
    }

    @Override
    public R<PageResult<CmsNavigationVO>> pageNavigations(CmsNavigationPageQuery query) {
        CmsNavigationPageQuery resolved = query == null ? new CmsNavigationPageQuery() : query;
        IPage<CmsNavigationEntity> page = navigationMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()),
                navigationWrapper(resolved));
        return R.ok(PageResult.of(page.getRecords().stream().map(this::toNavigationVO).toList(),
                page.getTotal(), page.getCurrent(), page.getSize()));
    }

    @Override
    public R<CmsNavigationVO> detailNavigation(Long id) {
        return R.ok(toNavigationVO(requireNavigation(id)));
    }

    @Override
    public R<Long> createNavigation(SaveCmsNavigationCommand command) {
        CmsNavigationEntity entity = new CmsNavigationEntity();
        applyNavigation(entity, command);
        entity.setTenantId(CmsSupport.currentTenantId());
        navigationMapper.insert(entity);
        return R.ok(entity.getId());
    }

    @Override
    public R<Boolean> updateNavigation(SaveCmsNavigationCommand command) {
        CmsNavigationEntity entity = requireNavigation(command.getId());
        applyNavigation(entity, command);
        return R.ok(navigationMapper.updateById(entity) > 0);
    }

    @Override
    public R<Boolean> updateNavigationStatus(UpdateCmsStatusCommand command) {
        CmsNavigationEntity entity = requireNavigation(command.getId());
        entity.setStatus(CmsSupport.enumName(CmsStatus.class, command.getStatus(), "导航状态非法"));
        return R.ok(navigationMapper.updateById(entity) > 0);
    }

    @Override
    public R<Boolean> deleteNavigation(Long id) {
        return R.ok(navigationMapper.deleteById(requireNavigation(id).getId()) > 0);
    }

    @Override
    public R<PageResult<CmsBannerVO>> pageBanners(CmsBannerPageQuery query) {
        CmsBannerPageQuery resolved = query == null ? new CmsBannerPageQuery() : query;
        IPage<CmsBannerEntity> page = bannerMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()),
                bannerWrapper(resolved));
        return R.ok(PageResult.of(page.getRecords().stream().map(this::toBannerVO).toList(),
                page.getTotal(), page.getCurrent(), page.getSize()));
    }

    @Override
    public R<CmsBannerVO> detailBanner(Long id) {
        return R.ok(toBannerVO(requireBanner(id)));
    }

    @Override
    public R<Long> createBanner(SaveCmsBannerCommand command) {
        CmsBannerEntity entity = new CmsBannerEntity();
        applyBanner(entity, command);
        entity.setTenantId(CmsSupport.currentTenantId());
        bannerMapper.insert(entity);
        return R.ok(entity.getId());
    }

    @Override
    public R<Boolean> updateBanner(SaveCmsBannerCommand command) {
        CmsBannerEntity entity = requireBanner(command.getId());
        applyBanner(entity, command);
        return R.ok(bannerMapper.updateById(entity) > 0);
    }

    @Override
    public R<Boolean> updateBannerStatus(UpdateCmsStatusCommand command) {
        CmsBannerEntity entity = requireBanner(command.getId());
        entity.setStatus(CmsSupport.enumName(CmsStatus.class, command.getStatus(), "Banner 状态非法"));
        return R.ok(bannerMapper.updateById(entity) > 0);
    }

    @Override
    public R<Boolean> deleteBanner(Long id) {
        return R.ok(bannerMapper.deleteById(requireBanner(id).getId()) > 0);
    }

    @Override
    public R<PageResult<CmsAdvertisementVO>> pageAdvertisements(CmsAdvertisementPageQuery query) {
        CmsAdvertisementPageQuery resolved = query == null ? new CmsAdvertisementPageQuery() : query;
        IPage<CmsAdvertisementEntity> page = advertisementMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()),
                advertisementWrapper(resolved));
        return R.ok(PageResult.of(page.getRecords().stream().map(this::toAdvertisementVO).toList(),
                page.getTotal(), page.getCurrent(), page.getSize()));
    }

    @Override
    public R<CmsAdvertisementVO> detailAdvertisement(Long id) {
        return R.ok(toAdvertisementVO(requireAdvertisement(id)));
    }

    @Override
    public R<Long> createAdvertisement(SaveCmsAdvertisementCommand command) {
        CmsAdvertisementEntity entity = new CmsAdvertisementEntity();
        applyAdvertisement(entity, command, false);
        entity.setTenantId(CmsSupport.currentTenantId());
        advertisementMapper.insert(entity);
        return R.ok(entity.getId());
    }

    @Override
    public R<Boolean> updateAdvertisement(SaveCmsAdvertisementCommand command) {
        CmsAdvertisementEntity entity = requireAdvertisement(command.getId());
        applyAdvertisement(entity, command, true);
        return R.ok(advertisementMapper.updateById(entity) > 0);
    }

    @Override
    public R<Boolean> updateAdvertisementStatus(UpdateCmsStatusCommand command) {
        CmsAdvertisementEntity entity = requireAdvertisement(command.getId());
        entity.setStatus(CmsSupport.enumName(CmsStatus.class, command.getStatus(), "广告状态非法"));
        return R.ok(advertisementMapper.updateById(entity) > 0);
    }

    @Override
    public R<Boolean> deleteAdvertisement(Long id) {
        CmsAdvertisementEntity entity = requireAdvertisement(id);
        Long deliveryCount = adDeliveryMapper.selectCount(new LambdaQueryWrapper<CmsAdDeliveryEntity>()
                .eq(CmsAdDeliveryEntity::getTenantId, CmsSupport.currentTenantId())
                .eq(CmsAdDeliveryEntity::getAdId, id));
        Require.isTrue(deliveryCount == 0, "广告位存在投放记录，不能删除");
        return R.ok(advertisementMapper.deleteById(entity.getId()) > 0);
    }

    @Override
    public R<PageResult<CmsAdDeliveryVO>> pageAdDeliveries(CmsAdDeliveryPageQuery query) {
        CmsAdDeliveryPageQuery resolved = query == null ? new CmsAdDeliveryPageQuery() : query;
        IPage<CmsAdDeliveryEntity> page = adDeliveryMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()),
                adDeliveryWrapper(resolved));
        return R.ok(PageResult.of(page.getRecords().stream().map(this::toAdDeliveryVO).toList(),
                page.getTotal(), page.getCurrent(), page.getSize()));
    }

    @Override
    public R<CmsAdDeliveryVO> detailAdDelivery(Long id) {
        return R.ok(toAdDeliveryVO(requireAdDelivery(id)));
    }

    @Override
    public R<Long> createAdDelivery(SaveCmsAdDeliveryCommand command) {
        CmsAdDeliveryEntity entity = new CmsAdDeliveryEntity();
        applyAdDelivery(entity, command);
        entity.setTenantId(CmsSupport.currentTenantId());
        adDeliveryMapper.insert(entity);
        return R.ok(entity.getId());
    }

    @Override
    public R<Boolean> updateAdDelivery(SaveCmsAdDeliveryCommand command) {
        CmsAdDeliveryEntity entity = requireAdDelivery(command.getId());
        applyAdDelivery(entity, command);
        return R.ok(adDeliveryMapper.updateById(entity) > 0);
    }

    @Override
    public R<Boolean> updateAdDeliveryStatus(UpdateCmsStatusCommand command) {
        CmsAdDeliveryEntity entity = requireAdDelivery(command.getId());
        entity.setStatus(CmsSupport.enumName(CmsStatus.class, command.getStatus(), "广告投放状态非法"));
        return R.ok(adDeliveryMapper.updateById(entity) > 0);
    }

    @Override
    public R<Boolean> deleteAdDelivery(Long id) {
        return R.ok(adDeliveryMapper.deleteById(requireAdDelivery(id).getId()) > 0);
    }

    @Override
    public R<CmsSiteSettingVO> detailSiteSetting(Long siteId) {
        requireSite(siteId);
        CmsSiteSettingEntity entity = siteSettingMapper.selectOne(new LambdaQueryWrapper<CmsSiteSettingEntity>()
                .eq(CmsSiteSettingEntity::getTenantId, CmsSupport.currentTenantId())
                .eq(CmsSiteSettingEntity::getSiteId, siteId)
                .last("LIMIT 1"));
        return R.ok(entity == null ? null : toSiteSettingVO(entity));
    }

    @Override
    public R<Boolean> saveSiteSetting(SaveCmsSiteSettingCommand command) {
        requireSite(command.getSiteId());
        CmsSiteSettingEntity entity = siteSettingMapper.selectOne(new LambdaQueryWrapper<CmsSiteSettingEntity>()
                .eq(CmsSiteSettingEntity::getTenantId, CmsSupport.currentTenantId())
                .eq(CmsSiteSettingEntity::getSiteId, command.getSiteId())
                .last("LIMIT 1"));
        boolean create = entity == null;
        if (create) {
            entity = new CmsSiteSettingEntity();
            entity.setTenantId(CmsSupport.currentTenantId());
            entity.setSiteId(command.getSiteId());
        }
        entity.setSeoTitle(CmsSupport.trimToNull(command.getSeoTitle()));
        entity.setSeoKeywords(CmsSupport.trimToNull(command.getSeoKeywords()));
        entity.setSeoDescription(CmsSupport.trimToNull(command.getSeoDescription()));
        entity.setFooterCopyright(CmsSupport.trimToNull(command.getFooterCopyright()));
        entity.setIcpRecord(CmsSupport.trimToNull(command.getIcpRecord()));
        entity.setContactInfo(CmsSupport.trimToNull(command.getContactInfo()));
        return R.ok(create ? siteSettingMapper.insert(entity) > 0 : siteSettingMapper.updateById(entity) > 0);
    }

    private void applyContentCategory(CmsContentCategoryEntity entity, SaveCmsContentCategoryCommand command, boolean update) {
        Require.notNull(command, "分类保存命令不能为空");
        if (update) {
            Require.notNull(command.getId(), "分类 ID 不能为空");
        }
        String code = CmsSupport.trimRequired(command.getCategoryCode(), "分类编码不能为空");
        CmsContentCategoryEntity exists = contentCategoryMapper.selectOne(new LambdaQueryWrapper<CmsContentCategoryEntity>()
                .eq(CmsContentCategoryEntity::getTenantId, CmsSupport.currentTenantId())
                .eq(CmsContentCategoryEntity::getCategoryCode, code)
                .last("LIMIT 1"));
        Require.isTrue(exists == null || exists.getId().equals(entity.getId()), "分类编码已存在");
        entity.setParentId(CmsSupport.defaultParentId(command.getParentId()));
        entity.setCategoryCode(code);
        entity.setCategoryName(CmsSupport.trimRequired(command.getCategoryName(), "分类名称不能为空"));
        entity.setSort(CmsSupport.defaultSort(command.getSort()));
        entity.setStatus(CmsSupport.defaultStatus(command.getStatus()));
        entity.setRemark(CmsSupport.trimToNull(command.getRemark()));
    }

    private void applyContentTag(CmsContentTagEntity entity, SaveCmsContentTagCommand command, boolean update) {
        Require.notNull(command, "标签保存命令不能为空");
        if (update) {
            Require.notNull(command.getId(), "标签 ID 不能为空");
        }
        String code = CmsSupport.trimRequired(command.getTagCode(), "标签编码不能为空");
        CmsContentTagEntity exists = contentTagMapper.selectOne(new LambdaQueryWrapper<CmsContentTagEntity>()
                .eq(CmsContentTagEntity::getTenantId, CmsSupport.currentTenantId())
                .eq(CmsContentTagEntity::getTagCode, code)
                .last("LIMIT 1"));
        Require.isTrue(exists == null || exists.getId().equals(entity.getId()), "标签编码已存在");
        entity.setTagCode(code);
        entity.setTagName(CmsSupport.trimRequired(command.getTagName(), "标签名称不能为空"));
        entity.setSort(CmsSupport.defaultSort(command.getSort()));
        entity.setStatus(CmsSupport.defaultStatus(command.getStatus()));
        entity.setRemark(CmsSupport.trimToNull(command.getRemark()));
    }

    private void applySite(CmsSiteEntity entity, SaveCmsSiteCommand command, boolean update) {
        Require.notNull(command, "站点保存命令不能为空");
        if (update) {
            Require.notNull(command.getId(), "站点 ID 不能为空");
        }
        String code = CmsSupport.trimRequired(command.getSiteCode(), "站点编码不能为空");
        CmsSiteEntity exists = siteMapper.selectOne(new LambdaQueryWrapper<CmsSiteEntity>()
                .eq(CmsSiteEntity::getTenantId, CmsSupport.currentTenantId())
                .eq(CmsSiteEntity::getSiteCode, code)
                .last("LIMIT 1"));
        Require.isTrue(exists == null || exists.getId().equals(entity.getId()), "站点编码已存在");
        String domain = CmsSupport.trimToNull(command.getDomain());
        if (StringUtils.hasText(domain)) {
            CmsSiteEntity domainExists = siteMapper.selectOne(new LambdaQueryWrapper<CmsSiteEntity>()
                    .eq(CmsSiteEntity::getTenantId, CmsSupport.currentTenantId())
                    .eq(CmsSiteEntity::getDomain, domain)
                    .last("LIMIT 1"));
            Require.isTrue(domainExists == null || domainExists.getId().equals(entity.getId()), "站点域名已存在");
        }
        entity.setSiteName(CmsSupport.trimRequired(command.getSiteName(), "站点名称不能为空"));
        entity.setSiteCode(code);
        entity.setLogoFileId(validateImageFile(command.getLogoFileId(), "站点 Logo 文件"));
        entity.setDescription(CmsSupport.trimToNull(command.getDescription()));
        entity.setDomain(domain);
        entity.setDefaultLanguage(CmsSupport.trimToNull(command.getDefaultLanguage()));
        entity.setSeoTitle(CmsSupport.trimToNull(command.getSeoTitle()));
        entity.setSeoKeywords(CmsSupport.trimToNull(command.getSeoKeywords()));
        entity.setSeoDescription(CmsSupport.trimToNull(command.getSeoDescription()));
        entity.setFooterCopyright(CmsSupport.trimToNull(command.getFooterCopyright()));
        entity.setIcpRecord(CmsSupport.trimToNull(command.getIcpRecord()));
        entity.setContactInfo(CmsSupport.trimToNull(command.getContactInfo()));
        entity.setStatus(CmsSupport.defaultStatus(command.getStatus()));
    }

    private void applySiteCategory(CmsSiteCategoryEntity entity, SaveCmsSiteCategoryCommand command, boolean update) {
        Require.notNull(command, "栏目保存命令不能为空");
        if (update) {
            Require.notNull(command.getId(), "栏目 ID 不能为空");
        }
        String code = CmsSupport.trimRequired(command.getCategoryCode(), "栏目编码不能为空");
        CmsSiteCategoryEntity exists = siteCategoryMapper.selectOne(new LambdaQueryWrapper<CmsSiteCategoryEntity>()
                .eq(CmsSiteCategoryEntity::getTenantId, CmsSupport.currentTenantId())
                .eq(CmsSiteCategoryEntity::getSiteId, command.getSiteId())
                .eq(CmsSiteCategoryEntity::getCategoryCode, code)
                .last("LIMIT 1"));
        Require.isTrue(exists == null || exists.getId().equals(entity.getId()), "栏目编码已存在");
        requireSite(command.getSiteId());
        entity.setSiteId(command.getSiteId());
        entity.setParentId(CmsSupport.defaultParentId(command.getParentId()));
        entity.setCategoryName(CmsSupport.trimRequired(command.getCategoryName(), "栏目名称不能为空"));
        entity.setCategoryCode(code);
        entity.setCategoryType(CmsSupport.enumName(CmsCategoryType.class, command.getCategoryType(), "栏目类型非法"));
        entity.setAccessPath(CmsSupport.trimToNull(command.getAccessPath()));
        entity.setExternalUrl(normalizePublicUrl(command.getExternalUrl(), "栏目外链地址非法"));
        entity.setSort(CmsSupport.defaultSort(command.getSort()));
        entity.setVisibleStatus(CmsSupport.defaultStatus(command.getVisibleStatus()));
        entity.setAccessType(command.getAccessType() == null ? CmsAccessType.PUBLIC.name()
                : CmsSupport.enumName(CmsAccessType.class, command.getAccessType(), "访问类型非法"));
        entity.setRoleCodes(CmsSupport.trimToNull(command.getRoleCodes()));
        entity.setSeoTitle(CmsSupport.trimToNull(command.getSeoTitle()));
        entity.setSeoKeywords(CmsSupport.trimToNull(command.getSeoKeywords()));
        entity.setSeoDescription(CmsSupport.trimToNull(command.getSeoDescription()));
    }

    private void applyContent(CmsContentEntity entity, SaveCmsContentCommand command) {
        Require.notNull(command, "内容保存命令不能为空");
        entity.setTitle(CmsSupport.trimRequired(command.getTitle(), "标题不能为空"));
        entity.setSubtitle(CmsSupport.trimToNull(command.getSubtitle()));
        entity.setSummary(CmsSupport.trimToNull(command.getSummary()));
        entity.setContentType(CmsSupport.enumName(CmsContentType.class, command.getContentType(), "内容类型非法"));
        entity.setCoverFileId(validateImageFile(command.getCoverFileId(), "内容封面文件"));
        entity.setBody(CmsSupport.trimToNull(command.getBody()));
        entity.setExternalUrl(normalizePublicUrl(command.getExternalUrl(), "内容外链地址非法"));
        entity.setAttachmentFileId(validateAnyFile(command.getAttachmentFileId(), "内容附件文件"));
        entity.setVideoFileId(validateVideoFile(command.getVideoFileId(), "内容视频文件"));
        entity.setSource(CmsSupport.trimToNull(command.getSource()));
        entity.setAuthor(CmsSupport.trimToNull(command.getAuthor()));
        if (command.getCategoryId() != null) {
            requireContentCategory(command.getCategoryId());
        }
        entity.setCategoryId(command.getCategoryId());
        entity.setSeoTitle(CmsSupport.trimToNull(command.getSeoTitle()));
        entity.setSeoKeywords(CmsSupport.trimToNull(command.getSeoKeywords()));
        entity.setSeoDescription(CmsSupport.trimToNull(command.getSeoDescription()));
        entity.setPublishTime(command.getPublishTime());
        entity.setOfflineTime(command.getOfflineTime());
    }

    private void applyNavigation(CmsNavigationEntity entity, SaveCmsNavigationCommand command) {
        Require.notNull(command, "导航保存命令不能为空");
        requireSite(command.getSiteId());
        entity.setSiteId(command.getSiteId());
        entity.setNavType(CmsSupport.enumName(CmsNavigationType.class, command.getNavType(), "导航类型非法"));
        entity.setNavName(CmsSupport.trimRequired(command.getNavName(), "导航名称不能为空"));
        entity.setJumpType(CmsSupport.enumName(CmsJumpType.class, command.getJumpType(), "跳转类型非法"));
        if (command.getCategoryId() != null) {
            CmsSiteCategoryEntity category = requireSiteCategory(command.getCategoryId());
            Require.isTrue(command.getSiteId().equals(category.getSiteId()), "导航栏目不属于目标站点");
        }
        if (command.getContentId() != null) {
            requireContent(command.getContentId());
        }
        entity.setCategoryId(command.getCategoryId());
        entity.setContentId(command.getContentId());
        entity.setExternalUrl(normalizePublicUrl(command.getExternalUrl(), "导航外链地址非法"));
        entity.setOpenTarget(command.getOpenTarget() == null ? CmsOpenTarget.SELF.name()
                : CmsSupport.enumName(CmsOpenTarget.class, command.getOpenTarget(), "打开方式非法"));
        entity.setSort(CmsSupport.defaultSort(command.getSort()));
        entity.setStatus(CmsSupport.defaultStatus(command.getStatus()));
    }

    private void applyBanner(CmsBannerEntity entity, SaveCmsBannerCommand command) {
        Require.notNull(command, "Banner 保存命令不能为空");
        requireSite(command.getSiteId());
        entity.setSiteId(command.getSiteId());
        entity.setPosition(CmsSupport.trimRequired(command.getPosition(), "展示位置不能为空"));
        entity.setTitle(CmsSupport.trimRequired(command.getTitle(), "标题不能为空"));
        entity.setSubtitle(CmsSupport.trimToNull(command.getSubtitle()));
        String mediaType = CmsSupport.enumName(CmsBannerMediaType.class, command.getMediaType(), "媒体类型非法");
        entity.setMediaType(mediaType);
        entity.setMediaFileId(CmsBannerMediaType.VIDEO.name().equals(mediaType)
                ? validateVideoFile(command.getMediaFileId(), "Banner 媒体文件")
                : validateImageFile(command.getMediaFileId(), "Banner 媒体文件"));
        entity.setJumpUrl(normalizePublicUrl(command.getJumpUrl(), "Banner 跳转地址非法"));
        entity.setStartTime(command.getStartTime());
        entity.setEndTime(command.getEndTime());
        entity.setSort(CmsSupport.defaultSort(command.getSort()));
        entity.setStatus(CmsSupport.defaultStatus(command.getStatus()));
    }

    private void applyAdvertisement(CmsAdvertisementEntity entity, SaveCmsAdvertisementCommand command, boolean update) {
        Require.notNull(command, "广告保存命令不能为空");
        if (update) {
            Require.notNull(command.getId(), "广告 ID 不能为空");
        }
        requireSite(command.getSiteId());
        String code = CmsSupport.trimRequired(command.getAdCode(), "广告位编码不能为空");
        CmsAdvertisementEntity exists = advertisementMapper.selectOne(new LambdaQueryWrapper<CmsAdvertisementEntity>()
                .eq(CmsAdvertisementEntity::getTenantId, CmsSupport.currentTenantId())
                .eq(CmsAdvertisementEntity::getSiteId, command.getSiteId())
                .eq(CmsAdvertisementEntity::getAdCode, code)
                .last("LIMIT 1"));
        Require.isTrue(exists == null || exists.getId().equals(entity.getId()), "广告位编码已存在");
        entity.setSiteId(command.getSiteId());
        entity.setAdCode(code);
        entity.setAdName(CmsSupport.trimRequired(command.getAdName(), "广告位名称不能为空"));
        entity.setPosition(CmsSupport.trimRequired(command.getPosition(), "广告位位置不能为空"));
        entity.setPositionType(CmsSupport.enumName(CmsAdPositionType.class, command.getPositionType(), "位置类型非法"));
        entity.setSupportedMaterialTypes(CmsSupport.trimToNull(command.getSupportedMaterialTypes()));
        entity.setWidth(command.getWidth());
        entity.setHeight(command.getHeight());
        entity.setRemark(CmsSupport.trimToNull(command.getRemark()));
        entity.setAdType(CmsAdvertisementType.SINGLE_IMAGE.name());
        entity.setMaterialFileId(null);
        entity.setJumpUrl(null);
        entity.setStartTime(null);
        entity.setEndTime(null);
        entity.setSort(CmsSupport.defaultSort(command.getSort()));
        entity.setStatus(CmsSupport.defaultStatus(command.getStatus()));
    }

    private void applyAdDelivery(CmsAdDeliveryEntity entity, SaveCmsAdDeliveryCommand command) {
        Require.notNull(command, "广告投放保存命令不能为空");
        requireSite(command.getSiteId());
        CmsAdvertisementEntity ad = requireAdvertisement(command.getAdId());
        Require.isTrue(command.getSiteId().equals(ad.getSiteId()), "广告投放站点与广告位不一致");
        String materialType = CmsSupport.enumName(CmsAdvertisementType.class, command.getMaterialType(), "物料类型非法");
        validateDeliveryContent(materialType, command);
        entity.setSiteId(command.getSiteId());
        entity.setAdId(command.getAdId());
        entity.setDeliveryName(CmsSupport.trimRequired(command.getDeliveryName(), "投放名称不能为空"));
        entity.setMaterialType(materialType);
        entity.setTitle(CmsSupport.trimToNull(command.getTitle()));
        entity.setTextContent(CmsSupport.trimToNull(command.getTextContent()));
        entity.setRichContent(CmsSupport.trimToNull(command.getRichContent()));
        entity.setHtmlContent(CmsSupport.trimToNull(command.getHtmlContent()));
        entity.setImageFileId(validateDeliveryImage(materialType, command.getImageFileId()));
        entity.setImageFileIds(validateDeliveryImages(materialType, command.getImageFileIds()));
        entity.setVideoFileId(CmsAdvertisementType.VIDEO.name().equals(materialType)
                ? validateVideoFile(command.getVideoFileId(), "广告视频文件")
                : null);
        entity.setCoverFileId(CmsAdvertisementType.VIDEO.name().equals(materialType)
                ? validateImageFile(command.getCoverFileId(), "广告视频封面")
                : null);
        entity.setJumpUrl(normalizePublicUrl(command.getJumpUrl(), "广告跳转地址非法"));
        entity.setOpenTarget(command.getOpenTarget() == null ? CmsOpenTarget.SELF.name()
                : CmsSupport.enumName(CmsOpenTarget.class, command.getOpenTarget(), "打开方式非法"));
        entity.setStartTime(command.getStartTime());
        entity.setEndTime(command.getEndTime());
        if (entity.getStartTime() != null && entity.getEndTime() != null) {
            Require.isTrue(!entity.getEndTime().isBefore(entity.getStartTime()), "结束时间不能早于开始时间");
        }
        entity.setSort(CmsSupport.defaultSort(command.getSort()));
        entity.setStatus(CmsSupport.defaultStatus(command.getStatus()));
    }

    private void validateDeliveryContent(String materialType, SaveCmsAdDeliveryCommand command) {
        if (CmsAdvertisementType.TEXT.name().equals(materialType)) {
            CmsSupport.trimRequired(command.getTextContent(), "文本内容不能为空");
        } else if (CmsAdvertisementType.RICH_TEXT.name().equals(materialType)) {
            CmsSupport.trimRequired(command.getRichContent(), "富文本内容不能为空");
        } else if (CmsAdvertisementType.HTML.name().equals(materialType)) {
            CmsSupport.trimRequired(command.getHtmlContent(), "HTML 内容不能为空");
        }
    }

    private String validateDeliveryImage(String materialType, String imageFileId) {
        if (CmsAdvertisementType.IMAGE.name().equals(materialType)
                || CmsAdvertisementType.SINGLE_IMAGE.name().equals(materialType)) {
            String value = CmsSupport.trimRequired(imageFileId, "广告图片不能为空");
            return validateImageFile(value, "广告图片文件");
        }
        return null;
    }

    private String validateDeliveryImages(String materialType, String imageFileIds) {
        if (!CmsAdvertisementType.MULTI_IMAGE.name().equals(materialType)) {
            return null;
        }
        String value = CmsSupport.trimRequired(imageFileIds, "广告图片组不能为空");
        List<String> normalized = new ArrayList<>();
        for (String item : value.split(",")) {
            String fileId = CmsSupport.trimRequired(item, "广告图片组文件不能为空");
            normalized.add(validateImageFile(fileId, "广告图片组文件"));
        }
        return String.join(",", normalized);
    }

    private void upsertPublish(BatchCmsContentPublishCommand command, Long contentId, Long categoryId) {
        CmsContentPublishEntity entity = publishMapper.selectOne(new LambdaQueryWrapper<CmsContentPublishEntity>()
                .eq(CmsContentPublishEntity::getTenantId, CmsSupport.currentTenantId())
                .eq(CmsContentPublishEntity::getContentId, contentId)
                .eq(CmsContentPublishEntity::getSiteId, command.getSiteId())
                .eq(CmsContentPublishEntity::getCategoryId, categoryId)
                .last("LIMIT 1"));
        boolean create = entity == null;
        if (create) {
            entity = new CmsContentPublishEntity();
            entity.setTenantId(CmsSupport.currentTenantId());
            entity.setContentId(contentId);
            entity.setSiteId(command.getSiteId());
            entity.setCategoryId(categoryId);
        }
        entity.setPublishStatus(command.getScheduledPublishTime() == null
                ? CmsPublishStatus.PUBLISHED.name() : CmsPublishStatus.SCHEDULED.name());
        entity.setPublishTime(command.getPublishTime() == null ? LocalDateTime.now() : command.getPublishTime());
        entity.setScheduledPublishTime(command.getScheduledPublishTime());
        entity.setOfflineTime(command.getOfflineTime());
        entity.setTop(Boolean.TRUE.equals(command.getTop()));
        entity.setTopScope(command.getTopScope() == null ? CmsTopScope.NONE.name()
                : CmsSupport.enumName(CmsTopScope.class, command.getTopScope(), "置顶范围非法"));
        entity.setRecommended(Boolean.TRUE.equals(command.getRecommended()));
        entity.setRecommendationType(command.getRecommendationType() == null ? CmsRecommendationType.NONE.name()
                : CmsSupport.enumName(CmsRecommendationType.class, command.getRecommendationType(), "推荐类型非法"));
        entity.setSort(CmsSupport.defaultSort(command.getSort()));
        if (create) {
            publishMapper.insert(entity);
        } else {
            publishMapper.updateById(entity);
        }
    }

    private void saveContentTags(Long contentId, List<Long> tagIds) {
        contentTagRelMapper.delete(new LambdaQueryWrapper<CmsContentTagRelEntity>()
                .eq(CmsContentTagRelEntity::getTenantId, CmsSupport.currentTenantId())
                .eq(CmsContentTagRelEntity::getContentId, contentId));
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        for (Long tagId : tagIds) {
            requireContentTag(tagId);
            CmsContentTagRelEntity rel = new CmsContentTagRelEntity();
            rel.setTenantId(CmsSupport.currentTenantId());
            rel.setContentId(contentId);
            rel.setTagId(tagId);
            contentTagRelMapper.insert(rel);
        }
    }

    private String normalizePublicUrl(String value, String message) {
        String url = CmsSupport.trimToNull(value);
        if (!StringUtils.hasText(url)) {
            return null;
        }
        String lower = url.toLowerCase(Locale.ROOT);
        boolean safe = lower.startsWith("/")
                || lower.startsWith("#")
                || lower.startsWith("http://")
                || lower.startsWith("https://");
        Require.isTrue(safe && !lower.startsWith("//"), message);
        return url;
    }

    private void saveSiteSettingFromSite(CmsSiteEntity site) {
        SaveCmsSiteSettingCommand command = new SaveCmsSiteSettingCommand();
        command.setSiteId(site.getId());
        command.setSeoTitle(site.getSeoTitle());
        command.setSeoKeywords(site.getSeoKeywords());
        command.setSeoDescription(site.getSeoDescription());
        command.setFooterCopyright(site.getFooterCopyright());
        command.setIcpRecord(site.getIcpRecord());
        command.setContactInfo(site.getContactInfo());
        saveSiteSetting(command);
    }

    private long countBySite(Long siteId) {
        String tenantId = CmsSupport.currentTenantId();
        return siteCategoryMapper.selectCount(new LambdaQueryWrapper<CmsSiteCategoryEntity>().eq(CmsSiteCategoryEntity::getTenantId, tenantId).eq(CmsSiteCategoryEntity::getSiteId, siteId))
                + navigationMapper.selectCount(new LambdaQueryWrapper<CmsNavigationEntity>().eq(CmsNavigationEntity::getTenantId, tenantId).eq(CmsNavigationEntity::getSiteId, siteId))
                + bannerMapper.selectCount(new LambdaQueryWrapper<CmsBannerEntity>().eq(CmsBannerEntity::getTenantId, tenantId).eq(CmsBannerEntity::getSiteId, siteId))
                + advertisementMapper.selectCount(new LambdaQueryWrapper<CmsAdvertisementEntity>().eq(CmsAdvertisementEntity::getTenantId, tenantId).eq(CmsAdvertisementEntity::getSiteId, siteId))
                + adDeliveryMapper.selectCount(new LambdaQueryWrapper<CmsAdDeliveryEntity>().eq(CmsAdDeliveryEntity::getTenantId, tenantId).eq(CmsAdDeliveryEntity::getSiteId, siteId))
                + publishMapper.selectCount(new LambdaQueryWrapper<CmsContentPublishEntity>().eq(CmsContentPublishEntity::getTenantId, tenantId).eq(CmsContentPublishEntity::getSiteId, siteId));
    }

    private QueryWrapper<CmsContentCategoryEntity> contentCategoryWrapper(CmsContentCategoryPageQuery query) {
        String keyword = CmsSupport.trimToNull(query.getKeyword());
        QueryWrapper<CmsContentCategoryEntity> wrapper = new QueryWrapper<CmsContentCategoryEntity>()
                .eq("tenant_id", CmsSupport.currentTenantId())
                .eq(StringUtils.hasText(query.getStatus()), "status", query.getStatus())
                .and(StringUtils.hasText(keyword), w -> w.like("category_code", keyword).or().like("category_name", keyword))
                .orderByAsc("sort").orderByDesc("updated_at");
        return applyDataScope(wrapper, "cms:content-category:list", CONTENT_CATEGORY_SCOPE);
    }

    private QueryWrapper<CmsContentTagEntity> contentTagWrapper(CmsContentTagPageQuery query) {
        String keyword = CmsSupport.trimToNull(query.getKeyword());
        QueryWrapper<CmsContentTagEntity> wrapper = new QueryWrapper<CmsContentTagEntity>()
                .eq("tenant_id", CmsSupport.currentTenantId())
                .eq(StringUtils.hasText(query.getStatus()), "status", query.getStatus())
                .and(StringUtils.hasText(keyword), w -> w.like("tag_code", keyword).or().like("tag_name", keyword))
                .orderByAsc("sort").orderByDesc("updated_at");
        return applyDataScope(wrapper, "cms:content-tag:list", CONTENT_TAG_SCOPE);
    }

    private QueryWrapper<CmsSiteEntity> siteWrapper(CmsSitePageQuery query) {
        String keyword = CmsSupport.trimToNull(query.getKeyword());
        QueryWrapper<CmsSiteEntity> wrapper = new QueryWrapper<CmsSiteEntity>()
                .eq("tenant_id", CmsSupport.currentTenantId())
                .eq(StringUtils.hasText(query.getStatus()), "status", query.getStatus())
                .and(StringUtils.hasText(keyword), w -> w.like("site_code", keyword).or().like("site_name", keyword))
                .orderByDesc("updated_at");
        return applyDataScope(wrapper, "cms:site:list", SITE_SCOPE);
    }

    private QueryWrapper<CmsSiteCategoryEntity> siteCategoryWrapper(CmsSiteCategoryTreeQuery query) {
        QueryWrapper<CmsSiteCategoryEntity> wrapper = new QueryWrapper<CmsSiteCategoryEntity>()
                .eq("tenant_id", CmsSupport.currentTenantId())
                .eq("site_id", query.getSiteId())
                .eq(StringUtils.hasText(query.getStatus()), "visible_status", query.getStatus())
                .orderByAsc("sort").orderByAsc("id");
        return applyDataScope(wrapper, "cms:site-category:list", SITE_CATEGORY_SCOPE);
    }

    private QueryWrapper<CmsContentEntity> contentWrapper(CmsContentPageQuery query) {
        String keyword = CmsSupport.trimToNull(query.getKeyword());
        QueryWrapper<CmsContentEntity> wrapper = new QueryWrapper<CmsContentEntity>()
                .eq("tenant_id", CmsSupport.currentTenantId())
                .eq(query.getCategoryId() != null, "category_id", query.getCategoryId())
                .eq(StringUtils.hasText(query.getStatus()), "status", query.getStatus())
                .eq(StringUtils.hasText(query.getContentType()), "content_type", query.getContentType())
                .and(StringUtils.hasText(keyword), w -> w.like("title", keyword).or().like("summary", keyword))
                .orderByDesc("updated_at");
        return applyDataScope(wrapper, "cms:content:list", CONTENT_SCOPE);
    }

    private QueryWrapper<CmsContentPublishEntity> publishWrapper(CmsContentPublishPageQuery query) {
        String keyword = CmsSupport.trimToNull(query.getKeyword());
        QueryWrapper<CmsContentPublishEntity> wrapper = new QueryWrapper<CmsContentPublishEntity>()
                .eq("tenant_id", CmsSupport.currentTenantId())
                .eq(query.getContentId() != null, "content_id", query.getContentId())
                .eq(query.getSiteId() != null, "site_id", query.getSiteId())
                .eq(query.getCategoryId() != null, "category_id", query.getCategoryId())
                .eq(StringUtils.hasText(query.getStatus()), "publish_status", query.getStatus())
                .orderByDesc("updated_at");
        if (StringUtils.hasText(keyword)) {
            String escaped = escapeSqlLike(keyword);
            String pattern = "'%" + escaped + "%'";
            String tenant = "'" + escapeSqlLiteral(CmsSupport.currentTenantId()) + "'";
            wrapper.and(w -> w.exists("SELECT 1 FROM cms_content c WHERE c.deleted = 0"
                            + " AND c.tenant_id = " + tenant
                            + " AND c.id = cms_content_publish.content_id"
                            + " AND (c.title LIKE " + pattern + " ESCAPE '\\\\' OR c.summary LIKE " + pattern + " ESCAPE '\\\\')")
                    .or()
                    .exists("SELECT 1 FROM cms_site_category sc WHERE sc.deleted = 0"
                            + " AND sc.tenant_id = " + tenant
                            + " AND sc.id = cms_content_publish.category_id"
                            + " AND sc.category_name LIKE " + pattern + " ESCAPE '\\\\'"));
        }
        return applyDataScope(wrapper, "cms:publish:list", PUBLISH_SCOPE);
    }

    private QueryWrapper<CmsNavigationEntity> navigationWrapper(CmsNavigationPageQuery query) {
        String keyword = CmsSupport.trimToNull(query.getKeyword());
        QueryWrapper<CmsNavigationEntity> wrapper = new QueryWrapper<CmsNavigationEntity>()
                .eq("tenant_id", CmsSupport.currentTenantId())
                .eq(query.getSiteId() != null, "site_id", query.getSiteId())
                .eq(StringUtils.hasText(query.getNavType()), "nav_type", query.getNavType())
                .eq(StringUtils.hasText(query.getStatus()), "status", query.getStatus())
                .and(StringUtils.hasText(keyword), w -> w.like("nav_name", keyword).or().like("external_url", keyword))
                .orderByAsc("sort");
        return applyDataScope(wrapper, "cms:navigation:list", NAVIGATION_SCOPE);
    }

    private QueryWrapper<CmsBannerEntity> bannerWrapper(CmsBannerPageQuery query) {
        QueryWrapper<CmsBannerEntity> wrapper = new QueryWrapper<CmsBannerEntity>()
                .eq("tenant_id", CmsSupport.currentTenantId())
                .eq(query.getSiteId() != null, "site_id", query.getSiteId())
                .eq(StringUtils.hasText(query.getPosition()), "position", query.getPosition())
                .eq(StringUtils.hasText(query.getStatus()), "status", query.getStatus())
                .orderByAsc("sort");
        return applyDataScope(wrapper, "cms:banner:list", BANNER_SCOPE);
    }

    private QueryWrapper<CmsAdvertisementEntity> advertisementWrapper(CmsAdvertisementPageQuery query) {
        String keyword = CmsSupport.trimToNull(query.getKeyword());
        QueryWrapper<CmsAdvertisementEntity> wrapper = new QueryWrapper<CmsAdvertisementEntity>()
                .eq("tenant_id", CmsSupport.currentTenantId())
                .eq(query.getSiteId() != null, "site_id", query.getSiteId())
                .eq(StringUtils.hasText(query.getPosition()), "position", query.getPosition())
                .eq(StringUtils.hasText(query.getStatus()), "status", query.getStatus())
                .and(StringUtils.hasText(keyword), w -> w.like("ad_code", keyword).or().like("ad_name", keyword))
                .orderByAsc("sort");
        return applyDataScope(wrapper, "cms:advertisement:list", ADVERTISEMENT_SCOPE);
    }

    private QueryWrapper<CmsAdDeliveryEntity> adDeliveryWrapper(CmsAdDeliveryPageQuery query) {
        String keyword = CmsSupport.trimToNull(query.getKeyword());
        QueryWrapper<CmsAdDeliveryEntity> wrapper = new QueryWrapper<CmsAdDeliveryEntity>()
                .eq("tenant_id", CmsSupport.currentTenantId())
                .eq(query.getSiteId() != null, "site_id", query.getSiteId())
                .eq(query.getAdId() != null, "ad_id", query.getAdId())
                .eq(StringUtils.hasText(query.getMaterialType()), "material_type", query.getMaterialType())
                .and(StringUtils.hasText(keyword), w -> w.like("delivery_name", keyword).or().like("title", keyword))
                .orderByAsc("sort")
                .orderByDesc("updated_at");
        return applyDataScope(wrapper, "cms:ad-delivery:list", AD_DELIVERY_SCOPE);
    }

    private CmsContentCategoryEntity requireContentCategory(Long id) {
        Require.notNull(id, "分类 ID 不能为空");
        CmsContentCategoryEntity entity = contentCategoryMapper.selectOne(scopedById(id, "cms:content-category:list", CONTENT_CATEGORY_SCOPE));
        Require.notNull(entity, "分类不存在");
        return entity;
    }

    private CmsContentTagEntity requireContentTag(Long id) {
        Require.notNull(id, "标签 ID 不能为空");
        CmsContentTagEntity entity = contentTagMapper.selectOne(scopedById(id, "cms:content-tag:list", CONTENT_TAG_SCOPE));
        Require.notNull(entity, "标签不存在");
        return entity;
    }

    private CmsSiteEntity requireSite(Long id) {
        Require.notNull(id, "站点 ID 不能为空");
        CmsSiteEntity entity = siteMapper.selectOne(scopedById(id, "cms:site:list", SITE_SCOPE));
        Require.notNull(entity, "站点不存在");
        return entity;
    }

    private CmsSiteCategoryEntity requireSiteCategory(Long id) {
        Require.notNull(id, "栏目 ID 不能为空");
        CmsSiteCategoryEntity entity = siteCategoryMapper.selectOne(scopedById(id, "cms:site-category:list", SITE_CATEGORY_SCOPE));
        Require.notNull(entity, "栏目不存在");
        return entity;
    }

    private CmsContentEntity requireContent(Long id) {
        Require.notNull(id, "内容 ID 不能为空");
        CmsContentEntity entity = contentMapper.selectOne(scopedById(id, "cms:content:list", CONTENT_SCOPE));
        Require.notNull(entity, "内容不存在");
        return entity;
    }

    private CmsContentPublishEntity requirePublish(Long id) {
        Require.notNull(id, "发布关系 ID 不能为空");
        CmsContentPublishEntity entity = publishMapper.selectOne(scopedById(id, "cms:publish:list", PUBLISH_SCOPE));
        Require.notNull(entity, "发布关系不存在");
        return entity;
    }

    private CmsNavigationEntity requireNavigation(Long id) {
        Require.notNull(id, "导航 ID 不能为空");
        CmsNavigationEntity entity = navigationMapper.selectOne(scopedById(id, "cms:navigation:list", NAVIGATION_SCOPE));
        Require.notNull(entity, "导航不存在");
        return entity;
    }

    private CmsBannerEntity requireBanner(Long id) {
        Require.notNull(id, "Banner ID 不能为空");
        CmsBannerEntity entity = bannerMapper.selectOne(scopedById(id, "cms:banner:list", BANNER_SCOPE));
        Require.notNull(entity, "Banner 不存在");
        return entity;
    }

    private CmsAdvertisementEntity requireAdvertisement(Long id) {
        Require.notNull(id, "广告 ID 不能为空");
        CmsAdvertisementEntity entity = advertisementMapper.selectOne(scopedById(id, "cms:advertisement:list", ADVERTISEMENT_SCOPE));
        Require.notNull(entity, "广告不存在");
        return entity;
    }

    private CmsAdDeliveryEntity requireAdDelivery(Long id) {
        Require.notNull(id, "广告投放 ID 不能为空");
        CmsAdDeliveryEntity entity = adDeliveryMapper.selectOne(scopedById(id, "cms:ad-delivery:list", AD_DELIVERY_SCOPE));
        Require.notNull(entity, "广告投放不存在");
        return entity;
    }

    private static DataScopeMapping cmsScope(String tableName) {
        return DataScopeMapping.builder()
                .tableName(tableName)
                .selfField("created_by")
                .orgField("org_id")
                .tenantField("tenant_id")
                .build();
    }

    private <T> QueryWrapper<T> scopedById(Long id, String resourceCode, DataScopeMapping mapping) {
        QueryWrapper<T> wrapper = new QueryWrapper<T>()
                .eq("tenant_id", CmsSupport.currentTenantId())
                .eq("id", id)
                .last("LIMIT 1");
        return applyDataScope(wrapper, resourceCode, mapping);
    }

    private <T> QueryWrapper<T> applyDataScope(QueryWrapper<T> wrapper, String resourceCode, DataScopeMapping mapping) {
        DataScopeApplier dataScopeApplier = dataScopeApplierProvider.getIfAvailable();
        if (dataScopeApplier != null) {
            dataScopeApplier.apply(wrapper, resourceCode, mapping);
        }
        return wrapper;
    }

    private String escapeSqlLike(String value) {
        return escapeSqlLiteral(value)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private String escapeSqlLiteral(String value) {
        return value == null ? "" : value.replace("'", "''");
    }

    private String validateImageFile(String value, String fieldName) {
        return validateFile(value, fieldName, "image/");
    }

    private String validateVideoFile(String value, String fieldName) {
        return validateFile(value, fieldName, "video/");
    }

    private String validateAnyFile(String value, String fieldName) {
        return validateFile(value, fieldName, null);
    }

    private String validateFile(String value, String fieldName, String contentTypePrefix) {
        String normalized = CmsSupport.trimToNull(value);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        Long fileId = parseFileId(normalized, fieldName);
        FileApi fileApi = fileApiProvider.getIfAvailable();
        Require.notNull(fileApi, fieldName + "能力不可用");
        R<FileRecordVO> response = fileApi.get(fileId);
        Require.isTrue(response != null && response.isSuccess() && response.getData() != null, fieldName + "不存在或不可见");
        FileRecordVO file = response.getData();
        Require.isTrue(FileRecordStatus.COMPLETED.value() == (file.getStatus() == null ? -1 : file.getStatus()), fieldName + "未上传完成");
        Require.isTrue(file.getArchived() == null || file.getArchived() == 0, fieldName + "已归档");
        if (StringUtils.hasText(contentTypePrefix)) {
            String contentType = CmsSupport.trimToNull(file.getContentType());
            Require.isTrue(contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith(contentTypePrefix), fieldName + "类型不匹配");
        }
        return normalized;
    }

    private Long parseFileId(String value, String fieldName) {
        String raw = value.startsWith("mango-file:") ? value.substring("mango-file:".length()) : value;
        Require.isTrue(raw.matches("\\d+"), fieldName + "格式非法");
        return Long.valueOf(raw);
    }

    private List<CmsSiteCategoryVO> buildCategoryTree(List<CmsSiteCategoryVO> items) {
        Map<Long, CmsSiteCategoryVO> map = new HashMap<>();
        List<CmsSiteCategoryVO> roots = new ArrayList<>();
        items.forEach(item -> map.put(item.getId(), item));
        for (CmsSiteCategoryVO item : items) {
            Long parentId = item.getParentId() == null ? CmsSupport.ROOT_PARENT_ID : item.getParentId();
            if (parentId == CmsSupport.ROOT_PARENT_ID || !map.containsKey(parentId)) {
                roots.add(item);
            } else {
                map.get(parentId).getChildren().add(item);
            }
        }
        Comparator<CmsSiteCategoryVO> comparator = Comparator.comparing(vo -> vo.getSort() == null ? 0 : vo.getSort());
        roots.sort(comparator);
        map.values().forEach(item -> item.getChildren().sort(comparator));
        return roots;
    }

    private List<CmsContentCategoryVO> buildContentCategoryTree(List<CmsContentCategoryVO> items) {
        Map<Long, CmsContentCategoryVO> map = new HashMap<>();
        List<CmsContentCategoryVO> roots = new ArrayList<>();
        items.forEach(item -> map.put(item.getId(), item));
        for (CmsContentCategoryVO item : items) {
            Long parentId = item.getParentId() == null ? CmsSupport.ROOT_PARENT_ID : item.getParentId();
            if (parentId == CmsSupport.ROOT_PARENT_ID || !map.containsKey(parentId)) {
                roots.add(item);
            } else {
                map.get(parentId).getChildren().add(item);
            }
        }
        Comparator<CmsContentCategoryVO> comparator = Comparator.comparing(vo -> vo.getSort() == null ? 0 : vo.getSort());
        roots.sort(comparator);
        map.values().forEach(item -> item.getChildren().sort(comparator));
        return roots;
    }

    private CmsSiteVO toSiteVO(CmsSiteEntity e) {
        CmsSiteVO vo = new CmsSiteVO();
        vo.setId(e.getId());
        vo.setSiteName(e.getSiteName());
        vo.setSiteCode(e.getSiteCode());
        vo.setLogoFileId(e.getLogoFileId());
        vo.setDescription(e.getDescription());
        vo.setDomain(e.getDomain());
        vo.setStatus(e.getStatus());
        vo.setDefaultLanguage(e.getDefaultLanguage());
        vo.setSeoTitle(e.getSeoTitle());
        vo.setSeoKeywords(e.getSeoKeywords());
        vo.setSeoDescription(e.getSeoDescription());
        vo.setFooterCopyright(e.getFooterCopyright());
        vo.setIcpRecord(e.getIcpRecord());
        vo.setContactInfo(e.getContactInfo());
        vo.setOrgId(e.getOrgId());
        vo.setCreatedAt(e.getCreatedAt());
        vo.setUpdatedAt(e.getUpdatedAt());
        return vo;
    }

    private CmsSiteCategoryVO toSiteCategoryVO(CmsSiteCategoryEntity e) {
        CmsSiteCategoryVO vo = new CmsSiteCategoryVO();
        vo.setId(e.getId());
        vo.setSiteId(e.getSiteId());
        vo.setParentId(e.getParentId());
        vo.setCategoryName(e.getCategoryName());
        vo.setCategoryCode(e.getCategoryCode());
        vo.setCategoryType(e.getCategoryType());
        vo.setAccessPath(e.getAccessPath());
        vo.setExternalUrl(e.getExternalUrl());
        vo.setSort(e.getSort());
        vo.setVisibleStatus(e.getVisibleStatus());
        vo.setAccessType(e.getAccessType());
        vo.setRoleCodes(e.getRoleCodes());
        vo.setSeoTitle(e.getSeoTitle());
        vo.setSeoKeywords(e.getSeoKeywords());
        vo.setSeoDescription(e.getSeoDescription());
        vo.setCreatedAt(e.getCreatedAt());
        vo.setUpdatedAt(e.getUpdatedAt());
        return vo;
    }

    private CmsContentVO toContentVO(CmsContentEntity e) {
        CmsContentVO vo = new CmsContentVO();
        vo.setId(e.getId());
        vo.setTitle(e.getTitle());
        vo.setSubtitle(e.getSubtitle());
        vo.setSummary(e.getSummary());
        vo.setContentType(e.getContentType());
        vo.setCoverFileId(e.getCoverFileId());
        vo.setBody(e.getBody());
        vo.setExternalUrl(e.getExternalUrl());
        vo.setAttachmentFileId(e.getAttachmentFileId());
        vo.setVideoFileId(e.getVideoFileId());
        vo.setSource(e.getSource());
        vo.setAuthor(e.getAuthor());
        vo.setCategoryId(e.getCategoryId());
        if (e.getCategoryId() != null) {
            CmsContentCategoryEntity category = contentCategoryMapper.selectById(e.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getCategoryName());
            }
        }
        vo.setTags(loadTags(e.getId()));
        vo.setSeoTitle(e.getSeoTitle());
        vo.setSeoKeywords(e.getSeoKeywords());
        vo.setSeoDescription(e.getSeoDescription());
        vo.setStatus(e.getStatus());
        vo.setPublishTime(e.getPublishTime());
        vo.setOfflineTime(e.getOfflineTime());
        vo.setReviewComment(e.getReviewComment());
        vo.setOrgId(e.getOrgId());
        vo.setCreatedAt(e.getCreatedAt());
        vo.setUpdatedAt(e.getUpdatedAt());
        return vo;
    }

    private List<CmsContentTagVO> loadTags(Long contentId) {
        List<CmsContentTagRelEntity> rels = contentTagRelMapper.selectList(new LambdaQueryWrapper<CmsContentTagRelEntity>()
                .eq(CmsContentTagRelEntity::getTenantId, CmsSupport.currentTenantId())
                .eq(CmsContentTagRelEntity::getContentId, contentId));
        return rels.stream()
                .map(rel -> contentTagMapper.selectById(rel.getTagId()))
                .filter(tag -> tag != null && CmsSupport.currentTenantId().equals(tag.getTenantId()))
                .map(this::toContentTagVO)
                .toList();
    }

    private CmsContentCategoryVO toContentCategoryVO(CmsContentCategoryEntity e) {
        CmsContentCategoryVO vo = new CmsContentCategoryVO();
        vo.setId(e.getId());
        vo.setParentId(e.getParentId());
        vo.setCategoryCode(e.getCategoryCode());
        vo.setCategoryName(e.getCategoryName());
        vo.setSort(e.getSort());
        vo.setStatus(e.getStatus());
        vo.setRemark(e.getRemark());
        vo.setCreatedAt(e.getCreatedAt());
        vo.setUpdatedAt(e.getUpdatedAt());
        return vo;
    }

    private CmsContentTagVO toContentTagVO(CmsContentTagEntity e) {
        CmsContentTagVO vo = new CmsContentTagVO();
        vo.setId(e.getId());
        vo.setTagCode(e.getTagCode());
        vo.setTagName(e.getTagName());
        vo.setSort(e.getSort());
        vo.setStatus(e.getStatus());
        vo.setRemark(e.getRemark());
        vo.setCreatedAt(e.getCreatedAt());
        vo.setUpdatedAt(e.getUpdatedAt());
        return vo;
    }

    private CmsContentPublishVO toPublishVO(CmsContentPublishEntity e) {
        CmsContentPublishVO vo = new CmsContentPublishVO();
        vo.setId(e.getId());
        vo.setContentId(e.getContentId());
        CmsContentEntity content = contentMapper.selectById(e.getContentId());
        if (content != null) {
            vo.setContentTitle(content.getTitle());
        }
        vo.setSiteId(e.getSiteId());
        CmsSiteEntity site = siteMapper.selectById(e.getSiteId());
        if (site != null) {
            vo.setSiteName(site.getSiteName());
        }
        vo.setCategoryId(e.getCategoryId());
        CmsSiteCategoryEntity category = siteCategoryMapper.selectById(e.getCategoryId());
        if (category != null) {
            vo.setCategoryName(category.getCategoryName());
        }
        vo.setPublishStatus(e.getPublishStatus());
        vo.setPublishTime(e.getPublishTime());
        vo.setScheduledPublishTime(e.getScheduledPublishTime());
        vo.setOfflineTime(e.getOfflineTime());
        vo.setTop(e.getTop());
        vo.setTopScope(e.getTopScope());
        vo.setRecommended(e.getRecommended());
        vo.setRecommendationType(e.getRecommendationType());
        vo.setSort(e.getSort());
        vo.setCreatedAt(e.getCreatedAt());
        vo.setUpdatedAt(e.getUpdatedAt());
        return vo;
    }

    private CmsNavigationVO toNavigationVO(CmsNavigationEntity e) {
        CmsNavigationVO vo = new CmsNavigationVO();
        vo.setId(e.getId());
        vo.setSiteId(e.getSiteId());
        vo.setNavType(e.getNavType());
        vo.setNavName(e.getNavName());
        vo.setJumpType(e.getJumpType());
        vo.setCategoryId(e.getCategoryId());
        vo.setContentId(e.getContentId());
        vo.setExternalUrl(e.getExternalUrl());
        vo.setOpenTarget(e.getOpenTarget());
        vo.setSort(e.getSort());
        vo.setStatus(e.getStatus());
        vo.setCreatedAt(e.getCreatedAt());
        vo.setUpdatedAt(e.getUpdatedAt());
        return vo;
    }

    private CmsBannerVO toBannerVO(CmsBannerEntity e) {
        CmsBannerVO vo = new CmsBannerVO();
        vo.setId(e.getId());
        vo.setSiteId(e.getSiteId());
        vo.setPosition(e.getPosition());
        vo.setTitle(e.getTitle());
        vo.setSubtitle(e.getSubtitle());
        vo.setMediaType(e.getMediaType());
        vo.setMediaFileId(e.getMediaFileId());
        vo.setJumpUrl(e.getJumpUrl());
        vo.setStartTime(e.getStartTime());
        vo.setEndTime(e.getEndTime());
        vo.setSort(e.getSort());
        vo.setStatus(e.getStatus());
        vo.setCreatedAt(e.getCreatedAt());
        vo.setUpdatedAt(e.getUpdatedAt());
        return vo;
    }

    private CmsAdvertisementVO toAdvertisementVO(CmsAdvertisementEntity e) {
        CmsAdvertisementVO vo = new CmsAdvertisementVO();
        vo.setId(e.getId());
        vo.setSiteId(e.getSiteId());
        vo.setAdCode(e.getAdCode());
        vo.setAdName(e.getAdName());
        vo.setPosition(e.getPosition());
        vo.setPositionType(e.getPositionType());
        vo.setSupportedMaterialTypes(e.getSupportedMaterialTypes());
        vo.setWidth(e.getWidth());
        vo.setHeight(e.getHeight());
        vo.setRemark(e.getRemark());
        vo.setSort(e.getSort());
        vo.setStatus(e.getStatus());
        vo.setCreatedAt(e.getCreatedAt());
        vo.setUpdatedAt(e.getUpdatedAt());
        return vo;
    }

    private CmsAdDeliveryVO toAdDeliveryVO(CmsAdDeliveryEntity e) {
        CmsAdDeliveryVO vo = new CmsAdDeliveryVO();
        vo.setId(e.getId());
        vo.setSiteId(e.getSiteId());
        vo.setAdId(e.getAdId());
        CmsAdvertisementEntity ad = advertisementMapper.selectById(e.getAdId());
        if (ad != null && CmsSupport.currentTenantId().equals(ad.getTenantId())) {
            vo.setAdName(ad.getAdName());
            vo.setAdCode(ad.getAdCode());
            vo.setPosition(ad.getPosition());
            vo.setPositionType(ad.getPositionType());
        }
        vo.setDeliveryName(e.getDeliveryName());
        vo.setMaterialType(e.getMaterialType());
        vo.setTitle(e.getTitle());
        vo.setTextContent(e.getTextContent());
        vo.setRichContent(e.getRichContent());
        vo.setHtmlContent(e.getHtmlContent());
        vo.setImageFileId(e.getImageFileId());
        vo.setImageFileIds(e.getImageFileIds());
        vo.setVideoFileId(e.getVideoFileId());
        vo.setCoverFileId(e.getCoverFileId());
        vo.setJumpUrl(e.getJumpUrl());
        vo.setOpenTarget(e.getOpenTarget());
        vo.setStartTime(e.getStartTime());
        vo.setEndTime(e.getEndTime());
        vo.setSort(e.getSort());
        vo.setStatus(e.getStatus());
        vo.setCreatedAt(e.getCreatedAt());
        vo.setUpdatedAt(e.getUpdatedAt());
        return vo;
    }

    private CmsSiteSettingVO toSiteSettingVO(CmsSiteSettingEntity e) {
        CmsSiteSettingVO vo = new CmsSiteSettingVO();
        vo.setId(e.getId());
        vo.setSiteId(e.getSiteId());
        vo.setSeoTitle(e.getSeoTitle());
        vo.setSeoKeywords(e.getSeoKeywords());
        vo.setSeoDescription(e.getSeoDescription());
        vo.setFooterCopyright(e.getFooterCopyright());
        vo.setIcpRecord(e.getIcpRecord());
        vo.setContactInfo(e.getContactInfo());
        return vo;
    }
}
