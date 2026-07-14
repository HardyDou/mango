package io.mango.cms.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.mango.cms.api.command.SaveCmsSiteCategoryCommand;
import io.mango.cms.api.command.UpdateCmsStatusCommand;
import io.mango.cms.api.enums.CmsAccessType;
import io.mango.cms.api.enums.CmsCategoryType;
import io.mango.cms.api.enums.CmsCode;
import io.mango.cms.api.enums.CmsStatus;
import io.mango.cms.api.query.CmsSiteCategoryTreeQuery;
import io.mango.cms.api.vo.CmsSiteCategoryVO;
import io.mango.cms.core.entity.CmsContentPublishEntity;
import io.mango.cms.core.entity.CmsSiteCategoryEntity;
import io.mango.cms.core.entity.CmsSiteEntity;
import io.mango.cms.core.mapper.CmsContentPublishMapper;
import io.mango.cms.core.mapper.CmsSiteCategoryMapper;
import io.mango.cms.core.mapper.CmsSiteMapper;
import io.mango.cms.core.service.ICmsSiteCategoryService;
import io.mango.common.result.Require;
import io.mango.infra.persistence.api.scope.DataScopeApplier;
import io.mango.infra.persistence.api.scope.DataScopeMapping;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** CMS SiteCategory aggregate service. */
@SuppressWarnings("PMD.ServiceOrDaoClassShouldEndWithImplRule")
@Service
@RequiredArgsConstructor
public class CmsSiteCategoryService implements ICmsSiteCategoryService {

    private static final String LIST_RESOURCE_SUFFIX = ":list";
    private static final DataScopeMapping SITE_SCOPE = cmsScope("cms_site");
    private static final DataScopeMapping SITE_CATEGORY_SCOPE = cmsScope("cms_site_category");
    private final CmsSiteMapper siteMapper;
    private final CmsSiteCategoryMapper siteCategoryMapper;
    private final CmsContentPublishMapper publishMapper;
    private final ObjectProvider<DataScopeApplier> dataScopeApplierProvider;

    @Override
    public List<CmsSiteCategoryVO> treeSiteCategories(CmsSiteCategoryTreeQuery query) {
        Require.notNull(query, CmsCode.CMS_BUSINESS_ERROR, "栏目查询不能为空");
        requireSite(query.getSiteId());
        List<CmsSiteCategoryEntity> records = siteCategoryMapper.selectList(siteCategoryWrapper(query));
        return buildCategoryTree(records.stream().map(this::toSiteCategoryVO).toList());
    }

    @Override
    public CmsSiteCategoryVO detailSiteCategory(Long id) {
        return toSiteCategoryVO(requireSiteCategory(id));
    }

