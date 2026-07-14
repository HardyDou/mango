package io.mango.cms.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.cms.api.command.SaveCmsContentCategoryCommand;
import io.mango.cms.api.command.UpdateCmsStatusCommand;
import io.mango.cms.api.enums.CmsCode;
import io.mango.cms.api.enums.CmsStatus;
import io.mango.cms.api.query.CmsContentCategoryPageQuery;
import io.mango.cms.api.vo.CmsContentCategoryVO;
import io.mango.cms.core.entity.CmsContentCategoryEntity;
import io.mango.cms.core.entity.CmsContentEntity;
import io.mango.cms.core.mapper.CmsContentCategoryMapper;
import io.mango.cms.core.mapper.CmsContentMapper;
import io.mango.cms.core.service.ICmsContentCategoryService;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.infra.persistence.api.scope.DataScopeApplier;
import io.mango.infra.persistence.api.scope.DataScopeMapping;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** CMS ContentCategory aggregate service. */
@SuppressWarnings("PMD.ServiceOrDaoClassShouldEndWithImplRule")
@Service
@RequiredArgsConstructor
public class CmsContentCategoryService implements ICmsContentCategoryService {

    private static final String LIST_RESOURCE_SUFFIX = ":list";
    private static final DataScopeMapping CONTENT_CATEGORY_SCOPE = cmsScope("cms_content_category");
    private final CmsContentCategoryMapper contentCategoryMapper;
    private final CmsContentMapper contentMapper;
    private final ObjectProvider<DataScopeApplier> dataScopeApplierProvider;

