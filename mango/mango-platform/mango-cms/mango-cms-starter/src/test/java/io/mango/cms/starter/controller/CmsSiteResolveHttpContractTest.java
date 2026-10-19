package io.mango.cms.starter.controller;

import io.mango.cms.core.entity.CmsSiteEntity;
import io.mango.cms.core.mapper.CmsAdDeliveryMapper;
import io.mango.cms.core.mapper.CmsAdvertisementMapper;
import io.mango.cms.core.mapper.CmsBannerMapper;
import io.mango.cms.core.mapper.CmsContentMapper;
import io.mango.cms.core.mapper.CmsContentPublishMapper;
import io.mango.cms.core.mapper.CmsNavigationMapper;
import io.mango.cms.core.mapper.CmsSiteCategoryMapper;
import io.mango.cms.core.mapper.CmsSiteMapper;
import io.mango.cms.core.service.impl.CmsSiteService;
import io.mango.file.api.IFileContentProvider;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.web.starter.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CmsSiteResolveHttpContractTest {

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
    void resolveSite_anonymousSiteCode_returnsResolvedSite() throws Exception {
        when(siteMapper.selectList(any())).thenReturn(List.of(site(10L, "tenant-a", "main", "www.example.test")));

        mvc().perform(get("/cms/open/sites/resolve").queryParam("siteCode", "main"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.siteId").value(10))
                .andExpect(jsonPath("$.data.siteCode").value("main"));
    }

    @Test
    void resolveSite_anonymousWithoutSelector_returnsBusinessError() throws Exception {
        mvc().perform(get("/cms/open/sites/resolve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("站点编码或域名不能为空"));
    }

    @Test
    void resolveSite_anonymousAmbiguousSiteCode_returnsBusinessError() throws Exception {
        when(siteMapper.selectList(any())).thenReturn(List.of(
                site(1L, "tenant-a", "main", "a.example.test"),
                site(2L, "tenant-b", "main", "b.example.test")));

        mvc().perform(get("/cms/open/sites/resolve").queryParam("siteCode", "main"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("站点不存在或不唯一"));
    }

    private MockMvc mvc() {
        CmsSiteService service = new CmsSiteService(siteMapper, siteCategoryMapper, navigationMapper,
                bannerMapper, advertisementMapper, adDeliveryMapper, contentMapper, publishMapper, provider());
        return MockMvcBuilders.standaloneSetup(new CmsSiteController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static CmsSiteEntity site(Long id, String tenantId, String siteCode, String domain) {
        CmsSiteEntity site = new CmsSiteEntity();
        site.setId(id);
        site.setTenantId(tenantId);
        site.setSiteCode(siteCode);
        site.setSiteName("Main Site");
        site.setDomain(domain);
        site.setStatus("ENABLED");
        return site;
    }

    private static ObjectProvider<IFileContentProvider> provider() {
        return new ObjectProvider<>() {
            @Override
            public IFileContentProvider getObject(Object... args) {
                return null;
            }

            @Override
            public IFileContentProvider getIfAvailable() {
                return null;
            }

            @Override
            public IFileContentProvider getIfUnique() {
                return null;
            }

            @Override
            public IFileContentProvider getObject() {
                return null;
            }
        };
    }
}
