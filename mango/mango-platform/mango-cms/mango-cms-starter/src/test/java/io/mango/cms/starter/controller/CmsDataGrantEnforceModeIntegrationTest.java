package io.mango.cms.starter.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.mango.cms.api.vo.CmsContentVO;
import io.mango.cms.api.vo.CmsSiteVO;
import io.mango.common.vo.PageResult;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * 强制模式下的 Grant 接入测试：不带 Grant 的管理端详情必须被拒绝，带首页 Grant 才放行。
 */
@SpringBootTest(classes = CmsDataGrantTestApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "mango.data-grant.mode=enforce",
        "mango.data-grant.secret=" + CmsDataGrantHttpTestSupport.SECRET
})
class CmsDataGrantEnforceModeIntegrationTest extends CmsDataGrantHttpTestSupport {

    @Test
    void siteDetail_withoutGrant_isForbiddenBeforeServiceCall() throws Exception {
        mockMvc.perform(get("/cms/sites/detail").param("id", Long.toString(SITE_ID)))
                .andExpect(status().isForbidden())
                .andExpect(header().string(REQUIRED_HEADER, "1"))
                                .andExpect(jsonPath("$.code").value(403));

        verifyNoInteractions(siteAdminService);
    }

    @Test
    void siteDetail_withListGrant_isAllowed() throws Exception {
        CmsSiteVO site = site(SITE_ID);
        when(siteAdminService.pageSites(any())).thenReturn(PageResult.of(List.of(site), 1, 1, 10));
        when(siteAdminService.detailSite(SITE_ID)).thenReturn(site);

        String grant = pageGrant("/cms/sites/page", 1);

        detailIsAllowed("/cms/sites/detail", "id", SITE_ID, SITE_ID, grant);
    }

    @Test
    void contentDetail_withoutGrant_isForbiddenBeforeServiceCall() throws Exception {
        mockMvc.perform(get("/cms/contents/detail").param("id", Long.toString(RECORD_ID)))
                .andExpect(status().isForbidden())
                .andExpect(header().string(REQUIRED_HEADER, "1"))
                                .andExpect(jsonPath("$.code").value(403));

        verifyNoInteractions(contentService);
    }

    @Test
    void contentDetail_withListGrant_isAllowed() throws Exception {
        CmsContentVO content = content(RECORD_ID);
        when(contentService.pageContents(any())).thenReturn(PageResult.of(List.of(content), 1, 1, 10));
        when(contentService.detailContent(RECORD_ID)).thenReturn(content);

        String grant = pageGrant("/cms/contents/page", 1);

        detailIsAllowed("/cms/contents/detail", "id", RECORD_ID, RECORD_ID, grant);
    }

    private static CmsSiteVO site(long id) {
        CmsSiteVO vo = new CmsSiteVO();
        vo.setId(id);
        return vo;
    }

    private static CmsContentVO content(long id) {
        CmsContentVO vo = new CmsContentVO();
        vo.setId(id);
        return vo;
    }
}
