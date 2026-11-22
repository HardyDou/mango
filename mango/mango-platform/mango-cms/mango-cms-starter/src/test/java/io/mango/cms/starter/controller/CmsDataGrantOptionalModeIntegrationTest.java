package io.mango.cms.starter.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.mango.cms.api.vo.CmsAdDeliveryVO;
import io.mango.cms.api.vo.CmsAdvertisementVO;
import io.mango.cms.api.vo.CmsContentCategoryVO;
import io.mango.cms.api.vo.CmsContentTagVO;
import io.mango.cms.api.vo.CmsContentVO;
import io.mango.cms.api.vo.CmsNavigationVO;
import io.mango.cms.api.vo.CmsSiteSettingVO;
import io.mango.cms.api.vo.CmsSiteVO;
import io.mango.common.vo.PageResult;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * CMS 管理端 Grant 接入的主链路测试：八个数据域都能「列表签发 → 详情校验」。
 */
@SpringBootTest(classes = CmsDataGrantTestApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "mango.data-grant.mode=optional",
        "mango.data-grant.secret=" + CmsDataGrantHttpTestSupport.SECRET
})
class CmsDataGrantOptionalModeIntegrationTest extends CmsDataGrantHttpTestSupport {

    @Test
    void siteList_issuesGrantAcceptedBySiteDetail() throws Exception {
        CmsSiteVO first = site(SITE_ID);
        CmsSiteVO second = site(SECOND_RECORD_ID);
        when(siteAdminService.pageSites(any())).thenReturn(PageResult.of(List.of(first, second), 2, 1, 10));
        when(siteAdminService.detailSite(SITE_ID)).thenReturn(first);

        String grant = pageGrant("/cms/sites/page", 2);

        detailIsAllowed("/cms/sites/detail", "id", SITE_ID, SITE_ID, grant);
        detailIsAllowedWithoutGrant("/cms/sites/detail", "id", SITE_ID, SITE_ID);
    }

    @Test
    void siteList_issuesGrantAcceptedBySiteSettingDetail() throws Exception {
        CmsSiteVO first = site(SITE_ID);
        when(siteAdminService.pageSites(any())).thenReturn(PageResult.of(List.of(first), 1, 1, 10));
        CmsSiteSettingVO setting = new CmsSiteSettingVO();
        setting.setId(1L);
        setting.setSiteId(SITE_ID);
        when(siteSettingService.detailSiteSetting(SITE_ID)).thenReturn(setting);

        String grant = pageGrant("/cms/sites/page", 1);

        detailIsAllowed("/cms/site-settings/detail", "siteId", SITE_ID, 1L, grant);
        detailIsAllowedWithoutGrant("/cms/site-settings/detail", "siteId", SITE_ID, 1L);
    }

    @Test
    void contentList_issuesGrantAcceptedByContentDetail() throws Exception {
        CmsContentVO first = content(RECORD_ID);
        CmsContentVO second = content(SECOND_RECORD_ID);
        when(contentService.pageContents(any())).thenReturn(PageResult.of(List.of(first, second), 2, 1, 10));
        when(contentService.detailContent(RECORD_ID)).thenReturn(first);

        String grant = pageGrant("/cms/contents/page", 2);

        detailIsAllowed("/cms/contents/detail", "id", RECORD_ID, RECORD_ID, grant);
        detailIsAllowedWithoutGrant("/cms/contents/detail", "id", RECORD_ID, RECORD_ID);
    }

