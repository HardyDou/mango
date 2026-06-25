package io.mango.cms.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.cms.api.enums.CmsAdPositionType;
import io.mango.cms.api.enums.CmsAdvertisementType;
import io.mango.cms.api.enums.CmsAccessType;
import io.mango.cms.api.enums.CmsBannerMediaType;
import io.mango.cms.api.enums.CmsContentStatus;
import io.mango.cms.api.enums.CmsPublishStatus;
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
import io.mango.cms.core.entity.CmsAdDeliveryEntity;
import io.mango.cms.core.entity.CmsAdvertisementEntity;
import io.mango.cms.core.entity.CmsBannerEntity;
import io.mango.cms.core.entity.CmsContentEntity;
import io.mango.cms.core.entity.CmsContentPublishEntity;
import io.mango.cms.core.entity.CmsNavigationEntity;
import io.mango.cms.core.entity.CmsSiteCategoryEntity;
import io.mango.cms.core.entity.CmsSiteEntity;
import io.mango.cms.core.mapper.CmsAdDeliveryMapper;
import io.mango.cms.core.mapper.CmsAdvertisementMapper;
import io.mango.cms.core.mapper.CmsBannerMapper;
import io.mango.cms.core.mapper.CmsContentMapper;
import io.mango.cms.core.mapper.CmsContentPublishMapper;
import io.mango.cms.core.mapper.CmsNavigationMapper;
import io.mango.cms.core.mapper.CmsSiteCategoryMapper;
import io.mango.cms.core.mapper.CmsSiteMapper;
import io.mango.cms.core.service.ICmsSiteService;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.file.api.FileApi;
import io.mango.file.api.vo.FileDownloadVO;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CmsSiteService implements ICmsSiteService {

    private static final Logger LOGGER = Logger.getLogger(CmsSiteService.class.getName());

    private final CmsSiteMapper siteMapper;
    private final CmsSiteCategoryMapper siteCategoryMapper;
    private final CmsNavigationMapper navigationMapper;
    private final CmsBannerMapper bannerMapper;
    private final CmsAdvertisementMapper advertisementMapper;
    private final CmsAdDeliveryMapper adDeliveryMapper;
    private final CmsContentMapper contentMapper;
    private final CmsContentPublishMapper publishMapper;
    private final ObjectProvider<FileApi> fileApiProvider;
    @Override
    public R<SiteResolveVO> resolveSite(SiteResolveQuery query) {
        return R.ok(toResolveVO(resolveSiteEntity(query == null ? new SiteResolveQuery() : query)));
    }

    @Override
    public R<SiteVO> detailSite(SiteResolveQuery query) {
        CmsSiteEntity site = resolveSiteEntity(query == null ? new SiteResolveQuery() : query);
        return R.ok(toPublicSiteVO(site));
    }

    @Override
    public R<List<SiteCategoryVO>> treeCategories(SiteCategoryQuery query) {
        CmsSiteEntity site = resolveSiteEntity(toResolveQuery(query));
        List<SiteCategoryVO> tree = siteCategoryMapper.selectList(new LambdaQueryWrapper<CmsSiteCategoryEntity>()
                        .eq(CmsSiteCategoryEntity::getTenantId, site.getTenantId())
                        .eq(CmsSiteCategoryEntity::getSiteId, site.getId())
                        .eq(CmsSiteCategoryEntity::getVisibleStatus, CmsSupport.ENABLED)
                        .eq(CmsSiteCategoryEntity::getAccessType, CmsAccessType.PUBLIC.name())
                        .orderByAsc(CmsSiteCategoryEntity::getSort)
                        .orderByAsc(CmsSiteCategoryEntity::getId))
                .stream()
                .map(this::toPublicSiteCategoryVO)
                .toList();
        return R.ok(buildCategoryTree(tree.stream().filter(this::publicCategory).toList()));
    }

    @Override
    public R<List<SiteNavigationVO>> listNavigations(SiteNavigationQuery query) {
        SiteNavigationQuery resolved = query == null ? new SiteNavigationQuery() : query;
        CmsSiteEntity site = resolveSiteEntity(toResolveQuery(resolved));
        List<CmsNavigationEntity> records = navigationMapper.selectList(new LambdaQueryWrapper<CmsNavigationEntity>()
                .eq(CmsNavigationEntity::getTenantId, site.getTenantId())
                .eq(CmsNavigationEntity::getSiteId, site.getId())
                .eq(StringUtils.hasText(resolved.getNavType()), CmsNavigationEntity::getNavType, resolved.getNavType())
                .eq(CmsNavigationEntity::getStatus, CmsSupport.ENABLED)
                .orderByAsc(CmsNavigationEntity::getSort));
        return R.ok(records.stream().map(this::toPublicNavigationVO).toList());
    }

    @Override
    public R<List<SiteBannerVO>> listBanners(SiteBannerQuery query) {
        SiteBannerQuery resolved = query == null ? new SiteBannerQuery() : query;
        CmsSiteEntity site = resolveSiteEntity(toResolveQuery(resolved));
        LocalDateTime now = LocalDateTime.now();
        List<SiteBannerVO> deliveryBanners = listEffectiveDeliveries(site, resolved.getPosition(), CmsAdPositionType.BANNER.name(), now)
                .stream()
                .map(this::toPublicBannerVO)
                .toList();
        if (!deliveryBanners.isEmpty()) {
            return R.ok(deliveryBanners);
        }
        List<CmsBannerEntity> records = bannerMapper.selectList(new LambdaQueryWrapper<CmsBannerEntity>()
                .eq(CmsBannerEntity::getTenantId, site.getTenantId())
                .eq(CmsBannerEntity::getSiteId, site.getId())
                .eq(StringUtils.hasText(resolved.getPosition()), CmsBannerEntity::getPosition, resolved.getPosition())
                .eq(CmsBannerEntity::getStatus, CmsSupport.ENABLED)
                .orderByAsc(CmsBannerEntity::getSort));
        return R.ok(records.stream()
                .filter(item -> CmsSupport.isEffective(item.getStartTime(), item.getEndTime(), now))
                .map(this::toPublicBannerVO)
                .toList());
    }

    @Override
    public R<List<SiteAdvertisementVO>> listAdvertisements(SiteAdvertisementQuery query) {
        SiteAdvertisementQuery resolved = query == null ? new SiteAdvertisementQuery() : query;
        CmsSiteEntity site = resolveSiteEntity(toResolveQuery(resolved));
        LocalDateTime now = LocalDateTime.now();
        List<SiteAdvertisementVO> deliveries = listEffectiveDeliveries(site, resolved.getPosition(), null, now)
                .stream()
                .map(item -> toPublicAdvertisementVO(site, item))
                .toList();
        if (!deliveries.isEmpty()) {
            return R.ok(deliveries);
        }
        List<CmsAdvertisementEntity> records = advertisementMapper.selectList(new LambdaQueryWrapper<CmsAdvertisementEntity>()
                .eq(CmsAdvertisementEntity::getTenantId, site.getTenantId())
                .eq(CmsAdvertisementEntity::getSiteId, site.getId())
                .eq(StringUtils.hasText(resolved.getPosition()), CmsAdvertisementEntity::getPosition, resolved.getPosition())
                .eq(CmsAdvertisementEntity::getStatus, CmsSupport.ENABLED)
                .orderByAsc(CmsAdvertisementEntity::getSort));
        return R.ok(records.stream()
                .filter(item -> CmsSupport.isEffective(item.getStartTime(), item.getEndTime(), now))
                .map(this::toPublicAdvertisementVO)
                .toList());
    }

    private List<AdDeliveryWithPosition> listEffectiveDeliveries(CmsSiteEntity site,
                                                                 String position,
                                                                 String positionType,
                                                                 LocalDateTime now) {
        List<CmsAdvertisementEntity> positions = advertisementMapper.selectList(new LambdaQueryWrapper<CmsAdvertisementEntity>()
                .eq(CmsAdvertisementEntity::getTenantId, site.getTenantId())
                .eq(CmsAdvertisementEntity::getSiteId, site.getId())
                .eq(StringUtils.hasText(position), CmsAdvertisementEntity::getPosition, position)
                .eq(StringUtils.hasText(positionType), CmsAdvertisementEntity::getPositionType, positionType)
                .eq(CmsAdvertisementEntity::getStatus, CmsSupport.ENABLED)
                .orderByAsc(CmsAdvertisementEntity::getSort));
        if (positions.isEmpty()) {
            return List.of();
        }
        List<Long> adIds = positions.stream().map(CmsAdvertisementEntity::getId).toList();
        Map<Long, CmsAdvertisementEntity> positionMap = positions.stream()
                .collect(Collectors.toMap(CmsAdvertisementEntity::getId, item -> item));
        return adDeliveryMapper.selectList(new LambdaQueryWrapper<CmsAdDeliveryEntity>()
                        .eq(CmsAdDeliveryEntity::getTenantId, site.getTenantId())
                        .eq(CmsAdDeliveryEntity::getSiteId, site.getId())
                        .in(CmsAdDeliveryEntity::getAdId, adIds)
                        .eq(CmsAdDeliveryEntity::getStatus, CmsSupport.ENABLED)
                        .orderByAsc(CmsAdDeliveryEntity::getSort)
                        .orderByDesc(CmsAdDeliveryEntity::getUpdatedAt))
                .stream()
                .filter(item -> CmsSupport.isEffective(item.getStartTime(), item.getEndTime(), now))
                .map(item -> new AdDeliveryWithPosition(item, positionMap.get(item.getAdId())))
                .filter(item -> item.position() != null)
                .toList();
    }

    @Override
    public R<PageResult<SiteContentVO>> pageContents(SiteContentPageQuery query) {
        SiteContentPageQuery resolved = query == null ? new SiteContentPageQuery() : query;
        CmsSiteEntity site = resolveSiteEntity(toResolveQuery(resolved));
        LocalDateTime now = LocalDateTime.now();
        IPage<CmsContentEntity> page = contentMapper.selectPublicPage(new Page<>(resolved.getPage(), resolved.getSize()),
                site.getTenantId(),
                site.getId(),
                resolved.getCategoryId(),
                resolved.getRecommendationType(),
                resolved.getKeyword(),
                CmsPublishStatus.PUBLISHED.name(),
                CmsPublishStatus.SCHEDULED.name(),
                CmsContentStatus.PUBLISHED.name(),
                now);
        List<SiteContentVO> list = page.getRecords().stream()
                .map(content -> toPublicContentVO(site, content, findEffectivePublish(site, content.getId(), resolved.getCategoryId(), now)))
                .toList();
        return R.ok(PageResult.of(list, page.getTotal(), page.getCurrent(), page.getSize()));
    }

    @Override
    public R<SiteContentVO> detailContent(SiteContentDetailQuery query) {
        Require.notNull(query, "内容详情查询不能为空");
        CmsSiteEntity site = resolveSiteEntity(toResolveQuery(query));
        CmsContentEntity content = contentMapper.selectById(query.getContentId());
        Require.notNull(content, "内容不存在");
        Require.isTrue(publicContent(content, site.getTenantId(), LocalDateTime.now()), "内容不存在");
        CmsContentPublishEntity publish = findEffectivePublish(site, content.getId(), query.getCategoryId(), LocalDateTime.now());
        Require.notNull(publish, "内容不存在");
        return R.ok(toPublicContentVO(site, content, publish));
    }

    @Override
    public FileDownloadVO publicFile(Long id, SiteResolveQuery query) {
        Require.notNull(id, "文件ID不能为空");
        CmsSiteEntity site = resolveSiteEntity(query == null ? new SiteResolveQuery() : query);
        Require.isTrue(isPublicSiteFile(site, String.valueOf(id)), "文件不存在");
        FileApi fileApi = fileApiProvider.getIfAvailable();
        Require.notNull(fileApi, "文件服务不可用");
        return withTenantContext(site.getTenantId(), () -> fileApi.downloadForService(id));
    }

    private CmsSiteEntity resolveSiteEntity(SiteResolveQuery query) {
        Require.notNull(query, "站点查询不能为空");
        Require.isTrue(StringUtils.hasText(query.getSiteCode()) || StringUtils.hasText(query.getDomain()), "站点编码或域名不能为空");
        String tenantId = CmsSupport.currentTenantIdOrNull();
        validateResolveScope(query, tenantId);
        LambdaQueryWrapper<CmsSiteEntity> wrapper = new LambdaQueryWrapper<CmsSiteEntity>()
                .eq(StringUtils.hasText(tenantId), CmsSiteEntity::getTenantId, tenantId)
                .eq(CmsSiteEntity::getStatus, CmsSupport.ENABLED);
        if (StringUtils.hasText(query.getDomain())) {
            wrapper.eq(CmsSiteEntity::getDomain, query.getDomain().trim());
        } else if (StringUtils.hasText(query.getSiteCode())) {
            wrapper.eq(CmsSiteEntity::getSiteCode, query.getSiteCode().trim());
        }
        List<CmsSiteEntity> sites = siteMapper.selectList(wrapper);
        Require.isTrue(sites.size() == 1, "站点不存在或不唯一");
        return sites.get(0);
    }

    static void validateResolveScope(SiteResolveQuery query, String tenantId) {
        if (!StringUtils.hasText(tenantId)) {
            Require.isTrue(StringUtils.hasText(query.getDomain()), "匿名站点解析必须提供域名");
            Require.isTrue(!StringUtils.hasText(query.getSiteCode()), "匿名站点解析不能使用站点编码");
        }
    }

    private LambdaQueryWrapper<CmsContentPublishEntity> effectivePublishWrapper(String tenantId,
                                                                               Long siteId,
                                                                               Long categoryId,
                                                                               String recommendationType,
                                                                               LocalDateTime now) {
        return new LambdaQueryWrapper<CmsContentPublishEntity>()
                .eq(CmsContentPublishEntity::getTenantId, tenantId)
                .eq(CmsContentPublishEntity::getSiteId, siteId)
                .eq(categoryId != null, CmsContentPublishEntity::getCategoryId, categoryId)
                .in(CmsContentPublishEntity::getPublishStatus, CmsPublishStatus.PUBLISHED.name(), CmsPublishStatus.SCHEDULED.name())
                .eq(StringUtils.hasText(recommendationType), CmsContentPublishEntity::getRecommendationType, recommendationType)
                .and(w -> w.isNull(CmsContentPublishEntity::getScheduledPublishTime)
                        .or()
                        .le(CmsContentPublishEntity::getScheduledPublishTime, now))
                .and(w -> w.isNull(CmsContentPublishEntity::getOfflineTime)
                        .or()
                        .gt(CmsContentPublishEntity::getOfflineTime, now))
                .orderByDesc(CmsContentPublishEntity::getTop)
                .orderByAsc(CmsContentPublishEntity::getSort)
                .orderByDesc(CmsContentPublishEntity::getPublishTime);
    }

    private boolean publicContent(CmsContentEntity content, String tenantId, LocalDateTime now) {
        return tenantId.equals(content.getTenantId())
                && CmsContentStatus.PUBLISHED.name().equals(content.getStatus())
                && CmsSupport.isEffective(content.getPublishTime(), content.getOfflineTime(), now);
    }

    private boolean publicCategory(SiteCategoryVO category) {
        if (category.getChildren() != null) {
            category.setChildren(category.getChildren().stream()
                    .filter(this::publicCategory)
                    .collect(Collectors.toCollection(ArrayList::new)));
        }
        return true;
    }

    private List<SiteCategoryVO> buildCategoryTree(List<SiteCategoryVO> items) {
        Map<Long, SiteCategoryVO> map = new HashMap<>();
        List<SiteCategoryVO> roots = new ArrayList<>();
        items.forEach(item -> map.put(item.getId(), item));
        for (SiteCategoryVO item : items) {
            Long parentId = item.getParentId() == null ? CmsSupport.ROOT_PARENT_ID : item.getParentId();
            if (parentId == CmsSupport.ROOT_PARENT_ID || !map.containsKey(parentId)) {
                roots.add(item);
            } else {
                map.get(parentId).getChildren().add(item);
            }
        }
        Comparator<SiteCategoryVO> comparator = Comparator.comparing(vo -> vo.getSort() == null ? 0 : vo.getSort());
        roots.sort(comparator);
        map.values().forEach(item -> item.getChildren().sort(comparator));
        return roots;
    }

    private SiteResolveQuery toResolveQuery(SiteCategoryQuery query) {
        SiteResolveQuery resolve = new SiteResolveQuery();
        if (query != null) {
            resolve.setSiteCode(query.getSiteCode());
            resolve.setDomain(query.getDomain());
        }
        return resolve;
    }

    private SiteResolveQuery toResolveQuery(SiteNavigationQuery query) {
        SiteResolveQuery resolve = new SiteResolveQuery();
        if (query != null) {
            resolve.setSiteCode(query.getSiteCode());
            resolve.setDomain(query.getDomain());
        }
        return resolve;
    }

    private SiteResolveQuery toResolveQuery(SiteBannerQuery query) {
        SiteResolveQuery resolve = new SiteResolveQuery();
        if (query != null) {
            resolve.setSiteCode(query.getSiteCode());
            resolve.setDomain(query.getDomain());
        }
        return resolve;
    }

    private SiteResolveQuery toResolveQuery(SiteAdvertisementQuery query) {
        SiteResolveQuery resolve = new SiteResolveQuery();
        if (query != null) {
            resolve.setSiteCode(query.getSiteCode());
            resolve.setDomain(query.getDomain());
        }
        return resolve;
    }

    private SiteResolveQuery toResolveQuery(SiteContentPageQuery query) {
        SiteResolveQuery resolve = new SiteResolveQuery();
        resolve.setSiteCode(query.getSiteCode());
        resolve.setDomain(query.getDomain());
        return resolve;
    }

    private SiteResolveQuery toResolveQuery(SiteContentDetailQuery query) {
        SiteResolveQuery resolve = new SiteResolveQuery();
        resolve.setSiteCode(query.getSiteCode());
        resolve.setDomain(query.getDomain());
        return resolve;
    }

    private SiteResolveVO toResolveVO(CmsSiteEntity site) {
        SiteResolveVO vo = new SiteResolveVO();
        vo.setSiteId(site.getId());
        vo.setSiteCode(site.getSiteCode());
        vo.setSiteName(site.getSiteName());
        vo.setStatus(site.getStatus());
        vo.setSeoTitle(site.getSeoTitle());
        vo.setSeoKeywords(site.getSeoKeywords());
        vo.setSeoDescription(site.getSeoDescription());
        vo.setFooterCopyright(site.getFooterCopyright());
        vo.setIcpRecord(site.getIcpRecord());
        vo.setContactInfo(site.getContactInfo());
        return vo;
    }

    private SiteVO toPublicSiteVO(CmsSiteEntity site) {
        SiteVO vo = new SiteVO();
        vo.setId(site.getId());
        vo.setSiteName(site.getSiteName());
        vo.setSiteCode(site.getSiteCode());
        vo.setLogoFileId(site.getLogoFileId());
        vo.setLogoUrl(publicFileUrl(site, site.getLogoFileId()));
        vo.setDescription(site.getDescription());
        vo.setDomain(site.getDomain());
        vo.setDefaultLanguage(site.getDefaultLanguage());
        vo.setSeoTitle(site.getSeoTitle());
        vo.setSeoKeywords(site.getSeoKeywords());
        vo.setSeoDescription(site.getSeoDescription());
        vo.setFooterCopyright(site.getFooterCopyright());
        vo.setIcpRecord(site.getIcpRecord());
        vo.setContactInfo(site.getContactInfo());
        return vo;
    }

    private SiteCategoryVO toPublicSiteCategoryVO(CmsSiteCategoryEntity entity) {
        SiteCategoryVO vo = new SiteCategoryVO();
        vo.setId(entity.getId());
        vo.setParentId(entity.getParentId());
        vo.setCategoryName(entity.getCategoryName());
        vo.setCategoryCode(entity.getCategoryCode());
        vo.setCategoryType(entity.getCategoryType());
        vo.setAccessPath(entity.getAccessPath());
        vo.setExternalUrl(entity.getExternalUrl());
        vo.setSort(entity.getSort());
        vo.setSeoTitle(entity.getSeoTitle());
        vo.setSeoKeywords(entity.getSeoKeywords());
        vo.setSeoDescription(entity.getSeoDescription());
        return vo;
    }

    private SiteNavigationVO toPublicNavigationVO(CmsNavigationEntity entity) {
        SiteNavigationVO vo = new SiteNavigationVO();
        vo.setId(entity.getId());
        vo.setNavType(entity.getNavType());
        vo.setNavName(entity.getNavName());
        vo.setJumpType(entity.getJumpType());
        vo.setCategoryId(entity.getCategoryId());
        vo.setContentId(entity.getContentId());
        vo.setExternalUrl(entity.getExternalUrl());
        vo.setOpenTarget(entity.getOpenTarget());
        vo.setSort(entity.getSort());
        return vo;
    }

    private SiteBannerVO toPublicBannerVO(CmsBannerEntity entity) {
        SiteBannerVO vo = new SiteBannerVO();
        vo.setId(entity.getId());
        vo.setPosition(entity.getPosition());
        vo.setTitle(entity.getTitle());
        vo.setSubtitle(entity.getSubtitle());
        vo.setMediaType(entity.getMediaType());
        vo.setMediaFileId(entity.getMediaFileId());
        vo.setJumpUrl(entity.getJumpUrl());
        vo.setSort(entity.getSort());
        return vo;
    }

    private SiteBannerVO toPublicBannerVO(AdDeliveryWithPosition item) {
        CmsAdDeliveryEntity delivery = item.delivery();
        CmsAdvertisementEntity position = item.position();
        SiteBannerVO vo = new SiteBannerVO();
        vo.setId(delivery.getId());
        vo.setPosition(position.getPosition());
        vo.setTitle(StringUtils.hasText(delivery.getTitle()) ? delivery.getTitle() : delivery.getDeliveryName());
        vo.setSubtitle(delivery.getTextContent());
        vo.setMediaType(CmsAdvertisementType.VIDEO.name().equals(delivery.getMaterialType())
                ? CmsBannerMediaType.VIDEO.name()
                : CmsBannerMediaType.IMAGE.name());
        vo.setMediaFileId(CmsAdvertisementType.VIDEO.name().equals(delivery.getMaterialType())
                ? delivery.getVideoFileId()
                : firstImageFileId(delivery));
        vo.setJumpUrl(delivery.getJumpUrl());
        vo.setSort(delivery.getSort());
        return vo;
    }

    private SiteAdvertisementVO toPublicAdvertisementVO(CmsAdvertisementEntity entity) {
        SiteAdvertisementVO vo = new SiteAdvertisementVO();
        vo.setId(entity.getId());
        vo.setAdCode(entity.getAdCode());
        vo.setAdName(entity.getAdName());
        vo.setPosition(entity.getPosition());
        vo.setAdType(entity.getAdType());
        vo.setMaterialFileId(entity.getMaterialFileId());
        vo.setJumpUrl(entity.getJumpUrl());
        vo.setSort(entity.getSort());
        return vo;
    }

    private SiteAdvertisementVO toPublicAdvertisementVO(CmsSiteEntity site, AdDeliveryWithPosition item) {
        CmsAdDeliveryEntity delivery = item.delivery();
        CmsAdvertisementEntity position = item.position();
        SiteAdvertisementVO vo = new SiteAdvertisementVO();
        vo.setId(delivery.getId());
        vo.setAdCode(position.getAdCode());
        vo.setAdName(position.getAdName());
        vo.setPosition(position.getPosition());
        vo.setPositionType(position.getPositionType());
        vo.setAdType(delivery.getMaterialType());
        vo.setMaterialType(delivery.getMaterialType());
        vo.setMaterialFileId(firstMaterialFileId(delivery));
        vo.setTitle(delivery.getTitle());
        vo.setTextContent(delivery.getTextContent());
        vo.setRichContent(delivery.getRichContent());
        vo.setHtmlContent(delivery.getHtmlContent());
        vo.setImageFileId(delivery.getImageFileId());
        vo.setImageFileIds(delivery.getImageFileIds());
        vo.setImageUrl(publicFileUrl(site, firstImageFileId(delivery)));
        vo.setImageUrls(publicFileUrls(site, delivery.getImageFileIds()));
        vo.setVideoFileId(delivery.getVideoFileId());
        vo.setCoverFileId(delivery.getCoverFileId());
        vo.setVideoUrl(publicFileUrl(site, delivery.getVideoFileId()));
        vo.setCoverUrl(publicFileUrl(site, delivery.getCoverFileId()));
        vo.setJumpUrl(delivery.getJumpUrl());
        vo.setOpenTarget(delivery.getOpenTarget());
        vo.setSort(delivery.getSort());
        return vo;
    }

    private String firstMaterialFileId(CmsAdDeliveryEntity delivery) {
        if (CmsAdvertisementType.VIDEO.name().equals(delivery.getMaterialType())) {
            return delivery.getVideoFileId();
        }
        return firstImageFileId(delivery);
    }

    private String firstImageFileId(CmsAdDeliveryEntity delivery) {
        if (StringUtils.hasText(delivery.getImageFileId())) {
            return delivery.getImageFileId();
        }
        if (!StringUtils.hasText(delivery.getImageFileIds())) {
            return null;
        }
        return delivery.getImageFileIds().split(",")[0].trim();
    }

    private String publicFileUrls(CmsSiteEntity site, String fileIds) {
        if (!StringUtils.hasText(fileIds)) {
            return null;
        }
        return java.util.Arrays.stream(fileIds.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(fileId -> publicFileUrl(site, fileId))
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(","));
    }

    private String publicFileUrl(CmsSiteEntity site, String fileId) {
        if (site == null || !StringUtils.hasText(site.getDomain()) || !StringUtils.hasText(fileId)) {
            return null;
        }
        try {
            Long id = Long.valueOf(fileId.trim());
            return "/api/cms-api/files/public-preview?id=" + id + "&domain=" + encode(site.getDomain());
        } catch (RuntimeException ex) {
            LOGGER.log(Level.WARNING, "CMS public file preview resolve failed, fileId=" + fileId, ex);
            return null;
        }
    }

    private boolean isPublicSiteFile(CmsSiteEntity site, String fileId) {
        if (!StringUtils.hasText(fileId)) {
            return false;
        }
        if (fileId.equals(String.valueOf(site.getLogoFileId()))) {
            return true;
        }
        if (isPublicContentFile(site, fileId)) {
            return true;
        }
        return isPublicAdFile(site, fileId);
    }

    private boolean isPublicContentFile(CmsSiteEntity site, String fileId) {
        LocalDateTime now = LocalDateTime.now();
        return contentMapper.selectList(new LambdaQueryWrapper<CmsContentEntity>()
                        .eq(CmsContentEntity::getTenantId, site.getTenantId())
                        .eq(CmsContentEntity::getStatus, CmsContentStatus.PUBLISHED.name())
                        .and(wrapper -> wrapper.eq(CmsContentEntity::getCoverFileId, fileId)
                                .or()
                                .eq(CmsContentEntity::getAttachmentFileId, fileId)
                                .or()
                                .eq(CmsContentEntity::getVideoFileId, fileId)
                                .or()
                                .like(CmsContentEntity::getBody, "mango-file:" + fileId)))
                .stream()
                .filter(item -> CmsSupport.isEffective(item.getPublishTime(), item.getOfflineTime(), now))
                .anyMatch(item -> publishMapper.selectCount(effectivePublishWrapper(site.getTenantId(), site.getId(), null, null, now)
                        .eq(CmsContentPublishEntity::getContentId, item.getId())) > 0);
    }

    private boolean isPublicAdFile(CmsSiteEntity site, String fileId) {
        LocalDateTime now = LocalDateTime.now();
        List<AdDeliveryWithPosition> deliveries = listEffectiveDeliveries(site, null, null, now);
        boolean matchedDelivery = deliveries.stream()
                .map(AdDeliveryWithPosition::delivery)
                .anyMatch(delivery -> adDeliveryReferencesFile(delivery, fileId));
        if (matchedDelivery) {
            return true;
        }
        return advertisementMapper.selectList(new LambdaQueryWrapper<CmsAdvertisementEntity>()
                        .eq(CmsAdvertisementEntity::getTenantId, site.getTenantId())
                        .eq(CmsAdvertisementEntity::getSiteId, site.getId())
                        .eq(CmsAdvertisementEntity::getStatus, CmsSupport.ENABLED))
                .stream()
                .filter(item -> CmsSupport.isEffective(item.getStartTime(), item.getEndTime(), now))
                .anyMatch(item -> fileId.equals(String.valueOf(item.getMaterialFileId())));
    }

    private boolean adDeliveryReferencesFile(CmsAdDeliveryEntity delivery, String fileId) {
        return fileId.equals(String.valueOf(delivery.getImageFileId()))
                || fileId.equals(String.valueOf(delivery.getVideoFileId()))
                || fileId.equals(String.valueOf(delivery.getCoverFileId()))
                || java.util.Arrays.stream(String.valueOf(delivery.getImageFileIds()).split(","))
                .map(String::trim)
                .anyMatch(fileId::equals);
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(String.valueOf(value), java.nio.charset.StandardCharsets.UTF_8);
    }

    private <T> T withTenantContext(String tenantId, java.util.function.Supplier<T> supplier) {
        MangoContextSnapshot previous = MangoContextHolder.get();
        if (StringUtils.hasText(tenantId) && !tenantId.equals(previous.tenantId())) {
            MangoContextHolder.set(previous.withTenantId(tenantId));
        }
        try {
            return supplier.get();
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    private SiteContentVO toPublicContentVO(CmsSiteEntity site, CmsContentEntity entity, CmsContentPublishEntity publish) {
        SiteContentVO vo = new SiteContentVO();
        vo.setId(entity.getId());
        vo.setTitle(entity.getTitle());
        vo.setSubtitle(entity.getSubtitle());
        vo.setSummary(entity.getSummary());
        vo.setContentType(entity.getContentType());
        vo.setCoverFileId(entity.getCoverFileId());
        vo.setCoverUrl(publicFileUrl(site, entity.getCoverFileId()));
        vo.setBody(entity.getBody());
        vo.setExternalUrl(entity.getExternalUrl());
        vo.setAttachmentFileId(entity.getAttachmentFileId());
        vo.setAttachmentUrl(publicFileUrl(site, entity.getAttachmentFileId()));
        vo.setVideoFileId(entity.getVideoFileId());
        vo.setVideoUrl(publicFileUrl(site, entity.getVideoFileId()));
        vo.setSource(entity.getSource());
        vo.setAuthor(entity.getAuthor());
        if (publish != null) {
            vo.setCategoryId(publish.getCategoryId());
            CmsSiteCategoryEntity category = siteCategoryMapper.selectById(publish.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getCategoryName());
            }
        }
        vo.setSeoTitle(entity.getSeoTitle());
        vo.setSeoKeywords(entity.getSeoKeywords());
        vo.setSeoDescription(entity.getSeoDescription());
        vo.setPublishTime(entity.getPublishTime());
        return vo;
    }

    private CmsContentPublishEntity findEffectivePublish(CmsSiteEntity site, Long contentId, Long categoryId, LocalDateTime now) {
        return publishMapper.selectList(effectivePublishWrapper(site.getTenantId(), site.getId(), categoryId, null, now)
                        .eq(CmsContentPublishEntity::getContentId, contentId)
                        .last("limit 1"))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private record AdDeliveryWithPosition(CmsAdDeliveryEntity delivery, CmsAdvertisementEntity position) {
    }
}
