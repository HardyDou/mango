package io.mango.cms.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.cms.api.command.CmsOfflineCommand;
import io.mango.cms.api.command.SaveCmsContentCommand;
import io.mango.cms.api.command.UpdateCmsContentReviewCommand;
import io.mango.cms.api.enums.CmsCode;
import io.mango.cms.api.enums.CmsContentStatus;
import io.mango.cms.api.enums.CmsContentType;
import io.mango.cms.api.query.CmsContentPageQuery;
import io.mango.cms.api.vo.CmsContentTagVO;
import io.mango.cms.api.vo.CmsContentVO;
import io.mango.cms.core.entity.CmsContentCategoryEntity;
import io.mango.cms.core.entity.CmsContentEntity;
import io.mango.cms.core.entity.CmsContentTagEntity;
import io.mango.cms.core.entity.CmsContentTagRelEntity;
import io.mango.cms.core.mapper.CmsContentCategoryMapper;
import io.mango.cms.core.mapper.CmsContentMapper;
import io.mango.cms.core.mapper.CmsContentTagMapper;
import io.mango.cms.core.mapper.CmsContentTagRelMapper;
import io.mango.cms.core.service.ICmsContentService;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.file.api.FileApi;
import io.mango.file.api.enums.FileRecordStatus;
import io.mango.file.api.vo.FileRecordVO;
import io.mango.infra.persistence.api.scope.DataScopeApplier;
import io.mango.infra.persistence.api.scope.DataScopeMapping;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** CMS Content aggregate service. */
@SuppressWarnings("PMD.ServiceOrDaoClassShouldEndWithImplRule")
@Service
@RequiredArgsConstructor
public class CmsContentService implements ICmsContentService {

    private static final String LIST_RESOURCE_SUFFIX = ":list";
    private static final DataScopeMapping CONTENT_CATEGORY_SCOPE = cmsScope("cms_content_category");
    private static final DataScopeMapping CONTENT_TAG_SCOPE = cmsScope("cms_content_tag");
    private static final DataScopeMapping CONTENT_SCOPE = cmsScope("cms_content");
    private final CmsContentCategoryMapper contentCategoryMapper;
    private final CmsContentTagMapper contentTagMapper;
    private final CmsContentTagRelMapper contentTagRelMapper;
    private final CmsContentMapper contentMapper;
    private final ObjectProvider<DataScopeApplier> dataScopeApplierProvider;
    private final ObjectProvider<FileApi> fileApiProvider;