    @Test
    void contentCategoryList_issuesGrantAcceptedByCategoryDetail() throws Exception {
        CmsContentCategoryVO first = new CmsContentCategoryVO();
        first.setId(RECORD_ID);
        CmsContentCategoryVO second = new CmsContentCategoryVO();
        second.setId(SECOND_RECORD_ID);
        when(contentCategoryService.pageContentCategories(any()))
                .thenReturn(PageResult.of(List.of(first, second), 2, 1, 10));
        when(contentCategoryService.detailContentCategory(RECORD_ID)).thenReturn(first);

        String grant = pageGrant("/cms/content-categories/page", 2);

        detailIsAllowed("/cms/content-categories/detail", "id", RECORD_ID, RECORD_ID, grant);
        detailIsAllowedWithoutGrant("/cms/content-categories/detail", "id", RECORD_ID, RECORD_ID);
    }

    @Test
    void contentTagList_issuesGrantAcceptedByTagDetail() throws Exception {
        CmsContentTagVO first = new CmsContentTagVO();
        first.setId(RECORD_ID);
        CmsContentTagVO second = new CmsContentTagVO();
        second.setId(SECOND_RECORD_ID);
        when(contentTagService.pageContentTags(any())).thenReturn(PageResult.of(List.of(first, second), 2, 1, 10));
        when(contentTagService.detailContentTag(RECORD_ID)).thenReturn(first);

        String grant = pageGrant("/cms/content-tags/page", 2);

        detailIsAllowed("/cms/content-tags/detail", "id", RECORD_ID, RECORD_ID, grant);
        detailIsAllowedWithoutGrant("/cms/content-tags/detail", "id", RECORD_ID, RECORD_ID);
    }

    @Test
    void advertisementList_issuesGrantAcceptedByAdvertisementDetail() throws Exception {
        CmsAdvertisementVO first = new CmsAdvertisementVO();
        first.setId(RECORD_ID);
        CmsAdvertisementVO second = new CmsAdvertisementVO();
        second.setId(SECOND_RECORD_ID);
        when(advertisementService.pageAdvertisements(any()))
                .thenReturn(PageResult.of(List.of(first, second), 2, 1, 10));
        when(advertisementService.detailAdvertisement(RECORD_ID)).thenReturn(first);

        String grant = pageGrant("/cms/advertisements/page", 2);

        detailIsAllowed("/cms/advertisements/detail", "id", RECORD_ID, RECORD_ID, grant);
        detailIsAllowedWithoutGrant("/cms/advertisements/detail", "id", RECORD_ID, RECORD_ID);
    }

    @Test
    void adDeliveryList_issuesGrantAcceptedByAdDeliveryDetail() throws Exception {
        CmsAdDeliveryVO first = new CmsAdDeliveryVO();
        first.setId(RECORD_ID);
        CmsAdDeliveryVO second = new CmsAdDeliveryVO();
        second.setId(SECOND_RECORD_ID);
        when(adDeliveryService.pageAdDeliveries(any())).thenReturn(PageResult.of(List.of(first, second), 2, 1, 10));
        when(adDeliveryService.detailAdDelivery(RECORD_ID)).thenReturn(first);

        String grant = pageGrant("/cms/ad-deliveries/page", 2);

        detailIsAllowed("/cms/ad-deliveries/detail", "id", RECORD_ID, RECORD_ID, grant);
        detailIsAllowedWithoutGrant("/cms/ad-deliveries/detail", "id", RECORD_ID, RECORD_ID);
    }

    @Test
    void navigationList_issuesGrantAcceptedByNavigationDetail() throws Exception {
        CmsNavigationVO first = new CmsNavigationVO();
        first.setId(RECORD_ID);
        CmsNavigationVO second = new CmsNavigationVO();
        second.setId(SECOND_RECORD_ID);
        when(navigationService.pageNavigations(any())).thenReturn(PageResult.of(List.of(first, second), 2, 1, 10));
        when(navigationService.detailNavigation(RECORD_ID)).thenReturn(first);

        String grant = pageGrant("/cms/navigations/page", 2);

        detailIsAllowed("/cms/navigations/detail", "id", RECORD_ID, RECORD_ID, grant);
        detailIsAllowedWithoutGrant("/cms/navigations/detail", "id", RECORD_ID, RECORD_ID);
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
