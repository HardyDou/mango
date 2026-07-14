package io.mango.cms.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.cms.api.command.SaveCmsNavigationCommand;
import io.mango.cms.api.command.UpdateCmsStatusCommand;
import io.mango.cms.api.enums.CmsCode;
import io.mango.cms.api.enums.CmsJumpType;
import io.mango.cms.api.enums.CmsNavigationType;
import io.mango.cms.api.enums.CmsOpenTarget;
import io.mango.cms.api.enums.CmsStatus;
import io.mango.cms.api.query.CmsNavigationPageQuery;
import io.mango.cms.api.vo.CmsNavigationVO;
import io.mango.cms.core.entity.CmsContentEntity;
import io.mango.cms.core.entity.CmsNavigationEntity;
import io.mango.cms.core.entity.CmsSiteCategoryEntity;
import io.mango.cms.core.entity.CmsSiteEntity;
import io.mango.cms.core.mapper.CmsContentMapper;
import io.mango.cms.core.mapper.CmsNavigationMapper;
import io.mango.cms.core.mapper.CmsSiteCategoryMapper;
import io.mango.cms.core.mapper.CmsSiteMapper;
import io.mango.cms.core.service.ICmsNavigationService;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.infra.persistence.api.scope.DataScopeApplier;
import io.mango.infra.persistence.api.scope.DataScopeMapping;
import java.util.Locale;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** CMS Navigation aggregate service. */
@SuppressWarnings("PMD.ServiceOrDaoClassShouldEndWithImplRule")
@Service
@RequiredArgsConstructor
public class CmsNavigationService implements ICmsNavigationService {

    private static final String LIST_RESOURCE_SUFFIX = ":list";
    private static final DataScopeMapping SITE_SCOPE = cmsScope("cms_site");
    private static final DataScopeMapping SITE_CATEGORY_SCOPE = cmsScope("cms_site_category");
    private static final DataScopeMapping CONTENT_SCOPE = cmsScope("cms_content");
    private static final DataScopeMapping NAVIGATION_SCOPE = cmsScope("cms_navigation");
    private final CmsSiteMapper siteMapper;
    private final CmsSiteCategoryMapper siteCategoryMapper;
    private final CmsContentMapper contentMapper;
    private final CmsNavigationMapper navigationMapper;
    private final ObjectProvider<DataScopeApplier> dataScopeApplierProvider;

    @Override
    public PageResult<CmsNavigationVO> pageNavigations(CmsNavigationPageQuery query) {
        CmsNavigationPageQuery resolved = query == null ? new CmsNavigationPageQuery() : query;
        IPage<CmsNavigationEntity> page = navigationMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()),
                navigationWrapper(resolved));
        return PageResult.of(page.getRecords().stream().map(this::toNavigationVO).toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public CmsNavigationVO detailNavigation(Long id) {
        return toNavigationVO(requireNavigation(id));
    }

    @Override
    public Long createNavigation(SaveCmsNavigationCommand command) {
        Require.notNull(command, CmsCode.CMS_BUSINESS_ERROR, "导航保存命令不能为空");
        CmsNavigationEntity entity = new CmsNavigationEntity();
        applyNavigation(entity, command);
        entity.setTenantId(CmsSupport.currentTenantId());
        navigationMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public Boolean updateNavigation(SaveCmsNavigationCommand command) {
        Require.notNull(command, CmsCode.CMS_BUSINESS_ERROR, "导航保存命令不能为空");
        CmsNavigationEntity entity = requireNavigation(command.getId());
        applyNavigation(entity, command);
        return navigationMapper.updateById(entity) > 0;
    }

    @Override
    public Boolean updateNavigationStatus(UpdateCmsStatusCommand command) {
        Require.notNull(command, CmsCode.CMS_BUSINESS_ERROR, "导航状态更新命令不能为空");
        CmsNavigationEntity entity = requireNavigation(command.getId());
        entity.setStatus(CmsSupport.enumName(CmsStatus.class, command.getStatus(), "导航状态非法"));
        return navigationMapper.updateById(entity) > 0;
    }

    @Override
    public Boolean deleteNavigation(Long id) {
        Require.notNull(id, CmsCode.CMS_BUSINESS_ERROR, "导航 ID 不能为空");
        return navigationMapper.deleteById(requireNavigation(id).getId()) > 0;
    }

    private void applyNavigation(CmsNavigationEntity entity, SaveCmsNavigationCommand command) {
        Require.notNull(command, CmsCode.CMS_BUSINESS_ERROR, "导航保存命令不能为空");
        requireSite(command.getSiteId());
        entity.setSiteId(command.getSiteId());
        entity.setNavType(CmsSupport.enumName(CmsNavigationType.class, command.getNavType(), "导航类型非法"));
        entity.setNavName(CmsSupport.trimRequired(command.getNavName(), "导航名称不能为空"));
        entity.setJumpType(CmsSupport.enumName(CmsJumpType.class, command.getJumpType(), "跳转类型非法"));
        if (command.getCategoryId() != null) {
            CmsSiteCategoryEntity category = requireSiteCategory(command.getCategoryId());
            Require.isTrue(command.getSiteId().equals(category.getSiteId()), CmsCode.CMS_BUSINESS_ERROR, "导航栏目不属于目标站点");
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

    private CmsNavigationEntity requireNavigation(Long id) {
        Require.notNull(id, CmsCode.CMS_BUSINESS_ERROR, "导航 ID 不能为空");
        CmsNavigationEntity entity = navigationMapper.selectOne(scopedById(id, "cms:navigation:list", NAVIGATION_SCOPE));
        Require.notNull(entity, CmsCode.CMS_BUSINESS_ERROR, "导航不存在");
        return entity;
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

    private CmsSiteCategoryEntity requireSiteCategory(Long id) {
        Require.notNull(id, CmsCode.CMS_BUSINESS_ERROR, "栏目 ID 不能为空");
        CmsSiteCategoryEntity entity = siteCategoryMapper.selectOne(scopedById(id, "cms:site-category:list", SITE_CATEGORY_SCOPE));
        Require.notNull(entity, CmsCode.CMS_BUSINESS_ERROR, "栏目不存在");
        return entity;
    }

    private CmsContentEntity requireContent(Long id) {
        Require.notNull(id, CmsCode.CMS_BUSINESS_ERROR, "内容 ID 不能为空");
        CmsContentEntity entity = contentMapper.selectOne(scopedById(id, "cms:content:list", CONTENT_SCOPE));
        Require.notNull(entity, CmsCode.CMS_BUSINESS_ERROR, "内容不存在");
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

    private static DataScopeMapping cmsScope(String tableName) {
        return DataScopeMapping.builder()
                .tableName(tableName)
                .selfField("created_by")
                .orgField("org_id")
                .tenantField("tenant_id")
                .build();
    }
}
