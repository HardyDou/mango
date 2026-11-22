package io.mango.cms.starter.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.mango.cms.core.service.ICmsAdDeliveryService;
import io.mango.cms.core.service.ICmsAdvertisementService;
import io.mango.cms.core.service.ICmsContentCategoryService;
import io.mango.cms.core.service.ICmsContentService;
import io.mango.cms.core.service.ICmsContentTagService;
import io.mango.cms.core.service.ICmsNavigationService;
import io.mango.cms.core.service.ICmsSiteAdminService;
import io.mango.cms.core.service.ICmsSiteSettingService;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * CMS 管理端 Grant 接入测试的公共装配与断言。
 *
 * <p>业务服务全部由测试替身提供：Grant 只有在签发与校验两端都落到同一租户、同一成员时才有效，
 * 因此每个用例都先绑定运行时上下文，再走「分页列表 → 响应头取 Grant → 带 Grant 取详情」的完整链路。</p>
 */
abstract class CmsDataGrantHttpTestSupport {

    /** 测试专用签名密钥，与 {@code mango.data-grant.secret} 保持一致。 */
    protected static final String SECRET = "cms-data-grant-977-test-secret";

    /** 测试租户。 */
    protected static final String TENANT_ID = "1";

    /** 测试成员 ID。 */
    protected static final long MEMBER_ID = 9770L;

    /** 首页记录的站点 ID。 */
    protected static final long SITE_ID = 10L;

    /** 首页记录的普通记录 ID。 */
    protected static final long RECORD_ID = 20L;

    /** 首页记录的第二个记录 ID。 */
    protected static final long SECOND_RECORD_ID = 21L;

    /** 列表响应头：本页记录可用的 Grant。 */
    protected static final String GRANT_HEADER = "X-Mango-Data-Grant";

    /** 强制模式缺少 Grant 时的标记响应头。 */
    protected static final String REQUIRED_HEADER = "X-Mango-Data-Grant-Required";

    /** Grant 未携带时的标记值。 */
    protected static final String GRANT_ABSENT = "absent";

    @Autowired
    protected MockMvc mockMvc;

    @MockitoBean
    protected ICmsSiteAdminService siteAdminService;

    @MockitoBean
    protected ICmsSiteSettingService siteSettingService;

    @MockitoBean
    protected ICmsContentService contentService;

    @MockitoBean
    protected ICmsContentCategoryService contentCategoryService;

    @MockitoBean
    protected ICmsContentTagService contentTagService;

    @MockitoBean
    protected ICmsAdvertisementService advertisementService;

    @MockitoBean
    protected ICmsAdDeliveryService adDeliveryService;

    @MockitoBean
    protected ICmsNavigationService navigationService;

    @BeforeEach
    void bindRuntimeContext() {
        MangoContextHolder.set(new MangoContextSnapshot(null, null, TENANT_ID, 1L, MEMBER_ID, "cms-tester",
                null, null, null, null, null, null));
    }

    @AfterEach
    void clearRuntimeContext() {
        MangoContextHolder.clear();
    }

    /**
     * 请求分页列表并取出响应头里的 Grant。
     *
     * @param path            列表端点路径
     * @param expectedRecords 期望返回的记录数
     * @return Grant 文本
     * @throws Exception 请求失败
     */
    protected String pageGrant(String path, int expectedRecords) throws Exception {
        MvcResult result = mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list.length()").value(expectedRecords))
                .andReturn();
        String grant = result.getResponse().getHeader(GRANT_HEADER);
        assertThat(grant)
                .as("%s 的响应头 %s", path, GRANT_HEADER)
                .isNotNull()
                .isNotEqualTo(GRANT_ABSENT);
        return grant;
    }

    /**
     * 带上首页签发的 Grant 请求详情，期望放行。
     *
     * @param path           详情端点路径
     * @param idParam        记录主键参数名
     * @param id             记录主键
     * @param expectedDataId 响应体里期望的主键（站点设置的记录主键是 siteId，响应主键另算）
     * @param grant          列表页签发的 Grant
     * @throws Exception 请求失败
     */
    protected void detailIsAllowed(String path, String idParam, long id, long expectedDataId, String grant)
            throws Exception {
        mockMvc.perform(get(path).param(idParam, Long.toString(id))
                        .header(GRANT_HEADER, grant))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(expectedDataId));
    }

    /**
     * 可选模式下不带 Grant 请求详情：放行，并在响应头标记 {@code absent}。
     *
     * @param path           详情端点路径
     * @param idParam        记录主键参数名
     * @param id             记录主键
     * @param expectedDataId 响应体里期望的主键
     * @throws Exception 请求失败
     */
    protected void detailIsAllowedWithoutGrant(String path, String idParam, long id, long expectedDataId)
            throws Exception {
        mockMvc.perform(get(path).param(idParam, Long.toString(id)))
                .andExpect(status().isOk())
                .andExpect(header().string(GRANT_HEADER, GRANT_ABSENT))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(expectedDataId));
    }
}
