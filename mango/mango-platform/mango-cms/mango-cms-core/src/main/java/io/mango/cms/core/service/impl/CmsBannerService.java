package io.mango.cms.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.cms.api.command.SaveCmsBannerCommand;
import io.mango.cms.api.command.UpdateCmsStatusCommand;
import io.mango.cms.api.enums.CmsBannerMediaType;
import io.mango.cms.api.enums.CmsCode;
import io.mango.cms.api.enums.CmsStatus;
import io.mango.cms.api.query.CmsBannerPageQuery;
import io.mango.cms.api.vo.CmsBannerVO;
import io.mango.cms.core.entity.CmsBannerEntity;
import io.mango.cms.core.entity.CmsSiteEntity;
import io.mango.cms.core.mapper.CmsBannerMapper;
import io.mango.cms.core.mapper.CmsSiteMapper;
import io.mango.cms.core.service.ICmsBannerService;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.file.api.FileApi;
import io.mango.file.api.enums.FileRecordStatus;
import io.mango.file.api.vo.FileRecordVO;
import io.mango.infra.persistence.api.scope.DataScopeApplier;
import io.mango.infra.persistence.api.scope.DataScopeMapping;
import java.util.Locale;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** CMS Banner aggregate service. */
@SuppressWarnings("PMD.ServiceOrDaoClassShouldEndWithImplRule")
@Service
@RequiredArgsConstructor
public class CmsBannerService implements ICmsBannerService {

    private static final String LIST_RESOURCE_SUFFIX = ":list";
    private static final DataScopeMapping SITE_SCOPE = cmsScope("cms_site");
    private static final DataScopeMapping BANNER_SCOPE = cmsScope("cms_banner");
    private final CmsSiteMapper siteMapper;
    private final CmsBannerMapper bannerMapper;
    private final ObjectProvider<DataScopeApplier> dataScopeApplierProvider;
    private final ObjectProvider<FileApi> fileApiProvider;

    @Override
    public PageResult<CmsBannerVO> pageBanners(CmsBannerPageQuery query) {
        CmsBannerPageQuery resolved = CmsSupport.defaultIfNull(query, new CmsBannerPageQuery());
        IPage<CmsBannerEntity> page = bannerMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()),
                bannerWrapper(resolved));
        return PageResult.of(page.getRecords().stream().map(this::toBannerVO).toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public CmsBannerVO detailBanner(Long id) {
        return toBannerVO(requireBanner(id));
    }

    @Override
    public Long createBanner(SaveCmsBannerCommand command) {
        Require.notNull(command, CmsCode.CMS_BUSINESS_ERROR, "Banner 保存命令不能为空");
        CmsBannerEntity entity = new CmsBannerEntity();
        applyBanner(entity, command);
        entity.setTenantId(CmsSupport.currentTenantId());
        bannerMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public Boolean updateBanner(SaveCmsBannerCommand command) {
        Require.notNull(command, CmsCode.CMS_BUSINESS_ERROR, "Banner 保存命令不能为空");
        CmsBannerEntity entity = requireBanner(command.getId());
        applyBanner(entity, command);
        return bannerMapper.updateById(entity) > 0;
    }

    @Override
    public Boolean updateBannerStatus(UpdateCmsStatusCommand command) {
        Require.notNull(command, CmsCode.CMS_BUSINESS_ERROR, "Banner 状态更新命令不能为空");
        CmsBannerEntity entity = requireBanner(command.getId());
        entity.setStatus(CmsSupport.enumName(CmsStatus.class, command.getStatus(), "Banner 状态非法"));
        return bannerMapper.updateById(entity) > 0;
    }

    @Override
    public Boolean deleteBanner(Long id) {
        Require.notNull(id, CmsCode.CMS_BUSINESS_ERROR, "Banner ID 不能为空");
        return bannerMapper.deleteById(requireBanner(id).getId()) > 0;
    }

    private void applyBanner(CmsBannerEntity entity, SaveCmsBannerCommand command) {
        Require.notNull(command, CmsCode.CMS_BUSINESS_ERROR, "Banner 保存命令不能为空");
        requireSite(command.getSiteId());
        entity.setSiteId(command.getSiteId());
        entity.setPosition(CmsSupport.trimRequired(command.getPosition(), "展示位置不能为空"));
        entity.setTitle(CmsSupport.trimRequired(command.getTitle(), "标题不能为空"));
        entity.setSubtitle(CmsSupport.trimToNull(command.getSubtitle()));
        String mediaType = CmsSupport.enumName(CmsBannerMediaType.class, command.getMediaType(), "媒体类型非法");
        entity.setMediaType(mediaType);
        if (CmsBannerMediaType.VIDEO.name().equals(mediaType)) {
            entity.setMediaFileId(validateVideoFile(command.getMediaFileId(), "Banner 媒体文件"));
        } else {
            entity.setMediaFileId(validateImageFile(command.getMediaFileId(), "Banner 媒体文件"));
        }
        entity.setJumpUrl(normalizePublicUrl(command.getJumpUrl(), "Banner 跳转地址非法"));
        entity.setStartTime(command.getStartTime());
        entity.setEndTime(command.getEndTime());
        entity.setSort(CmsSupport.defaultSort(command.getSort()));
        entity.setStatus(CmsSupport.defaultStatus(command.getStatus()));
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

    private CmsBannerEntity requireBanner(Long id) {
        Require.notNull(id, CmsCode.CMS_BUSINESS_ERROR, "Banner ID 不能为空");
        CmsBannerEntity entity = bannerMapper.selectOne(scopedById(id, "cms:banner:list", BANNER_SCOPE));
        Require.notNull(entity, CmsCode.CMS_BUSINESS_ERROR, "Banner 不存在");
        return entity;
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