    @Override
    public PageResult<CmsContentCategoryVO> pageContentCategories(CmsContentCategoryPageQuery query) {
        CmsContentCategoryPageQuery resolved = CmsSupport.defaultIfNull(query, new CmsContentCategoryPageQuery());
        IPage<CmsContentCategoryEntity> page = contentCategoryMapper.selectPage(
                new Page<>(resolved.getPage(), resolved.getSize()),
                contentCategoryWrapper(resolved));
        return PageResult.of(page.getRecords().stream().map(this::toContentCategoryVO).toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public List<CmsContentCategoryVO> listContentCategories(CmsContentCategoryPageQuery query) {
        CmsContentCategoryPageQuery resolved = CmsSupport.defaultIfNull(query, new CmsContentCategoryPageQuery());
        if (!StringUtils.hasText(resolved.getStatus())) {
            resolved.setStatus(CmsSupport.ENABLED);
        }
        return contentCategoryMapper.selectList(contentCategoryWrapper(resolved))
                .stream().map(this::toContentCategoryVO).toList();
    }

    @Override
    public List<CmsContentCategoryVO> treeContentCategories(CmsContentCategoryPageQuery query) {
        CmsContentCategoryPageQuery resolved = CmsSupport.defaultIfNull(query, new CmsContentCategoryPageQuery());
        List<CmsContentCategoryEntity> records = contentCategoryMapper.selectList(contentCategoryWrapper(resolved));
        return buildContentCategoryTree(records.stream().map(this::toContentCategoryVO).toList());
    }

    @Override
    public CmsContentCategoryVO detailContentCategory(Long id) {
        return toContentCategoryVO(requireContentCategory(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createContentCategory(SaveCmsContentCategoryCommand command) {
        Require.notNull(command, CmsCode.CMS_BUSINESS_ERROR, "分类保存命令不能为空");
        CmsContentCategoryEntity entity = new CmsContentCategoryEntity();
        applyContentCategory(entity, command, false);
        entity.setTenantId(CmsSupport.currentTenantId());
        contentCategoryMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateContentCategory(SaveCmsContentCategoryCommand command) {
        Require.notNull(command, CmsCode.CMS_BUSINESS_ERROR, "分类保存命令不能为空");
        CmsContentCategoryEntity entity = requireContentCategory(command.getId());
        applyContentCategory(entity, command, true);
        return contentCategoryMapper.updateById(entity) > 0;
    }

    @Override
    public Boolean updateContentCategoryStatus(UpdateCmsStatusCommand command) {
        Require.notNull(command, CmsCode.CMS_BUSINESS_ERROR, "分类状态更新命令不能为空");
        CmsContentCategoryEntity entity = requireContentCategory(command.getId());
        entity.setStatus(CmsSupport.enumName(CmsStatus.class, command.getStatus(), "分类状态非法"));
        return contentCategoryMapper.updateById(entity) > 0;
    }

    @Override
    public Boolean deleteContentCategory(Long id) {
        CmsContentCategoryEntity entity = requireContentCategory(id);
        Long count = contentMapper.selectCount(new LambdaQueryWrapper<CmsContentEntity>()
                .eq(CmsContentEntity::getTenantId, CmsSupport.currentTenantId())
                .eq(CmsContentEntity::getCategoryId, id));
        Require.isTrue(count == 0, CmsCode.CMS_BUSINESS_ERROR, "分类已被内容引用，不能删除");
        return contentCategoryMapper.deleteById(entity.getId()) > 0;
    }

    private void applyContentCategory(CmsContentCategoryEntity entity, SaveCmsContentCategoryCommand command, boolean update) {
        Require.notNull(command, CmsCode.CMS_BUSINESS_ERROR, "分类保存命令不能为空");
        if (update) {
            Require.notNull(command.getId(), CmsCode.CMS_BUSINESS_ERROR, "分类 ID 不能为空");
        }
        String code = CmsSupport.trimRequired(command.getCategoryCode(), "分类编码不能为空");
        CmsContentCategoryEntity exists = contentCategoryMapper.selectOne(new LambdaQueryWrapper<CmsContentCategoryEntity>()
                .eq(CmsContentCategoryEntity::getTenantId, CmsSupport.currentTenantId())
                .eq(CmsContentCategoryEntity::getCategoryCode, code)
                .last("LIMIT 1"));
        Require.isTrue(exists == null || exists.getId().equals(entity.getId()), CmsCode.CMS_BUSINESS_ERROR, "分类编码已存在");
        entity.setParentId(CmsSupport.defaultParentId(command.getParentId()));
        entity.setCategoryCode(code);
        entity.setCategoryName(CmsSupport.trimRequired(command.getCategoryName(), "分类名称不能为空"));
        entity.setSort(CmsSupport.defaultSort(command.getSort()));
        entity.setStatus(CmsSupport.defaultStatus(command.getStatus()));
        entity.setRemark(CmsSupport.trimToNull(command.getRemark()));
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

    private CmsContentCategoryEntity requireContentCategory(Long id) {
        Require.notNull(id, CmsCode.CMS_BUSINESS_ERROR, "分类 ID 不能为空");
        CmsContentCategoryEntity entity = contentCategoryMapper.selectOne(scopedById(id, "cms:content-category:list", CONTENT_CATEGORY_SCOPE));
        Require.notNull(entity, CmsCode.CMS_BUSINESS_ERROR, "分类不存在");
        return entity;
    }

    private List<CmsContentCategoryVO> buildContentCategoryTree(List<CmsContentCategoryVO> items) {
        Map<Long, CmsContentCategoryVO> map = new HashMap<>(items.size());
        List<CmsContentCategoryVO> roots = new ArrayList<>();
        items.forEach(item -> map.put(item.getId(), item));
        for (CmsContentCategoryVO item : items) {
            Long parentId = CmsSupport.defaultIfNull(item.getParentId(), CmsSupport.ROOT_PARENT_ID);
            if (parentId == CmsSupport.ROOT_PARENT_ID || !map.containsKey(parentId)) {
                roots.add(item);
            } else {
                map.get(parentId).getChildren().add(item);
            }
        }
        Comparator<CmsContentCategoryVO> comparator = Comparator.comparing(
                vo -> CmsSupport.defaultIfNull(vo.getSort(), 0));
        roots.sort(comparator);
        map.values().forEach(item -> item.getChildren().sort(comparator));
        return roots;
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
