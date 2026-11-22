package io.mango.authorization.data.grant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.mango.authorization.api.annotation.DataGrant;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.infra.persistence.api.scope.DataScopeProvider;
import io.mango.infra.persistence.api.scope.DataScopeRule;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Grant 端到端契约测试（MockMvc，不依赖数据库与真实服务）。
 *
 * <p>覆盖列表补签、命中放行、不命中 404、缺 Grant 的 optional / enforce 两种表现、超限与过期、数据范围变更。</p>
 */
class DataGrantHttpIntegrationTest {

    private DataGrantProperties properties;

    private DataGrantCodec codec;

    @BeforeEach
    void setUp() {
        properties = GrantTestSupport.properties();
        codec = GrantTestSupport.codec(properties);
        GrantTestSupport.context(GrantTestSupport.TENANT_ID, GrantTestSupport.MEMBER_ID);
    }

    @AfterEach
    void tearDown() {
        GrantTestSupport.clearContext();
    }

    @Test
    void listResponseCarriesGrant() throws Exception {
        MvcResult result = mockMvc(properties, null).perform(post("/fixture/items/page"))
                .andExpect(status().isOk())
                .andExpect(header().exists(DataGrantHeaders.GRANT))
                .andExpect(jsonPath("$.data.list").isArray())
                .andReturn();

        DataGrantPayload payload = codec.decode(result.getResponse().getHeader(DataGrantHeaders.GRANT));
        assertThat(payload.domain()).isEqualTo(GrantTestSupport.DOMAIN);
        assertThat(payload.ids()).containsExactly(1001L, 1002L);
        assertThat(result.getResponse().getHeader(DataGrantHeaders.SKIPPED)).isNull();
    }

