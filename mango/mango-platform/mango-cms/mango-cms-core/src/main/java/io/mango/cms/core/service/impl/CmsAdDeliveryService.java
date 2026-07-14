package io.mango.cms.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.cms.api.command.SaveCmsAdDeliveryCommand;
import io.mango.cms.api.command.UpdateCmsStatusCommand;
import io.mango.cms.api.enums.CmsAdvertisementType;
import io.mango.cms.api.enums.CmsCode;
import io.mango.cms.api.enums.CmsOpenTarget;
import io.mango.cms.api.enums.CmsStatus;
import io.mango.cms.api.query.CmsAdDeliveryPageQuery;
import io.mango.cms.api.vo.CmsAdDeliveryVO;
import io.mango.cms.core.entity.CmsAdDeliveryEntity;
import io.mango.cms.core.entity.CmsAdvertisementEntity;
import io.mango.cms.core.entity.CmsSiteEntity;
import io.mango.cms.core.mapper.CmsAdDeliveryMapper;
import io.mango.cms.core.mapper.CmsAdvertisementMapper;
import io.mango.cms.core.mapper.CmsSiteMapper;
import io.mango.cms.core.service.ICmsAdDeliveryService;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.file.api.FileApi;
import io.mango.file.api.enums.FileRecordStatus;
import io.mango.file.api.vo.FileRecordVO;
import io.mango.infra.persistence.api.scope.DataScopeApplier;
import io.mango.infra.persistence.api.scope.DataScopeMapping;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** CMS AdDelivery aggregate service. */
@SuppressWarnings("PMD.ServiceOrDaoClassShouldEndWithImplRule")
@Service
@RequiredArgsConstructor
public class CmsAdDeliveryService implements ICmsAdDeliveryService {

    private static final String FILE_ID_SEPARATOR = ",";
    private static final String LIST_RESOURCE_SUFFIX = ":list";
    private static final DataScopeMapping SITE_SCOPE = cmsScope("cms_site");
    private static final DataScopeMapping ADVERTISEMENT_SCOPE = cmsScope("cms_advertisement");
    private static final DataScopeMapping AD_DELIVERY_SCOPE = cmsScope("cms_ad_delivery");
    private final CmsSiteMapper siteMapper;
    private final CmsAdvertisementMapper advertisementMapper;
    private final CmsAdDeliveryMapper adDeliveryMapper;
    private final ObjectProvider<DataScopeApplier> dataScopeApplierProvider;
    private final ObjectProvider<FileApi> fileApiProvider;

