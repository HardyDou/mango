package io.mango.cms.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.cms.api.command.BatchCmsContentPublishCommand;
import io.mango.cms.api.command.CmsOfflineCommand;
import io.mango.cms.api.command.SaveCmsSiteCommand;
import io.mango.cms.api.command.UpdateCmsContentReviewCommand;
import io.mango.cms.api.enums.CmsContentStatus;
import io.mango.cms.api.query.CmsSitePageQuery;
import io.mango.cms.core.entity.CmsContentEntity;
import io.mango.cms.core.entity.CmsSiteCategoryEntity;
import io.mango.cms.core.entity.CmsSiteEntity;
import io.mango.cms.core.mapper.CmsAdDeliveryMapper;
import io.mango.cms.core.mapper.CmsAdvertisementMapper;
import io.mango.cms.core.mapper.CmsBannerMapper;
import io.mango.cms.core.mapper.CmsContentCategoryMapper;
import io.mango.cms.core.mapper.CmsContentMapper;
import io.mango.cms.core.mapper.CmsContentPublishMapper;
import io.mango.cms.core.mapper.CmsContentTagMapper;
import io.mango.cms.core.mapper.CmsContentTagRelMapper;
import io.mango.cms.core.mapper.CmsNavigationMapper;
import io.mango.cms.core.mapper.CmsSiteCategoryMapper;
import io.mango.cms.core.mapper.CmsSiteMapper;
import io.mango.cms.core.mapper.CmsSiteSettingMapper;
import io.mango.common.exception.BizException;
import io.mango.common.result.R;
import io.mango.file.api.FileApi;
import io.mango.file.api.enums.FileRecordStatus;
import io.mango.file.api.vo.FileRecordVO;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.persistence.api.scope.DataScopeApplier;
import io.mango.infra.persistence.api.scope.DataScopeMapping;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CmsAdminServiceSecurityTest {

    private final CmsContentCategoryMapper contentCategoryMapper = mock(CmsContentCategoryMapper.class);
    private final CmsContentTagMapper contentTagMapper = mock(CmsContentTagMapper.class);
    private final CmsContentTagRelMapper contentTagRelMapper = mock(CmsContentTagRelMapper.class);
    private final CmsSiteMapper siteMapper = mock(CmsSiteMapper.class);
    private final CmsSiteCategoryMapper siteCategoryMapper = mock(CmsSiteCategoryMapper.class);
    private final CmsContentMapper contentMapper = mock(CmsContentMapper.class);
    private final CmsContentPublishMapper publishMapper = mock(CmsContentPublishMapper.class);
    private final CmsNavigationMapper navigationMapper = mock(CmsNavigationMapper.class);
    private final CmsBannerMapper bannerMapper = mock(CmsBannerMapper.class);
    private final CmsAdvertisementMapper advertisementMapper = mock(CmsAdvertisementMapper.class);
    private final CmsAdDeliveryMapper adDeliveryMapper = mock(CmsAdDeliveryMapper.class);
    private final CmsSiteSettingMapper siteSettingMapper = mock(CmsSiteSettingMapper.class);

    @BeforeEach
    void setUp() {
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1001L, 2001L, "tenant-a", "admin", "ADMIN", "user", "org", 3001L, "mango-admin"));
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void pageSites_应用统一数据权限资源和字段映射() {
        RecordingDataScopeApplier applier = new RecordingDataScopeApplier();
        CmsAdminService service = service(applier, null);
        when(siteMapper.selectPage(any(), any(QueryWrapper.class))).thenReturn(new Page<CmsSiteEntity>(1, 10));

        service.pageSites(new CmsSitePageQuery());

        assertThat(applier.calls).hasSize(1);
        DataScopeCall call = applier.calls.get(0);
        assertThat(call.resourceCode).isEqualTo("cms:site:list");
        assertThat(call.mapping.tableName()).isEqualTo("cms_site");
        assertThat(call.mapping.selfField()).isEqualTo("created_by");
        assertThat(call.mapping.orgField()).isEqualTo("org_id");
        assertThat(call.mapping.tenantField()).isEqualTo("tenant_id");
        verify(siteMapper).selectPage(any(), any(QueryWrapper.class));
    }

    @Test
    void detailSite_使用带数据权限的对象级查询() {
        RecordingDataScopeApplier applier = new RecordingDataScopeApplier();
        CmsSiteEntity site = new CmsSiteEntity();
        site.setId(10L);
        site.setTenantId("tenant-a");
        site.setSiteCode("main");
        site.setSiteName("Main");
        when(siteMapper.selectOne(any(Wrapper.class))).thenReturn(site);
        CmsAdminService service = service(applier, null);

        service.detailSite(10L);

        assertThat(applier.calls).extracting(call -> call.resourceCode).containsExactly("cms:site:list");
        verify(siteMapper).selectOne(any(QueryWrapper.class));
    }

    @Test
    void createSite_文件引用不存在或不可见时拒绝保存() {
        FileApi fileApi = mock(FileApi.class);
        when(fileApi.get(11L)).thenReturn(R.fail("文件不存在"));
        CmsAdminService service = service(null, fileApi);

        SaveCmsSiteCommand command = siteCommand("11");

        assertThatThrownBy(() -> service.createSite(command))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("站点 Logo 文件不存在或不可见");
    }

    @Test
    void createSite_文件未完成或类型不匹配时拒绝保存() {
        FileApi fileApi = mock(FileApi.class);
        FileRecordVO uploading = file(12L, FileRecordStatus.UPLOADING.value(), 0, "image/png");
        when(fileApi.get(12L)).thenReturn(R.ok(uploading));
        CmsAdminService service = service(null, fileApi);

        assertThatThrownBy(() -> service.createSite(siteCommand("mango-file:12")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("站点 Logo 文件未上传完成");

        FileRecordVO video = file(13L, FileRecordStatus.COMPLETED.value(), 0, "video/mp4");
        when(fileApi.get(13L)).thenReturn(R.ok(video));
        assertThatThrownBy(() -> service.createSite(siteCommand("13")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("站点 Logo 文件类型不匹配");
    }

    @Test
    void createSite_合法图片文件允许保存() {
        FileApi fileApi = mock(FileApi.class);
        when(fileApi.get(21L)).thenReturn(R.ok(file(21L, FileRecordStatus.COMPLETED.value(), 0, "image/png")));
        doAnswer(invocation -> {
            CmsSiteEntity entity = invocation.getArgument(0);
            entity.setId(99L);
            return 1;
        }).when(siteMapper).insert(any(CmsSiteEntity.class));
        CmsSiteEntity site = new CmsSiteEntity();
        site.setId(99L);
        site.setTenantId("tenant-a");
        site.setSiteCode("main");
        site.setSiteName("Main Site");
        when(siteMapper.selectOne(any(Wrapper.class))).thenReturn(null, site);
        CmsAdminService service = service(null, fileApi);

        service.createSite(siteCommand("21"));

        verify(siteMapper).insert(any(CmsSiteEntity.class));
    }

    @Test
    void submitContent_movesDraftToPendingReview() {
        CmsContentEntity content = content(31L, CmsContentStatus.DRAFT.name());
        when(contentMapper.selectOne(any(Wrapper.class))).thenReturn(content);
        when(contentMapper.updateById(content)).thenReturn(1);
        CmsAdminService service = service(null, null);

        service.submitContent(offlineCommand(31L));

        assertThat(content.getStatus()).isEqualTo(CmsContentStatus.PENDING_REVIEW.name());
        verify(contentMapper).updateById(content);
    }

    @Test
    void submitContent_rejectsStatesOtherThanDraftOrRejected() {
        CmsContentEntity content = content(32L, CmsContentStatus.PUBLISHED.name());
        when(contentMapper.selectOne(any(Wrapper.class))).thenReturn(content);
        CmsAdminService service = service(null, null);

        assertThatThrownBy(() -> service.submitContent(offlineCommand(32L)))
                .isInstanceOf(BizException.class)
                .hasMessage("只有草稿或驳回内容可以提交审核");
        verify(contentMapper, never()).updateById(any(CmsContentEntity.class));
    }

    @Test
    void approveContent_movesPendingReviewToPublishedAndRecordsReview() {
        CmsContentEntity content = content(33L, CmsContentStatus.PENDING_REVIEW.name());
        when(contentMapper.selectOne(any(Wrapper.class))).thenReturn(content);
        when(contentMapper.updateById(content)).thenReturn(1);
        CmsAdminService service = service(null, null);
        UpdateCmsContentReviewCommand command = reviewCommand(33L, "  approved  ");

        service.approveContent(command);

        assertThat(content.getStatus()).isEqualTo(CmsContentStatus.PUBLISHED.name());
        assertThat(content.getReviewComment()).isEqualTo("approved");
        assertThat(content.getPublishTime()).isNotNull();
        verify(contentMapper).updateById(content);
    }

    @Test
    void rejectContent_movesPendingReviewToRejectedAndKeepsReason() {
        CmsContentEntity content = content(34L, CmsContentStatus.PENDING_REVIEW.name());
        when(contentMapper.selectOne(any(Wrapper.class))).thenReturn(content);
        when(contentMapper.updateById(content)).thenReturn(1);
        CmsAdminService service = service(null, null);

        service.rejectContent(reviewCommand(34L, "  needs changes  "));

        assertThat(content.getStatus()).isEqualTo(CmsContentStatus.REJECTED.name());
        assertThat(content.getReviewComment()).isEqualTo("needs changes");
    }

    @Test
    void deleteContent_requiresPublishedContentToBeTakenOfflineFirst() {
        CmsContentEntity content = content(35L, CmsContentStatus.PUBLISHED.name());
        when(contentMapper.selectOne(any(Wrapper.class))).thenReturn(content);
        CmsAdminService service = service(null, null);

        assertThatThrownBy(() -> service.deleteContent(35L))
                .isInstanceOf(BizException.class)
                .hasMessage("已发布内容需先下线再删除");
        verify(contentMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void publishContents_rejectsCategoryFromAnotherSite() {
        CmsSiteEntity site = new CmsSiteEntity();
        site.setId(41L);
        CmsSiteCategoryEntity category = new CmsSiteCategoryEntity();
        category.setId(42L);
        category.setSiteId(99L);
        when(siteMapper.selectOne(any(Wrapper.class))).thenReturn(site);
        when(siteCategoryMapper.selectOne(any(Wrapper.class))).thenReturn(category);
        CmsAdminService service = service(null, null);
        BatchCmsContentPublishCommand command = new BatchCmsContentPublishCommand();
        command.setSiteId(41L);
        command.setContentIds(List.of(43L));
        command.setCategoryIds(List.of(42L));

        assertThatThrownBy(() -> service.publishContents(command))
                .isInstanceOf(BizException.class)
                .hasMessage("发布栏目不属于目标站点");
        verify(contentMapper, never()).selectOne(any(Wrapper.class));
    }

    private CmsAdminService service(DataScopeApplier dataScopeApplier, FileApi fileApi) {
        return new CmsAdminService(
                contentCategoryMapper,
                contentTagMapper,
                contentTagRelMapper,
                siteMapper,
                siteCategoryMapper,
                contentMapper,
                publishMapper,
                navigationMapper,
                bannerMapper,
                advertisementMapper,
                adDeliveryMapper,
                siteSettingMapper,
                provider(dataScopeApplier),
                provider(fileApi));
    }

    private static SaveCmsSiteCommand siteCommand(String logoFileId) {
        SaveCmsSiteCommand command = new SaveCmsSiteCommand();
        command.setSiteName("Main Site");
        command.setSiteCode("main");
        command.setLogoFileId(logoFileId);
        command.setStatus("ENABLED");
        return command;
    }

    private static CmsContentEntity content(Long id, String status) {
        CmsContentEntity content = new CmsContentEntity();
        content.setId(id);
        content.setTenantId("tenant-a");
        content.setStatus(status);
        return content;
    }

    private static CmsOfflineCommand offlineCommand(Long id) {
        CmsOfflineCommand command = new CmsOfflineCommand();
        command.setId(id);
        return command;
    }

    private static UpdateCmsContentReviewCommand reviewCommand(Long id, String comment) {
        UpdateCmsContentReviewCommand command = new UpdateCmsContentReviewCommand();
        command.setId(id);
        command.setReviewComment(comment);
        return command;
    }

    private static FileRecordVO file(Long id, int status, int archived, String contentType) {
        FileRecordVO file = new FileRecordVO();
        file.setId(id);
        file.setStatus(status);
        file.setArchived(archived);
        file.setContentType(contentType);
        return file;
    }

    private static <T> ObjectProvider<T> provider(T value) {
        return new ObjectProvider<>() {
            @Override
            public T getObject(Object... args) {
                return value;
            }

            @Override
            public T getIfAvailable() {
                return value;
            }

            @Override
            public T getIfUnique() {
                return value;
            }

            @Override
            public T getObject() {
                return value;
            }
        };
    }

    private static final class RecordingDataScopeApplier implements DataScopeApplier {
        private final List<DataScopeCall> calls = new ArrayList<>();

        @Override
        public <T> void apply(QueryWrapper<T> wrapper, String resourceCode, DataScopeMapping mapping) {
            wrapper.eq("created_by", 1001L);
            calls.add(new DataScopeCall(resourceCode, mapping));
        }
    }

    private record DataScopeCall(String resourceCode, DataScopeMapping mapping) {
    }
}
