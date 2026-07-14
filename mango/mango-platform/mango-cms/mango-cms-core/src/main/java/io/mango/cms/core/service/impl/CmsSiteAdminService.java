package io.mango.cms.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.cms.api.command.SaveCmsSiteCommand;
import io.mango.cms.api.command.SaveCmsSiteSettingCommand;
import io.mango.cms.api.command.UpdateCmsStatusCommand;
import io.mango.cms.api.enums.CmsCode;
import io.mango.cms.api.enums.CmsStatus;
import io.mango.cms.api.query.CmsSitePageQuery;
import io.mango.cms.api.vo.CmsSiteVO;
import io.mango.cms.core.entity.CmsAdDeliveryEntity;
import io.mango.cms.core.entity.CmsAdvertisementEntity;
import io.mango.cms.core.entity.CmsBannerEntity;
import io.mango.cms.core.entity.CmsContentPublishEntity;
import io.mango.cms.core.entity.CmsNavigationEntity;
import io.mango.cms.core.entity.CmsSiteCategoryEntity;
import io.mango.cms.core.entity.CmsSiteEntity;
import io.mango.cms.core.mapper.CmsAdDeliveryMapper;
import io.mango.cms.core.mapper.CmsAdvertisementMapper;
import io.mango.cms.core.mapper.CmsBannerMapper;
import io.mango.cms.core.mapper.CmsContentPublishMapper;
import io.mango.cms.core.mapper.CmsNavigationMapper;
import io.mango.cms.core.mapper.CmsSiteCategoryMapper;
import io.mango.cms.core.mapper.CmsSiteMapper;
import io.mango.cms.core.service.ICmsSiteAdminService;
import io.mango.cms.core.service.ICmsSiteSettingService;
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

/** CMS SiteAdmin aggregate service. */
@SuppressWarnings("PMD.ServiceOrDaoClassShouldEndWithImplRule")
@Service
@RequiredArgsConstructor
public class CmsSiteAdminService implements ICmsSiteAdminService {

    private static final String LIST_RESOURCE_SUFFIX = ":list";
    private static final DataScopeMapping SITE_SCOPE = cmsScope("cms_site");
    private final CmsSiteMapper siteMapper;
    private final CmsSiteCategoryMapper siteCategoryMapper;
    private final CmsContentPublishMapper publishMapper;
    private final CmsNavigationMapper navigationMapper;
    private final CmsBannerMapper bannerMapper;
    private final CmsAdvertisementMapper advertisementMapper;
    private final CmsAdDeliveryMapper adDeliveryMapper;
    private final ICmsSiteSettingService siteSettingService;
    private final ObjectProvider<DataScopeApplier> dataScopeApplierProvider;
    private final ObjectProvider<FileApi> fileApiProvider;