    @Override
    public PageResult<CmsContentVO> pageContents(CmsContentPageQuery query) {
        CmsContentPageQuery resolved = CmsSupport.defaultIfNull(query, new CmsContentPageQuery());
        IPage<CmsContentEntity> page = contentMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()),
                contentWrapper(resolved));
        return PageResult.of(page.getRecords().stream().map(this::toContentVO).toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public CmsContentVO detailContent(Long id) {
        return toContentVO(requireContent(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createContent(SaveCmsContentCommand command) {
        Require.notNull(command, CmsCode.CMS_BUSINESS_ERROR, "内容保存命令不能为空");
        CmsContentEntity entity = new CmsContentEntity();
        applyContent(entity, command);
        entity.setTenantId(CmsSupport.currentTenantId());
        entity.setStatus(CmsContentStatus.DRAFT.name());
        contentMapper.insert(entity);
        saveContentTags(entity.getId(), command.getTagIds());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateContent(SaveCmsContentCommand command) {
        CmsContentEntity entity = requireContent(command.getId());
        Require.isTrue(CmsContentStatus.DRAFT.name().equals(entity.getStatus())
                || CmsContentStatus.REJECTED.name().equals(entity.getStatus())
                || CmsContentStatus.PUBLISHED.name().equals(entity.getStatus()), CmsCode.CMS_BUSINESS_ERROR, "当前内容状态不能编辑");
        applyContent(entity, command);
        boolean updated = contentMapper.updateById(entity) > 0;
        saveContentTags(entity.getId(), command.getTagIds());
        return updated;
    }

    @Override
    public Boolean submitContent(CmsOfflineCommand command) {
        CmsContentEntity entity = requireContent(command.getId());
        Require.isTrue(CmsContentStatus.DRAFT.name().equals(entity.getStatus())
                || CmsContentStatus.REJECTED.name().equals(entity.getStatus()), CmsCode.CMS_BUSINESS_ERROR, "只有草稿或驳回内容可以提交审核");
        entity.setStatus(CmsContentStatus.PENDING_REVIEW.name());
        return contentMapper.updateById(entity) > 0;
    }

    @Override
    public Boolean approveContent(UpdateCmsContentReviewCommand command) {
        CmsContentEntity entity = requireContent(command.getId());
        Require.isTrue(CmsContentStatus.PENDING_REVIEW.name().equals(entity.getStatus()), CmsCode.CMS_BUSINESS_ERROR, "只有待审核内容可以审核通过");
        entity.setStatus(CmsContentStatus.PUBLISHED.name());
        entity.setReviewComment(CmsSupport.trimToNull(command.getReviewComment()));
        entity.setPublishTime(LocalDateTime.now());
        return contentMapper.updateById(entity) > 0;
    }

    @Override
    public Boolean rejectContent(UpdateCmsContentReviewCommand command) {
        CmsContentEntity entity = requireContent(command.getId());
        Require.isTrue(CmsContentStatus.PENDING_REVIEW.name().equals(entity.getStatus()), CmsCode.CMS_BUSINESS_ERROR, "只有待审核内容可以驳回");
        entity.setStatus(CmsContentStatus.REJECTED.name());
        entity.setReviewComment(CmsSupport.trimToNull(command.getReviewComment()));
        return contentMapper.updateById(entity) > 0;
    }

    @Override
    public Boolean offlineContent(CmsOfflineCommand command) {
        Require.notNull(command, CmsCode.CMS_BUSINESS_ERROR, "内容下线命令不能为空");
        CmsContentEntity entity = requireContent(command.getId());
        entity.setStatus(CmsContentStatus.OFFLINE.name());
        entity.setOfflineTime(LocalDateTime.now());
        return contentMapper.updateById(entity) > 0;
    }

    @Override
    public Boolean deleteContent(Long id) {
        CmsContentEntity entity = requireContent(id);
        Require.isTrue(!CmsContentStatus.PUBLISHED.name().equals(entity.getStatus()), CmsCode.CMS_BUSINESS_ERROR, "已发布内容需先下线再删除");
        return contentMapper.deleteById(entity.getId()) > 0;
    }

    private void applyContent(CmsContentEntity entity, SaveCmsContentCommand command) {
        Require.notNull(command, CmsCode.CMS_BUSINESS_ERROR, "内容保存命令不能为空");
        entity.setTitle(CmsSupport.trimRequired(command.getTitle(), "标题不能为空"));
        entity.setSubtitle(CmsSupport.trimToNull(command.getSubtitle()));
        entity.setSummary(CmsSupport.trimToNull(command.getSummary()));
        entity.setContentType(CmsSupport.enumName(CmsContentType.class, command.getContentType(), "内容类型非法"));
        entity.setCoverFileId(validateImageFile(command.getCoverFileId(), "内容封面文件"));
        entity.setBody(CmsSupport.trimToNull(command.getBody()));
        entity.setExternalUrl(normalizePublicUrl(command.getExternalUrl(), "内容外链地址非法"));
        entity.setAttachmentFileId(validateAnyFile(command.getAttachmentFileId(), "内容附件文件"));
        entity.setVideoFileId(validateVideoFile(command.getVideoFileId(), "内容视频文件"));
        entity.setSource(CmsSupport.trimToNull(command.getSource()));
        entity.setAuthor(CmsSupport.trimToNull(command.getAuthor()));
        if (command.getCategoryId() != null) {
            requireContentCategory(command.getCategoryId());
        }
        entity.setCategoryId(command.getCategoryId());
        entity.setSeoTitle(CmsSupport.trimToNull(command.getSeoTitle()));
        entity.setSeoKeywords(CmsSupport.trimToNull(command.getSeoKeywords()));
        entity.setSeoDescription(CmsSupport.trimToNull(command.getSeoDescription()));
        entity.setPublishTime(command.getPublishTime());
        entity.setOfflineTime(command.getOfflineTime());
    }

    private void saveContentTags(Long contentId, List<Long> tagIds) {
        contentTagRelMapper.delete(new LambdaQueryWrapper<CmsContentTagRelEntity>()
                .eq(CmsContentTagRelEntity::getTenantId, CmsSupport.currentTenantId())
                .eq(CmsContentTagRelEntity::getContentId, contentId));
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        for (Long tagId : tagIds) {
            requireContentTag(tagId);
            CmsContentTagRelEntity rel = new CmsContentTagRelEntity();
            rel.setTenantId(CmsSupport.currentTenantId());
            rel.setContentId(contentId);
            rel.setTagId(tagId);
            contentTagRelMapper.insert(rel);
        }
    }

    private QueryWrapper<CmsContentEntity> contentWrapper(CmsContentPageQuery query) {
        String keyword = CmsSupport.trimToNull(query.getKeyword());
        QueryWrapper<CmsContentEntity> wrapper = new QueryWrapper<CmsContentEntity>()
                .eq("tenant_id", CmsSupport.currentTenantId())
                .eq(query.getCategoryId() != null, "category_id", query.getCategoryId())
                .eq(StringUtils.hasText(query.getStatus()), "status", query.getStatus())
                .eq(StringUtils.hasText(query.getContentType()), "content_type", query.getContentType())
                .and(StringUtils.hasText(keyword), w -> w.like("title", keyword).or().like("summary", keyword))
                .orderByDesc("updated_at");
        return applyDataScope(wrapper, "cms:content:list", CONTENT_SCOPE);
    }

    private CmsContentEntity requireContent(Long id) {
        Require.notNull(id, CmsCode.CMS_BUSINESS_ERROR, "内容 ID 不能为空");
        CmsContentEntity entity = contentMapper.selectOne(scopedById(id, "cms:content:list", CONTENT_SCOPE));
        Require.notNull(entity, CmsCode.CMS_BUSINESS_ERROR, "内容不存在");
        return entity;
    }

    private CmsContentVO toContentVO(CmsContentEntity e) {
        CmsContentVO vo = new CmsContentVO();
        vo.setId(e.getId());
        vo.setTitle(e.getTitle());
        vo.setSubtitle(e.getSubtitle());
        vo.setSummary(e.getSummary());
        vo.setContentType(e.getContentType());
        vo.setCoverFileId(e.getCoverFileId());
        vo.setBody(e.getBody());
        vo.setExternalUrl(e.getExternalUrl());
        vo.setAttachmentFileId(e.getAttachmentFileId());
        vo.setVideoFileId(e.getVideoFileId());
        vo.setSource(e.getSource());
        vo.setAuthor(e.getAuthor());
        vo.setCategoryId(e.getCategoryId());
        if (e.getCategoryId() != null) {
            CmsContentCategoryEntity category = contentCategoryMapper.selectById(e.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getCategoryName());
            }
        }
        vo.setTags(loadTags(e.getId()));
        vo.setSeoTitle(e.getSeoTitle());
        vo.setSeoKeywords(e.getSeoKeywords());
        vo.setSeoDescription(e.getSeoDescription());
        vo.setStatus(e.getStatus());
        vo.setPublishTime(e.getPublishTime());
        vo.setOfflineTime(e.getOfflineTime());
        vo.setReviewComment(e.getReviewComment());
        vo.setOrgId(e.getOrgId());
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

    private CmsContentCategoryEntity requireContentCategory(Long id) {
        Require.notNull(id, CmsCode.CMS_BUSINESS_ERROR, "分类 ID 不能为空");
        CmsContentCategoryEntity entity = contentCategoryMapper.selectOne(scopedById(id, "cms:content-category:list", CONTENT_CATEGORY_SCOPE));
        Require.notNull(entity, CmsCode.CMS_BUSINESS_ERROR, "分类不存在");
        return entity;
    }

    private CmsContentTagEntity requireContentTag(Long id) {
        Require.notNull(id, CmsCode.CMS_BUSINESS_ERROR, "标签 ID 不能为空");
        CmsContentTagEntity entity = contentTagMapper.selectOne(scopedById(id, "cms:content-tag:list", CONTENT_TAG_SCOPE));
        Require.notNull(entity, CmsCode.CMS_BUSINESS_ERROR, "标签不存在");
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

    private String validateAnyFile(String value, String fieldName) {
        return validateFile(value, fieldName, null);
    }

    private List<CmsContentTagVO> loadTags(Long contentId) {
        List<CmsContentTagRelEntity> rels = contentTagRelMapper.selectList(new LambdaQueryWrapper<CmsContentTagRelEntity>()
                .eq(CmsContentTagRelEntity::getTenantId, CmsSupport.currentTenantId())
                .eq(CmsContentTagRelEntity::getContentId, contentId));
        return rels.stream()
                .map(rel -> contentTagMapper.selectById(rel.getTagId()))
                .filter(tag -> tag != null && CmsSupport.currentTenantId().equals(tag.getTenantId()))
                .map(this::toContentTagVO)
                .toList();
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
