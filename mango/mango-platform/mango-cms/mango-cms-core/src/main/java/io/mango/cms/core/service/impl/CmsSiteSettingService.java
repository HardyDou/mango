package io.mango.cms.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.mango.cms.api.command.SaveCmsSiteSettingCommand;
import io.mango.cms.api.enums.CmsCode;
import io.mango.cms.api.vo.CmsSiteSettingVO;
import io.mango.cms.core.entity.CmsSiteEntity;
import io.mango.cms.core.entity.CmsSiteSettingEntity;
import io.mango.cms.core.mapper.CmsSiteMapper;
import io.mango.cms.core.mapper.CmsSiteSettingMapper;
import io.mango.cms.core.service.ICmsSiteSettingService;
import io.mango.common.result.Require;
import io.mango.infra.persistence.api.scope.DataScopeApplier;
import io.mango.infra.persistence.api.scope.DataScopeMapping;
import org.springframework.beans.factory.ObjectProvider;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** CMS SiteSetting aggregate service. */
@SuppressWarnings("PMD.ServiceOrDaoClassShouldEndWithImplRule")
@Service
@RequiredArgsConstructor
public class CmsSiteSettingService implements ICmsSiteSettingService {

    private static final String LIST_RESOURCE_SUFFIX = ":list";
    private static final DataScopeMapping SITE_SCOPE = cmsScope("cms_site");
    private final CmsSiteMapper siteMapper;
    private final CmsSiteSettingMapper siteSettingMapper;
    private final ObjectProvider<DataScopeApplier> dataScopeApplierProvider;

    @Override
    public CmsSiteSettingVO detailSiteSetting(Long siteId) {
        requireSite(siteId);
        CmsSiteSettingEntity entity = siteSettingMapper.selectOne(new LambdaQueryWrapper<CmsSiteSettingEntity>()
                .eq(CmsSiteSettingEntity::getTenantId, CmsSupport.currentTenantId())
                .eq(CmsSiteSettingEntity::getSiteId, siteId)
                .last("LIMIT 1"));
        return entity == null ? null : toSiteSettingVO(entity);
    }

    @Override
    public Boolean saveSiteSetting(SaveCmsSiteSettingCommand command) {
        Require.notNull(command, CmsCode.CMS_BUSINESS_ERROR, "站点设置保存命令不能为空");
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
        return create ? siteSettingMapper.insert(entity) > 0 : siteSettingMapper.updateById(entity) > 0;
    }

    private CmsSiteEntity requireSite(Long id) {
        Require.notNull(id, CmsCode.CMS_BUSINESS_ERROR, "站点 ID 不能为空");
        CmsSiteEntity entity = siteMapper.selectOne(scopedById(id, "cms:site:list", SITE_SCOPE));
        Require.notNull(entity, CmsCode.CMS_BUSINESS_ERROR, "站点不存在");
        return entity;
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

    private static DataScopeMapping cmsScope(String tableName) {
        return DataScopeMapping.builder()
                .tableName(tableName)
                .selfField("created_by")
                .orgField("org_id")
                .tenantField("tenant_id")
                .build();
    }
}