    @Override
    public Long createSiteCategory(SaveCmsSiteCategoryCommand command) {
        Require.notNull(command, CmsCode.CMS_BUSINESS_ERROR, "栏目保存命令不能为空");
        requireSite(command.getSiteId());
        CmsSiteCategoryEntity entity = new CmsSiteCategoryEntity();
        applySiteCategory(entity, command, false);
        entity.setTenantId(CmsSupport.currentTenantId());
        siteCategoryMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public Boolean updateSiteCategory(SaveCmsSiteCategoryCommand command) {
        Require.notNull(command, CmsCode.CMS_BUSINESS_ERROR, "栏目保存命令不能为空");
        CmsSiteCategoryEntity entity = requireSiteCategory(command.getId());
        applySiteCategory(entity, command, true);
        return siteCategoryMapper.updateById(entity) > 0;
    }

    @Override
    public Boolean updateSiteCategoryStatus(UpdateCmsStatusCommand command) {
        Require.notNull(command, CmsCode.CMS_BUSINESS_ERROR, "栏目状态更新命令不能为空");
        CmsSiteCategoryEntity entity = requireSiteCategory(command.getId());
        entity.setVisibleStatus(CmsSupport.enumName(CmsStatus.class, command.getStatus(), "栏目状态非法"));
        return siteCategoryMapper.updateById(entity) > 0;
    }

    @Override
    public Boolean deleteSiteCategory(Long id) {
        CmsSiteCategoryEntity entity = requireSiteCategory(id);
        Long childCount = siteCategoryMapper.selectCount(new LambdaQueryWrapper<CmsSiteCategoryEntity>()
                .eq(CmsSiteCategoryEntity::getTenantId, CmsSupport.currentTenantId())
                .eq(CmsSiteCategoryEntity::getParentId, id));
        Long publishCount = publishMapper.selectCount(new LambdaQueryWrapper<CmsContentPublishEntity>()
                .eq(CmsContentPublishEntity::getTenantId, CmsSupport.currentTenantId())
                .eq(CmsContentPublishEntity::getCategoryId, id));
        Require.isTrue(childCount == 0 && publishCount == 0, CmsCode.CMS_BUSINESS_ERROR, "栏目存在子栏目或发布关系，不能删除");
        return siteCategoryMapper.deleteById(entity.getId()) > 0;
    }

    private void applySiteCategory(CmsSiteCategoryEntity entity, SaveCmsSiteCategoryCommand command, boolean update) {
        Require.notNull(command, CmsCode.CMS_BUSINESS_ERROR, "栏目保存命令不能为空");
        if (update) {
            Require.notNull(command.getId(), CmsCode.CMS_BUSINESS_ERROR, "栏目 ID 不能为空");
        }
        String code = CmsSupport.trimRequired(command.getCategoryCode(), "栏目编码不能为空");
        CmsSiteCategoryEntity exists = siteCategoryMapper.selectOne(new LambdaQueryWrapper<CmsSiteCategoryEntity>()
                .eq(CmsSiteCategoryEntity::getTenantId, CmsSupport.currentTenantId())
                .eq(CmsSiteCategoryEntity::getSiteId, command.getSiteId())
                .eq(CmsSiteCategoryEntity::getCategoryCode, code)
                .last("LIMIT 1"));
        Require.isTrue(exists == null || exists.getId().equals(entity.getId()), CmsCode.CMS_BUSINESS_ERROR, "栏目编码已存在");
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
        entity.setAccessType(CmsSupport.enumNameOrDefault(
                CmsAccessType.class, command.getAccessType(), CmsAccessType.PUBLIC.name(), "访问类型非法"));
        entity.setRoleCodes(CmsSupport.trimToNull(command.getRoleCodes()));
        entity.setSeoTitle(CmsSupport.trimToNull(command.getSeoTitle()));
        entity.setSeoKeywords(CmsSupport.trimToNull(command.getSeoKeywords()));
        entity.setSeoDescription(CmsSupport.trimToNull(command.getSeoDescription()));
    }

    private QueryWrapper<CmsSiteCategoryEntity> siteCategoryWrapper(CmsSiteCategoryTreeQuery query) {
        QueryWrapper<CmsSiteCategoryEntity> wrapper = new QueryWrapper<CmsSiteCategoryEntity>()
                .eq("tenant_id", CmsSupport.currentTenantId())
                .eq("site_id", query.getSiteId())
                .eq(StringUtils.hasText(query.getStatus()), "visible_status", query.getStatus())
                .orderByAsc("sort").orderByAsc("id");
        return applyDataScope(wrapper, "cms:site-category:list", SITE_CATEGORY_SCOPE);
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

    private List<CmsSiteCategoryVO> buildCategoryTree(List<CmsSiteCategoryVO> items) {
        Map<Long, CmsSiteCategoryVO> map = new HashMap<>(items.size());
        List<CmsSiteCategoryVO> roots = new ArrayList<>();
        items.forEach(item -> map.put(item.getId(), item));
        for (CmsSiteCategoryVO item : items) {
            Long parentId = CmsSupport.defaultIfNull(item.getParentId(), CmsSupport.ROOT_PARENT_ID);
            if (parentId == CmsSupport.ROOT_PARENT_ID || !map.containsKey(parentId)) {
                roots.add(item);
            } else {
                map.get(parentId).getChildren().add(item);
            }
        }
        Comparator<CmsSiteCategoryVO> comparator = Comparator.comparing(
                vo -> CmsSupport.defaultIfNull(vo.getSort(), 0));
        roots.sort(comparator);
        map.values().forEach(item -> item.getChildren().sort(comparator));
        return roots;
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