    @Override
    public PageResult<CmsAdDeliveryVO> pageAdDeliveries(CmsAdDeliveryPageQuery query) {
        CmsAdDeliveryPageQuery resolved = CmsSupport.defaultIfNull(query, new CmsAdDeliveryPageQuery());
        IPage<CmsAdDeliveryEntity> page = adDeliveryMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()),
                adDeliveryWrapper(resolved));
        return PageResult.of(page.getRecords().stream().map(this::toAdDeliveryVO).toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public CmsAdDeliveryVO detailAdDelivery(Long id) {
        return toAdDeliveryVO(requireAdDelivery(id));
    }

    @Override
    public Long createAdDelivery(SaveCmsAdDeliveryCommand command) {
        Require.notNull(command, CmsCode.CMS_BUSINESS_ERROR, "广告投放保存命令不能为空");
        CmsAdDeliveryEntity entity = new CmsAdDeliveryEntity();
        applyAdDelivery(entity, command);
        entity.setTenantId(CmsSupport.currentTenantId());
        adDeliveryMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public Boolean updateAdDelivery(SaveCmsAdDeliveryCommand command) {
        Require.notNull(command, CmsCode.CMS_BUSINESS_ERROR, "广告投放保存命令不能为空");
        CmsAdDeliveryEntity entity = requireAdDelivery(command.getId());
        applyAdDelivery(entity, command);
        return adDeliveryMapper.updateById(entity) > 0;
    }

    @Override
    public Boolean updateAdDeliveryStatus(UpdateCmsStatusCommand command) {
        Require.notNull(command, CmsCode.CMS_BUSINESS_ERROR, "广告投放状态更新命令不能为空");
        CmsAdDeliveryEntity entity = requireAdDelivery(command.getId());
        entity.setStatus(CmsSupport.enumName(CmsStatus.class, command.getStatus(), "广告投放状态非法"));
        return adDeliveryMapper.updateById(entity) > 0;
    }

    @Override
    public Boolean deleteAdDelivery(Long id) {
        Require.notNull(id, CmsCode.CMS_BUSINESS_ERROR, "广告投放 ID 不能为空");
        return adDeliveryMapper.deleteById(requireAdDelivery(id).getId()) > 0;
    }

    private void applyAdDelivery(CmsAdDeliveryEntity entity, SaveCmsAdDeliveryCommand command) {
        Require.notNull(command, CmsCode.CMS_BUSINESS_ERROR, "广告投放保存命令不能为空");
        requireSite(command.getSiteId());
        CmsAdvertisementEntity ad = requireAdvertisement(command.getAdId());
        Require.isTrue(command.getSiteId().equals(ad.getSiteId()), CmsCode.CMS_BUSINESS_ERROR, "广告投放站点与广告位不一致");
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
        if (CmsAdvertisementType.VIDEO.name().equals(materialType)) {
            entity.setVideoFileId(validateVideoFile(command.getVideoFileId(), "广告视频文件"));
            entity.setCoverFileId(validateImageFile(command.getCoverFileId(), "广告视频封面"));
        } else {
            entity.setVideoFileId(null);
            entity.setCoverFileId(null);
        }
        entity.setJumpUrl(normalizePublicUrl(command.getJumpUrl(), "广告跳转地址非法"));
        entity.setOpenTarget(CmsSupport.enumNameOrDefault(
                CmsOpenTarget.class, command.getOpenTarget(), CmsOpenTarget.SELF.name(), "打开方式非法"));
        entity.setStartTime(command.getStartTime());
        entity.setEndTime(command.getEndTime());
        if (entity.getStartTime() != null && entity.getEndTime() != null) {
            Require.isTrue(!entity.getEndTime().isBefore(entity.getStartTime()), CmsCode.CMS_BUSINESS_ERROR, "结束时间不能早于开始时间");
        }
        entity.setSort(CmsSupport.defaultSort(command.getSort()));
        entity.setStatus(CmsSupport.defaultStatus(command.getStatus()));
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

    private CmsAdDeliveryEntity requireAdDelivery(Long id) {
        Require.notNull(id, CmsCode.CMS_BUSINESS_ERROR, "广告投放 ID 不能为空");
        CmsAdDeliveryEntity entity = adDeliveryMapper.selectOne(scopedById(id, "cms:ad-delivery:list", AD_DELIVERY_SCOPE));
        Require.notNull(entity, CmsCode.CMS_BUSINESS_ERROR, "广告投放不存在");
        return entity;
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
        for (String item : value.split(FILE_ID_SEPARATOR)) {
            String fileId = CmsSupport.trimRequired(item, "广告图片组文件不能为空");
            normalized.add(validateImageFile(fileId, "广告图片组文件"));
        }
        return String.join(FILE_ID_SEPARATOR, normalized);
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
        Require.isTrue(safe && !lower.startsWith("//"), CmsCode.CMS_BUSINESS_ERROR, message);
        return url;
    }

    private CmsSiteEntity requireSite(Long id) {
        Require.notNull(id, CmsCode.CMS_BUSINESS_ERROR, "站点 ID 不能为空");
        CmsSiteEntity entity = siteMapper.selectOne(scopedById(id, "cms:site:list", SITE_SCOPE));
        Require.notNull(entity, CmsCode.CMS_BUSINESS_ERROR, "站点不存在");
        return entity;
    }

    private CmsAdvertisementEntity requireAdvertisement(Long id) {
        Require.notNull(id, CmsCode.CMS_BUSINESS_ERROR, "广告 ID 不能为空");
        CmsAdvertisementEntity entity = advertisementMapper.selectOne(scopedById(id, "cms:advertisement:list", ADVERTISEMENT_SCOPE));
        Require.notNull(entity, CmsCode.CMS_BUSINESS_ERROR, "广告不存在");
        return entity;
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

    private String validateImageFile(String value, String fieldName) {
        return validateFile(value, fieldName, "image/");
    }

    private String validateVideoFile(String value, String fieldName) {
        return validateFile(value, fieldName, "video/");
    }

    private String validateFile(String value, String fieldName, String contentTypePrefix) {
        String normalized = CmsSupport.trimToNull(value);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        Long fileId = parseFileId(normalized, fieldName);
        FileApi fileApi = fileApiProvider.getIfAvailable();
        Require.notNull(fileApi, CmsCode.CMS_BUSINESS_ERROR, fieldName + "能力不可用");
        FileRecordVO file = CmsFileResponse.requireRecord(fileApi.get(fileId), fieldName);
        Require.isTrue(FileRecordStatus.COMPLETED.value() == CmsSupport.defaultIfNull(file.getStatus(), -1),
                CmsCode.CMS_BUSINESS_ERROR, fieldName + "未上传完成");
        Require.isTrue(file.getArchived() == null || file.getArchived() == 0, CmsCode.CMS_BUSINESS_ERROR, fieldName + "已归档");
        if (StringUtils.hasText(contentTypePrefix)) {
            String contentType = CmsSupport.trimToNull(file.getContentType());
            Require.isTrue(contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith(contentTypePrefix), CmsCode.CMS_BUSINESS_ERROR, fieldName + "类型不匹配");
        }
        return normalized;
    }

    private Long parseFileId(String value, String fieldName) {
        String raw = CmsSupport.removePrefix(value, "mango-file:");
        Require.isTrue(raw.matches("\\d+"), CmsCode.CMS_BUSINESS_ERROR, fieldName + "格式非法");
        return Long.valueOf(raw);
    }

    private static DataScopeMapping cmsScope(String tableName) {
        return DataScopeMapping.builder()
                .tableName(tableName)
                .selfField("created_by")
                .orgField("org_id")
                .tenantField("tenant_id")
                .build();
    }
}