    @Test
    void detailWithMatchingGrantReachesController() throws Exception {
        String grant = listGrant(properties, null);

        mockMvc(properties, null).perform(get("/fixture/items/detail").param("itemId", "1002")
                        .header(DataGrantHeaders.GRANT, grant))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1002));
    }

    @Test
    void detailOutsideGrantReturnsNotFound() throws Exception {
        String grant = listGrant(properties, null);

        mockMvc(properties, null).perform(get("/fixture/items/detail").param("itemId", "2002")
                        .header(DataGrantHeaders.GRANT, grant))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value(DataGrantHeaders.MESSAGE_RESOURCE_NOT_FOUND));
    }

    @Test
    void pathVariableDetailIsCheckedBeforeQuery() throws Exception {
        String grant = listGrant(properties, null);

        mockMvc(properties, null).perform(get("/fixture/items/1001/detail")
                        .header(DataGrantHeaders.GRANT, grant))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1001));

        mockMvc(properties, null).perform(get("/fixture/items/2002/detail")
                        .header(DataGrantHeaders.GRANT, grant))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(DataGrantHeaders.MESSAGE_RESOURCE_NOT_FOUND));
    }

    @Test
    void absentGrantInOptionalModeProceedsWithMarker() throws Exception {
        mockMvc(properties, null).perform(get("/fixture/items/detail").param("itemId", "2002"))
                .andExpect(status().isOk())
                .andExpect(header().string(DataGrantHeaders.GRANT, DataGrantHeaders.ABSENT))
                .andExpect(jsonPath("$.data.id").value(2002));
    }

    @Test
    void absentGrantInEnforceModeForbidden() throws Exception {
        DataGrantProperties enforce = GrantTestSupport.properties(DataGrantProperties.Mode.ENFORCE, 100);

        mockMvc(enforce, null).perform(get("/fixture/items/detail").param("itemId", "1001"))
                .andExpect(status().isForbidden())
                .andExpect(header().string(DataGrantHeaders.REQUIRED, "1"))
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value(DataGrantHeaders.MESSAGE_GRANT_REQUIRED));
    }

    @Test
    void oversizeListIsNotSignedButMarkedSkipped() throws Exception {
        DataGrantProperties small = GrantTestSupport.properties(DataGrantProperties.Mode.OPTIONAL, 1);

        mockMvc(small, null).perform(post("/fixture/items/page"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(DataGrantHeaders.GRANT))
                .andExpect(header().string(DataGrantHeaders.SKIPPED, DataGrantHeaders.SKIPPED_OVERSIZE));
    }

    @Test
    void expiredGrantForbidden() throws Exception {
        long now = Instant.now().getEpochSecond();
        String expired = GrantTestSupport.token(codec, GrantTestSupport.TENANT_ID, GrantTestSupport.MEMBER_ID,
                GrantTestSupport.DOMAIN, Set.of(1001L), now - 600, now - 300, null);

        mockMvc(properties, null).perform(get("/fixture/items/detail").param("itemId", "1001")
                        .header(DataGrantHeaders.GRANT, expired))
                .andExpect(status().isForbidden())
                .andExpect(header().string(DataGrantHeaders.REQUIRED, "1"))
                .andExpect(jsonPath("$.message").value(DataGrantHeaders.MESSAGE_GRANT_EXPIRED));
    }

    @Test
    void changedDataScopeForbidden() throws Exception {
        AtomicReference<Optional<DataScopeRule>> scope = new AtomicReference<>(
                Optional.of(GrantTestSupport.rule(Set.of("1", "2"), true)));
        String grant = listGrant(properties, providerOfScope(scope));

        scope.set(Optional.of(GrantTestSupport.rule(Set.of("1"), true)));

        mockMvc(properties, providerOfScope(scope)).perform(get("/fixture/items/detail").param("itemId", "1001")
                        .header(DataGrantHeaders.GRANT, grant))
                .andExpect(status().isForbidden())
                .andExpect(header().string(DataGrantHeaders.REQUIRED, "1"))
                .andExpect(jsonPath("$.message").value(DataGrantHeaders.MESSAGE_GRANT_SCOPE_CHANGED));
    }

    @Test
    void listResponseEscapesIntoExposedHeaderForCors() throws Exception {
        MvcResult result = mockMvc(properties, null).perform(post("/fixture/items/page")
                        .header(org.springframework.http.HttpHeaders.ORIGIN, "https://ui.example.com"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getHeader(DataGrantHeaders.GRANT)).isNotBlank();
    }

    private String listGrant(DataGrantProperties config,
            ObjectProvider<DataScopeProvider> provider) throws Exception {
        MvcResult result = mockMvc(config, provider).perform(post("/fixture/items/page"))
                .andExpect(status().isOk())
                .andReturn();
        String grant = result.getResponse().getHeader(DataGrantHeaders.GRANT);
        assertThat(grant).isNotBlank();
        return grant;
    }

    private static ObjectProvider<DataScopeProvider> providerOfScope(
            AtomicReference<Optional<DataScopeRule>> scope) {
        return GrantTestSupport.provider(resourceCode -> scope.get());
    }

    private MockMvc mockMvc(DataGrantProperties config, ObjectProvider<DataScopeProvider> provider) {
        ObjectProvider<DataScopeProvider> actual = provider == null ? GrantTestSupport.provider(null) : provider;
        DataGrantCodec configCodec = GrantTestSupport.codec(config);
        DataGrantIssuer issuer = new DataGrantIssuer(config, configCodec, actual);
        DataGrantVerifier verifier = new DataGrantVerifier(configCodec, actual);
        return MockMvcBuilders.standaloneSetup(new ItemController())
                .addInterceptors(new DataGrantInterceptor(config, verifier))
                .setControllerAdvice(new DataGrantResponseAdvice(config, issuer))
                .build();
    }

    /**
     * 被保护的试点控制器。
     */
    @RestController
    @RequestMapping("/fixture/items")
    static class ItemController {

        @PostMapping("/page")
        @DataGrant(domain = GrantTestSupport.DOMAIN)
        R<PageResult<ItemVO>> page() {
            List<ItemVO> records = List.of(new ItemVO(1001L), new ItemVO(1002L));
            return R.ok(PageResult.of(records, records.size(), 1, 10));
        }

        @GetMapping("/detail")
        @DataGrant(domain = GrantTestSupport.DOMAIN)
        R<ItemVO> detail(@RequestParam("itemId") Long itemId) {
            return R.ok(new ItemVO(itemId));
        }

        @GetMapping("/{id}/detail")
        @DataGrant(domain = GrantTestSupport.DOMAIN)
        R<ItemVO> pathDetail(@PathVariable("id") Long id) {
            return R.ok(new ItemVO(id));
        }
    }

    /**
     * 列表记录视图。
     */
    static class ItemVO {

        private final Long id;

        ItemVO(Long id) {
            this.id = id;
        }

        public Long getId() {
            return id;
        }
    }
}
