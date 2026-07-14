package io.mango.cms.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.cms.api.enums.CmsContentStatus;
import io.mango.cms.api.enums.CmsPublishStatus;
import io.mango.cms.api.query.SiteContentDetailQuery;
import io.mango.cms.api.query.SiteContentPageQuery;
import io.mango.cms.api.query.SiteResolveQuery;
import io.mango.cms.api.vo.SiteContentVO;
import io.mango.cms.api.vo.SiteResolveVO;
import io.mango.cms.core.entity.CmsContentEntity;
import io.mango.cms.core.entity.CmsContentPublishEntity;
import io.mango.cms.core.entity.CmsSiteCategoryEntity;
import io.mango.cms.core.entity.CmsSiteEntity;
import io.mango.cms.core.mapper.CmsAdDeliveryMapper;
import io.mango.cms.core.mapper.CmsAdvertisementMapper;
import io.mango.cms.core.mapper.CmsBannerMapper;
import io.mango.cms.core.mapper.CmsContentMapper;
import io.mango.cms.core.mapper.CmsContentPublishMapper;
import io.mango.cms.core.mapper.CmsNavigationMapper;
import io.mango.cms.core.mapper.CmsSiteCategoryMapper;
import io.mango.cms.core.mapper.CmsSiteMapper;
import io.mango.common.exception.BizException;
import io.mango.common.vo.PageResult;
import io.mango.file.api.FileApi;
import io.mango.file.api.vo.FileDownloadVO;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CmsSiteServiceBehaviorTest {

    private final CmsSiteMapper siteMapper = mock(CmsSiteMapper.class);
    private final CmsSiteCategoryMapper siteCategoryMapper = mock(CmsSiteCategoryMapper.class);
    private final CmsNavigationMapper navigationMapper = mock(CmsNavigationMapper.class);
    private final CmsBannerMapper bannerMapper = mock(CmsBannerMapper.class);
    private final CmsAdvertisementMapper advertisementMapper = mock(CmsAdvertisementMapper.class);
    private final CmsAdDeliveryMapper adDeliveryMapper = mock(CmsAdDeliveryMapper.class);
    private final CmsContentMapper contentMapper = mock(CmsContentMapper.class);
    private final CmsContentPublishMapper publishMapper = mock(CmsContentPublishMapper.class);

    @AfterEach
    void clearContext() {
        MangoContextHolder.clear();
    }

    @Test
    void anonymousResolve_rejectsSiteCodeAndRequiresDomain() {
        CmsSiteService service = service(null);
        SiteResolveQuery query = new SiteResolveQuery();
        query.setSiteCode("main");

        assertThatThrownBy(() -> service.resolveSite(query))
                .isInstanceOf(BizException.class)
                .hasMessage("匿名站点解析必须提供域名");
        verify(siteMapper, never()).selectList(any());
    }

    @Test
    void resolveSite_returnsTheSingleEnabledDomainSite() {
        CmsSiteEntity site = site(10L, "tenant-a", "main", "www.example.test");
        site.setSeoTitle("Mango CMS");
        when(siteMapper.selectList(any())).thenReturn(List.of(site));
        CmsSiteService service = service(null);

        SiteResolveVO result = service.resolveSite(domainQuery("www.example.test"));

        assertThat(result)
                .extracting(SiteResolveVO::getSiteId, SiteResolveVO::getSiteCode,
                        SiteResolveVO::getSiteName, SiteResolveVO::getSeoTitle)
                .containsExactly(10L, "main", "Main Site", "Mango CMS");
    }

    @Test
    void resolveSite_rejectsMissingOrAmbiguousSite() {
        CmsSiteService service = service(null);
        when(siteMapper.selectList(any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.resolveSite(domainQuery("missing.example.test")))
                .isInstanceOf(BizException.class)
                .hasMessage("站点不存在或不唯一");

        when(siteMapper.selectList(any())).thenReturn(List.of(
                site(1L, "tenant-a", "a", "duplicate.example.test"),
                site(2L, "tenant-b", "b", "duplicate.example.test")));
        assertThatThrownBy(() -> service.resolveSite(domainQuery("duplicate.example.test")))
                .isInstanceOf(BizException.class)
                .hasMessage("站点不存在或不唯一");
    }

    @Test
    void detailContent_hidesDraftAndCrossTenantContentAsNotFound() {
        CmsSiteEntity site = site(10L, "tenant-a", "main", "www.example.test");
        when(siteMapper.selectList(any())).thenReturn(List.of(site));
        CmsContentEntity draft = content(20L, "tenant-a", CmsContentStatus.DRAFT.name());
        when(contentMapper.selectById(20L)).thenReturn(draft);
        CmsSiteService service = service(null);

        assertThatThrownBy(() -> service.detailContent(contentQuery(20L)))
                .isInstanceOf(BizException.class)
                .hasMessage("内容不存在");

        CmsContentEntity crossTenant = content(21L, "tenant-b", CmsContentStatus.PUBLISHED.name());
        when(contentMapper.selectById(21L)).thenReturn(crossTenant);
        assertThatThrownBy(() -> service.detailContent(contentQuery(21L)))
                .isInstanceOf(BizException.class)
                .hasMessage("内容不存在");
    }

    @Test
    void detailContent_requiresAnEffectivePublishRelationship() {
        CmsSiteEntity site = site(10L, "tenant-a", "main", "www.example.test");
        CmsContentEntity content = content(20L, "tenant-a", CmsContentStatus.PUBLISHED.name());
        when(siteMapper.selectList(any())).thenReturn(List.of(site));
        when(contentMapper.selectById(20L)).thenReturn(content);
        when(publishMapper.selectList(any())).thenReturn(List.of());
        CmsSiteService service = service(null);

        assertThatThrownBy(() -> service.detailContent(contentQuery(20L)))
                .isInstanceOf(BizException.class)
                .hasMessage("内容不存在");
    }

    @Test
    void detailContent_returnsPublishedContentAndItsCategory() {
        CmsSiteEntity site = site(10L, "tenant-a", "main", "www.example.test");
        CmsContentEntity content = content(20L, "tenant-a", CmsContentStatus.PUBLISHED.name());
        content.setTitle("Release notes");
        content.setCoverFileId("100");
        CmsContentPublishEntity publish = publish(30L, 20L, 10L, 40L);
        CmsSiteCategoryEntity category = new CmsSiteCategoryEntity();
        category.setId(40L);
        category.setCategoryName("News");
        when(siteMapper.selectList(any())).thenReturn(List.of(site));
        when(contentMapper.selectById(20L)).thenReturn(content);
        when(publishMapper.selectList(any())).thenReturn(List.of(publish));
        when(siteCategoryMapper.selectById(40L)).thenReturn(category);
        CmsSiteService service = service(null);

        SiteContentVO result = service.detailContent(contentQuery(20L));

        assertThat(result)
                .extracting(SiteContentVO::getId, SiteContentVO::getTitle,
                        SiteContentVO::getCategoryId, SiteContentVO::getCategoryName,
                        SiteContentVO::getCoverUrl)
                .containsExactly(20L, "Release notes", 40L, "News",
                        "/api/cms/open/files/public-preview?id=100&domain=www.example.test");
    }

    @Test
    void publicFile_allowsSiteLogoAndRestoresCallerTenantContext() {
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1L, 2L, "caller-tenant", "caller", "ADMIN", "user", "org", 3L, "admin"));
        CmsSiteEntity site = site(10L, "site-tenant", "main", "www.example.test");
        site.setLogoFileId("99");
        when(siteMapper.selectList(any())).thenReturn(List.of(site));
        FileApi fileApi = mock(FileApi.class);
        AtomicReference<String> tenantDuringDownload = new AtomicReference<>();
        FileDownloadVO download = new FileDownloadVO(
                new ByteArrayInputStream(new byte[]{1, 2, 3}), "logo.png", "image/png", 3L);
        when(fileApi.downloadForService(99L)).thenAnswer(invocation -> {
            tenantDuringDownload.set(MangoContextHolder.get().tenantId());
            return download;
        });
        CmsSiteService service = service(fileApi);

        FileDownloadVO result = service.publicFile(99L, siteCodeQuery("main"));

        assertThat(result).isSameAs(download);
        assertThat(tenantDuringDownload).hasValue("site-tenant");
        assertThat(MangoContextHolder.get().tenantId()).isEqualTo("caller-tenant");
    }

    @Test
    void publicFile_rejectsFilesThatAreNotReferencedByPublicSiteData() {
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("tenant-a"));
        CmsSiteEntity site = site(10L, "tenant-a", "main", "www.example.test");
        when(siteMapper.selectList(any())).thenReturn(List.of(site));
        when(bannerMapper.selectList(any())).thenReturn(List.of());
        when(contentMapper.selectList(any())).thenReturn(List.of());
        when(advertisementMapper.selectList(any())).thenReturn(List.of());
        FileApi fileApi = mock(FileApi.class);
        CmsSiteService service = service(fileApi);

        assertThatThrownBy(() -> service.publicFile(777L, siteCodeQuery("main")))
                .isInstanceOf(BizException.class)
                .hasMessage("文件不存在");
        verify(fileApi, never()).downloadForService(any());
    }

    @Test
    void pageContents_usesOnlyPublishedAndEffectiveContentContract() {
        CmsSiteEntity site = site(10L, "tenant-a", "main", "www.example.test");
        CmsContentEntity content = content(20L, "tenant-a", CmsContentStatus.PUBLISHED.name());
        content.setTitle("Visible article");
        CmsContentPublishEntity publish = publish(30L, 20L, 10L, 40L);
        Page<CmsContentEntity> page = new Page<>(2, 5, 11);
        page.setRecords(List.of(content));
        when(siteMapper.selectList(any())).thenReturn(List.of(site));
        when(contentMapper.selectPublicPage(any(), eq("tenant-a"), eq(10L), eq(40L), eq("HOME"),
                eq("mango"), eq(CmsPublishStatus.PUBLISHED.name()), eq(CmsPublishStatus.SCHEDULED.name()),
                eq(CmsContentStatus.PUBLISHED.name()), any(LocalDateTime.class))).thenReturn(page);
        when(publishMapper.selectList(any())).thenReturn(List.of(publish));
        when(siteCategoryMapper.selectById(40L)).thenReturn(null);
        CmsSiteService service = service(null);
        SiteContentPageQuery query = new SiteContentPageQuery();
        query.setDomain("www.example.test");
        query.setPage(2L);
        query.setSize(5L);
        query.setCategoryId(40L);
        query.setRecommendationType("HOME");
        query.setKeyword("mango");

        PageResult<SiteContentVO> result = service.pageContents(query);

        assertThat(result.getTotal()).isEqualTo(11L);
        assertThat(result.getPage()).isEqualTo(2L);
        assertThat(result.getSize()).isEqualTo(5L);
        assertThat(result.getList()).extracting(SiteContentVO::getTitle)
                .containsExactly("Visible article");
    }

    private CmsSiteService service(FileApi fileApi) {
        return new CmsSiteService(siteMapper, siteCategoryMapper, navigationMapper, bannerMapper,
                advertisementMapper, adDeliveryMapper, contentMapper, publishMapper, provider(fileApi));
    }

    private static SiteResolveQuery domainQuery(String domain) {
        SiteResolveQuery query = new SiteResolveQuery();
        query.setDomain(domain);
        return query;
    }

    private static SiteResolveQuery siteCodeQuery(String siteCode) {
        SiteResolveQuery query = new SiteResolveQuery();
        query.setSiteCode(siteCode);
        return query;
    }

    private static SiteContentDetailQuery contentQuery(Long contentId) {
        SiteContentDetailQuery query = new SiteContentDetailQuery();
        query.setDomain("www.example.test");
        query.setContentId(contentId);
        return query;
    }

    private static CmsSiteEntity site(Long id, String tenantId, String siteCode, String domain) {
        CmsSiteEntity site = new CmsSiteEntity();
        site.setId(id);
        site.setTenantId(tenantId);
        site.setSiteCode(siteCode);
        site.setSiteName("Main Site");
        site.setDomain(domain);
        site.setStatus(CmsSupport.ENABLED);
        return site;
    }

    private static CmsContentEntity content(Long id, String tenantId, String status) {
        CmsContentEntity content = new CmsContentEntity();
        content.setId(id);
        content.setTenantId(tenantId);
        content.setStatus(status);
        content.setPublishTime(LocalDateTime.now().minusHours(1));
        content.setOfflineTime(LocalDateTime.now().plusHours(1));
        return content;
    }

    private static CmsContentPublishEntity publish(Long id, Long contentId, Long siteId, Long categoryId) {
        CmsContentPublishEntity publish = new CmsContentPublishEntity();
        publish.setId(id);
        publish.setTenantId("tenant-a");
        publish.setContentId(contentId);
        publish.setSiteId(siteId);
        publish.setCategoryId(categoryId);
        publish.setPublishStatus(CmsPublishStatus.PUBLISHED.name());
        publish.setPublishTime(LocalDateTime.now().minusHours(1));
        return publish;
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
}
