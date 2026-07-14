package io.mango.cms.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.cms.api.command.BatchCmsContentPublishCommand;
import io.mango.cms.api.command.CmsOfflineCommand;
import io.mango.cms.api.enums.CmsCode;
import io.mango.cms.api.enums.CmsContentStatus;
import io.mango.cms.api.enums.CmsPublishStatus;
import io.mango.cms.api.enums.CmsRecommendationType;
import io.mango.cms.api.enums.CmsTopScope;
import io.mango.cms.api.query.CmsContentPublishPageQuery;
import io.mango.cms.api.vo.CmsContentPublishVO;
import io.mango.cms.core.entity.CmsContentEntity;
import io.mango.cms.core.entity.CmsContentPublishEntity;
import io.mango.cms.core.entity.CmsSiteCategoryEntity;
import io.mango.cms.core.entity.CmsSiteEntity;
import io.mango.cms.core.mapper.CmsContentMapper;
import io.mango.cms.core.mapper.CmsContentPublishMapper;
import io.mango.cms.core.mapper.CmsSiteCategoryMapper;
import io.mango.cms.core.mapper.CmsSiteMapper;
import io.mango.cms.core.service.ICmsContentPublishService;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.infra.persistence.api.scope.DataScopeApplier;
import io.mango.infra.persistence.api.scope.DataScopeMapping;
import java.time.LocalDateTime;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** CMS ContentPublish aggregate service. */
@SuppressWarnings("PMD.ServiceOrDaoClassShouldEndWithImplRule")
@Service
@RequiredArgsConstructor
public class CmsContentPublishService implements ICmsContentPublishService {

    private static final String LIST_RESOURCE_SUFFIX = ":list";
    private static final DataScopeMapping SITE_SCOPE = cmsScope("cms_site");
    private static final DataScopeMapping SITE_CATEGORY_SCOPE = cmsScope("cms_site_category");
    private static final DataScopeMapping CONTENT_SCOPE = cmsScope("cms_content");
    private static final DataScopeMapping PUBLISH_SCOPE = cmsScope("cms_content_publish");
    private final CmsSiteMapper siteMapper;
    private final CmsSiteCategoryMapper siteCategoryMapper;
    private final CmsContentMapper contentMapper;
    private final CmsContentPublishMapper publishMapper;
    private final ObjectProvider<DataScopeApplier> dataScopeApplierProvider;

    @Override
    public PageResult<CmsContentPublishVO> pagePublishes(CmsContentPublishPageQuery query) {
        CmsContentPublishPageQuery resolved = CmsSupport.defaultIfNull(query, new CmsContentPublishPageQuery());
        IPage<CmsContentPublishEntity> page = publishMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()),
                publishWrapper(resolved));
        return PageResult.of(page.getRecords().stream().map(this::toPublishVO).toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean publishContents(BatchCmsContentPublishCommand command) {
        Require.notNull(command, CmsCode.CMS_BUSINESS_ERROR, "发布命令不能为空");
        requireSite(command.getSiteId());
        Require.isTrue(command.getContentIds() != null && !command.getContentIds().isEmpty(), CmsCode.CMS_BUSINESS_ERROR, "发布内容不能为空");
        Require.isTrue(command.getCategoryIds() != null && !command.getCategoryIds().isEmpty(), CmsCode.CMS_BUSINESS_ERROR, "发布栏目不能为空");
        for (Long categoryId : command.getCategoryIds()) {
            CmsSiteCategoryEntity category = requireSiteCategory(categoryId);
            Require.isTrue(command.getSiteId().equals(category.getSiteId()), CmsCode.CMS_BUSINESS_ERROR, "发布栏目不属于目标站点");
        }
        for (Long contentId : command.getContentIds()) {
            CmsContentEntity content = requireContent(contentId);
            Require.isTrue(CmsContentStatus.PUBLISHED.name().equals(content.getStatus()), CmsCode.CMS_BUSINESS_ERROR, "只有审核通过内容可以发布");
            for (Long categoryId : command.getCategoryIds()) {
                upsertPublish(command, contentId, categoryId);
            }
        }
        return true;
    }

    @Override
    public Boolean offlinePublish(CmsOfflineCommand command) {
        Require.notNull(command, CmsCode.CMS_BUSINESS_ERROR, "发布下线命令不能为空");
        CmsContentPublishEntity entity = requirePublish(command.getId());
        entity.setPublishStatus(CmsPublishStatus.OFFLINE.name());
        entity.setOfflineTime(LocalDateTime.now());
        return publishMapper.updateById(entity) > 0;
    }

    @Override
    public Boolean deletePublish(Long id) {
        Require.notNull(id, CmsCode.CMS_BUSINESS_ERROR, "发布关系 ID 不能为空");
        CmsContentPublishEntity entity = requirePublish(id);
        return publishMapper.deleteById(entity.getId()) > 0;
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
        if (command.getScheduledPublishTime() == null) {
            entity.setPublishStatus(CmsPublishStatus.PUBLISHED.name());
        } else {
            entity.setPublishStatus(CmsPublishStatus.SCHEDULED.name());
        }
        entity.setPublishTime(CmsSupport.defaultIfNull(command.getPublishTime(), LocalDateTime.now()));
        entity.setScheduledPublishTime(command.getScheduledPublishTime());
        entity.setOfflineTime(command.getOfflineTime());
        entity.setTop(Boolean.TRUE.equals(command.getTop()));
        entity.setTopScope(CmsSupport.enumNameOrDefault(
                CmsTopScope.class, command.getTopScope(), CmsTopScope.NONE.name(), "置顶范围非法"));
        entity.setRecommended(Boolean.TRUE.equals(command.getRecommended()));
        entity.setRecommendationType(CmsSupport.enumNameOrDefault(CmsRecommendationType.class,
                command.getRecommendationType(), CmsRecommendationType.NONE.name(), "推荐类型非法"));
        entity.setSort(CmsSupport.defaultSort(command.getSort()));
        if (create) {
            publishMapper.insert(entity);
        } else {
            publishMapper.updateById(entity);
        }
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

    private CmsContentPublishEntity requirePublish(Long id) {
        Require.notNull(id, CmsCode.CMS_BUSINESS_ERROR, "发布关系 ID 不能为空");
        CmsContentPublishEntity entity = publishMapper.selectOne(scopedById(id, "cms:publish:list", PUBLISH_SCOPE));
        Require.notNull(entity, CmsCode.CMS_BUSINESS_ERROR, "发布关系不存在");
        return entity;
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
        return CmsSupport.defaultIfNull(value, "").replace("'", "''");
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