    @Override
    public PageResult<CmsSiteVO> pageSites(CmsSitePageQuery query) {
        CmsSitePageQuery resolved = query == null ? new CmsSitePageQuery() : query;
        IPage<CmsSiteEntity> page = siteMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()), siteWrapper(resolved));
        return PageResult.of(page.getRecords().stream().map(this::toSiteVO).toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public CmsSiteVO detailSite(Long id) {
        return toSiteVO(requireSite(id));
    }

    @Override
    public Long createSite(SaveCmsSiteCommand command) {
        Require.notNull(command, CmsCode.CMS_BUSINESS_ERROR, "站点保存命令不能为空");
        CmsSiteEntity entity = new CmsSiteEntity();
        applySite(entity, command, false);
        entity.setTenantId(CmsSupport.currentTenantId());
        siteMapper.insert(entity);
        saveSiteSettingFromSite(entity);
        return entity.getId();
    }

    @Override
    public Boolean updateSite(SaveCmsSiteCommand command) {
        Require.notNull(command, CmsCode.CMS_BUSINESS_ERROR, "站点保存命令不能为空");
        CmsSiteEntity entity = requireSite(command.getId());
        applySite(entity, command, true);
        boolean updated = siteMapper.updateById(entity) > 0;
        saveSiteSettingFromSite(entity);
        return updated;
    }

    @Override
    public Boolean updateSiteStatus(UpdateCmsStatusCommand command) {
        Require.notNull(command, CmsCode.CMS_BUSINESS_ERROR, "站点状态更新命令不能为空");
        CmsSiteEntity entity = requireSite(command.getId());
        entity.setStatus(CmsSupport.enumName(CmsStatus.class, command.getStatus(), "站点状态非法"));
        return siteMapper.updateById(entity) > 0;
    }

    @Override
    public Boolean deleteSite(Long id) {
        CmsSiteEntity entity = requireSite(id);
        Require.isTrue(countBySite(id) == 0, CmsCode.CMS_BUSINESS_ERROR, "站点已被栏目、导航、Banner、广告或发布关系引用，不能删除");
        return siteMapper.deleteById(entity.getId()) > 0;
    }

    private void applySite(CmsSiteEntity entity, SaveCmsSiteCommand command, boolean update) {
        Require.notNull(command, CmsCode.CMS_BUSINESS_ERROR, "站点保存命令不能为空");
        if (update) {
            Require.notNull(command.getId(), CmsCode.CMS_BUSINESS_ERROR, "站点 ID 不能为空");
        }
        String code = CmsSupport.trimRequired(command.getSiteCode(), "站点编码不能为空");
        CmsSiteEntity exists = siteMapper.selectOne(new LambdaQueryWrapper<CmsSiteEntity>()
                .eq(CmsSiteEntity::getTenantId, CmsSupport.currentTenantId())
                .eq(CmsSiteEntity::getSiteCode, code)
                .last("LIMIT 1"));
        Require.isTrue(exists == null || exists.getId().equals(entity.getId()), CmsCode.CMS_BUSINESS_ERROR, "站点编码已存在");
        String domain = CmsSupport.trimToNull(command.getDomain());
        if (StringUtils.hasText(domain)) {
            CmsSiteEntity domainExists = siteMapper.selectOne(new LambdaQueryWrapper<CmsSiteEntity>()
                    .eq(CmsSiteEntity::getTenantId, CmsSupport.currentTenantId())
                    .eq(CmsSiteEntity::getDomain, domain)
                    .last("LIMIT 1"));
            Require.isTrue(domainExists == null || domainExists.getId().equals(entity.getId()), CmsCode.CMS_BUSINESS_ERROR, "站点域名已存在");
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

    private void saveSiteSettingFromSite(CmsSiteEntity site) {
        SaveCmsSiteSettingCommand command = new SaveCmsSiteSettingCommand();
        command.setSiteId(site.getId());
        command.setSeoTitle(site.getSeoTitle());
        command.setSeoKeywords(site.getSeoKeywords());
        command.setSeoDescription(site.getSeoDescription());
        command.setFooterCopyright(site.getFooterCopyright());
        command.setIcpRecord(site.getIcpRecord());
        command.setContactInfo(site.getContactInfo());
        siteSettingService.saveSiteSetting(command);
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

    private QueryWrapper<CmsSiteEntity> siteWrapper(CmsSitePageQuery query) {
        String keyword = CmsSupport.trimToNull(query.getKeyword());
        QueryWrapper<CmsSiteEntity> wrapper = new QueryWrapper<CmsSiteEntity>()
                .eq("tenant_id", CmsSupport.currentTenantId())
                .eq(StringUtils.hasText(query.getStatus()), "status", query.getStatus())
                .and(StringUtils.hasText(keyword), w -> w.like("site_code", keyword).or().like("site_name", keyword))
                .orderByDesc("updated_at");
        return applyDataScope(wrapper, "cms:site:list", SITE_SCOPE);
    }

    private CmsSiteEntity requireSite(Long id) {
        Require.notNull(id, CmsCode.CMS_BUSINESS_ERROR, "站点 ID 不能为空");
        CmsSiteEntity entity = siteMapper.selectOne(scopedById(id, "cms:site:list", SITE_SCOPE));
        Require.notNull(entity, CmsCode.CMS_BUSINESS_ERROR, "站点不存在");
        return entity;
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

    private String validateFile(String value, String fieldName, String contentTypePrefix) {
        String normalized = CmsSupport.trimToNull(value);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        Long fileId = parseFileId(normalized, fieldName);
        FileApi fileApi = fileApiProvider.getIfAvailable();
        Require.notNull(fileApi, CmsCode.CMS_BUSINESS_ERROR, fieldName + "能力不可用");
        FileRecordVO file = CmsFileResponse.requireRecord(fileApi.get(fileId), fieldName);
        Require.isTrue(FileRecordStatus.COMPLETED.value() == (file.getStatus() == null ? -1 : file.getStatus()), CmsCode.CMS_BUSINESS_ERROR, fieldName + "未上传完成");
        Require.isTrue(file.getArchived() == null || file.getArchived() == 0, CmsCode.CMS_BUSINESS_ERROR, fieldName + "已归档");
        if (StringUtils.hasText(contentTypePrefix)) {
            String contentType = CmsSupport.trimToNull(file.getContentType());
            Require.isTrue(contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith(contentTypePrefix), CmsCode.CMS_BUSINESS_ERROR, fieldName + "类型不匹配");
        }
        return normalized;
    }

    private Long parseFileId(String value, String fieldName) {
        String raw = value.startsWith("mango-file:") ? value.substring("mango-file:".length()) : value;
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
