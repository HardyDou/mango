package io.mango.cms.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.cms.api.command.SaveCmsContentTagCommand;
import io.mango.cms.api.command.UpdateCmsStatusCommand;
import io.mango.cms.api.enums.CmsCode;
import io.mango.cms.api.enums.CmsStatus;
import io.mango.cms.api.query.CmsContentTagPageQuery;
import io.mango.cms.api.vo.CmsContentTagVO;
import io.mango.cms.core.entity.CmsContentTagEntity;
import io.mango.cms.core.entity.CmsContentTagRelEntity;
import io.mango.cms.core.mapper.CmsContentTagMapper;
import io.mango.cms.core.mapper.CmsContentTagRelMapper;
import io.mango.cms.core.service.ICmsContentTagService;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.infra.persistence.api.scope.DataScopeApplier;
import io.mango.infra.persistence.api.scope.DataScopeMapping;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** CMS ContentTag aggregate service. */
@SuppressWarnings("PMD.ServiceOrDaoClassShouldEndWithImplRule")
@Service
@RequiredArgsConstructor
public class CmsContentTagService implements ICmsContentTagService {

    private static final String LIST_RESOURCE_SUFFIX = ":list";
    private static final DataScopeMapping CONTENT_TAG_SCOPE = cmsScope("cms_content_tag");
    private final CmsContentTagMapper contentTagMapper;
    private final CmsContentTagRelMapper contentTagRelMapper;
    private final ObjectProvider<DataScopeApplier> dataScopeApplierProvider;

    @Override
    public PageResult<CmsContentTagVO> pageContentTags(CmsContentTagPageQuery query) {
        CmsContentTagPageQuery resolved = query == null ? new CmsContentTagPageQuery() : query;
        IPage<CmsContentTagEntity> page = contentTagMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()),
                contentTagWrapper(resolved));
        return PageResult.of(page.getRecords().stream().map(this::toContentTagVO).toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public List<CmsContentTagVO> listContentTags(CmsContentTagPageQuery query) {
        CmsContentTagPageQuery resolved = query == null ? new CmsContentTagPageQuery() : query;
        if (!StringUtils.hasText(resolved.getStatus())) {
            resolved.setStatus(CmsSupport.ENABLED);
        }
        return contentTagMapper.selectList(contentTagWrapper(resolved)).stream().map(this::toContentTagVO).toList();
    }

    @Override
    public CmsContentTagVO detailContentTag(Long id) {
        return toContentTagVO(requireContentTag(id));
    }

    @Override
    public Long createContentTag(SaveCmsContentTagCommand command) {
        Require.notNull(command, CmsCode.CMS_BUSINESS_ERROR, "标签保存命令不能为空");
        CmsContentTagEntity entity = new CmsContentTagEntity();
        applyContentTag(entity, command, false);
        entity.setTenantId(CmsSupport.currentTenantId());
        contentTagMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public Boolean updateContentTag(SaveCmsContentTagCommand command) {
        Require.notNull(command, CmsCode.CMS_BUSINESS_ERROR, "标签保存命令不能为空");
        CmsContentTagEntity entity = requireContentTag(command.getId());
        applyContentTag(entity, command, true);
        return contentTagMapper.updateById(entity) > 0;
    }

    @Override
    public Boolean updateContentTagStatus(UpdateCmsStatusCommand command) {
        Require.notNull(command, CmsCode.CMS_BUSINESS_ERROR, "标签状态更新命令不能为空");
        CmsContentTagEntity entity = requireContentTag(command.getId());
        entity.setStatus(CmsSupport.enumName(CmsStatus.class, command.getStatus(), "标签状态非法"));
        return contentTagMapper.updateById(entity) > 0;
    }

    @Override
    public Boolean deleteContentTag(Long id) {
        CmsContentTagEntity entity = requireContentTag(id);
        Long count = contentTagRelMapper.selectCount(new LambdaQueryWrapper<CmsContentTagRelEntity>()
                .eq(CmsContentTagRelEntity::getTenantId, CmsSupport.currentTenantId())
                .eq(CmsContentTagRelEntity::getTagId, id));
        Require.isTrue(count == 0, CmsCode.CMS_BUSINESS_ERROR, "标签已被内容引用，不能删除");
        return contentTagMapper.deleteById(entity.getId()) > 0;
    }

    private void applyContentTag(CmsContentTagEntity entity, SaveCmsContentTagCommand command, boolean update) {
        Require.notNull(command, CmsCode.CMS_BUSINESS_ERROR, "标签保存命令不能为空");
        if (update) {
            Require.notNull(command.getId(), CmsCode.CMS_BUSINESS_ERROR, "标签 ID 不能为空");
        }
        String code = CmsSupport.trimRequired(command.getTagCode(), "标签编码不能为空");
        CmsContentTagEntity exists = contentTagMapper.selectOne(new LambdaQueryWrapper<CmsContentTagEntity>()
                .eq(CmsContentTagEntity::getTenantId, CmsSupport.currentTenantId())
                .eq(CmsContentTagEntity::getTagCode, code)
                .last("LIMIT 1"));
        Require.isTrue(exists == null || exists.getId().equals(entity.getId()), CmsCode.CMS_BUSINESS_ERROR, "标签编码已存在");
        entity.setTagCode(code);
        entity.setTagName(CmsSupport.trimRequired(command.getTagName(), "标签名称不能为空"));
        entity.setSort(CmsSupport.defaultSort(command.getSort()));
        entity.setStatus(CmsSupport.defaultStatus(command.getStatus()));
        entity.setRemark(CmsSupport.trimToNull(command.getRemark()));
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

    private CmsContentTagEntity requireContentTag(Long id) {
        Require.notNull(id, CmsCode.CMS_BUSINESS_ERROR, "标签 ID 不能为空");
        CmsContentTagEntity entity = contentTagMapper.selectOne(scopedById(id, "cms:content-tag:list", CONTENT_TAG_SCOPE));
        Require.notNull(entity, CmsCode.CMS_BUSINESS_ERROR, "标签不存在");
        return entity;
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
