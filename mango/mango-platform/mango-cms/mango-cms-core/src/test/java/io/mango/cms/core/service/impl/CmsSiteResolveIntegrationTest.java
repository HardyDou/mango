package io.mango.cms.core.service.impl;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.mango.cms.api.query.SiteResolveQuery;
import io.mango.cms.api.vo.SiteResolveVO;
import io.mango.cms.core.entity.CmsSiteEntity;
import io.mango.cms.core.mapper.CmsSiteMapper;
import io.mango.common.exception.BizException;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CMS 站点解析真实数据库集成测试。
 *
 * <p>用 H2 MySQL 模式和模块 V1 DDL 驱动真实 {@link CmsSiteMapper}，开启与生产一致的租户拦截器，证明匿名与带租户
 * 上下文两种解析都通过真实 SQL 按 {@code site_code} / {@code domain} 过滤，并保持启用状态、逻辑删除、默认租户范围和
 * 上下文租户隔离语义。
 */
@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        CmsSiteResolveIntegrationTest.TestConfiguration.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:cms_site_resolve;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.default-tenant-id=" + CmsSiteResolveIntegrationTest.DEFAULT_TENANT
})
@Sql(scripts = "classpath:db/migration/mango-cms/V1__init_mango_cms.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class CmsSiteResolveIntegrationTest {

    private static final String TENANT_A = "tenant-a";
    private static final String TENANT_B = "tenant-b";

    static final String DEFAULT_TENANT = TENANT_A;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CmsSiteMapper siteMapper;

    @Autowired
    private CmsSiteService siteService;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from cms_site");
        MangoContextHolder.clear();
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void resolveSite_anonymousSiteCodeQueriesSiteCodeColumnInRealDatabase() {
        siteMapper.insert(site(101L, TENANT_A, "main", "www.example.test", CmsSupport.ENABLED, 0));

        SiteResolveVO result = siteService.resolveSite(siteCodeQuery("main"));

        assertThat(result)
                .extracting(SiteResolveVO::getSiteId, SiteResolveVO::getSiteCode, SiteResolveVO::getSiteName)
                .containsExactly(101L, "main", "Main Site");
    }

    @Test
    void resolveSite_anonymousDomainStillResolvesTheSingleEnabledSite() {
        siteMapper.insert(site(102L, TENANT_A, "main", "www.example.test", CmsSupport.ENABLED, 0));

        SiteResolveVO result = siteService.resolveSite(domainQuery("www.example.test"));

        assertThat(result)
                .extracting(SiteResolveVO::getSiteId, SiteResolveVO::getSiteCode)
                .containsExactly(102L, "main");
    }

    @Test
    void resolveSite_domainTakesPrecedenceWhenBothSelectorsAreProvided() {
        siteMapper.insert(site(103L, TENANT_A, "main", "www.example.test", CmsSupport.ENABLED, 0));

        SiteResolveVO result = siteService.resolveSite(query("main", "www.example.test"));

        assertThat(result.getSiteId()).isEqualTo(103L);
        assertThatThrownBy(() -> siteService.resolveSite(query("main", "unknown.example.test")))
                .isInstanceOf(BizException.class)
                .hasMessage("站点不存在或不唯一");
    }

    @Test
    void resolveSite_anonymousSiteCodeIsScopedToTheConfiguredDefaultTenant() {
        siteMapper.insert(site(104L, TENANT_A, "main", "a.example.test", CmsSupport.ENABLED, 0));
        siteMapper.insert(site(105L, TENANT_B, "main", "b.example.test", CmsSupport.ENABLED, 0));

        SiteResolveVO result = siteService.resolveSite(siteCodeQuery("main"));

        assertThat(result)
                .extracting(SiteResolveVO::getSiteId, SiteResolveVO::getSiteCode)
                .containsExactly(104L, "main");
    }

    @Test
    void resolveSite_siteCodeIgnoresDisabledAndLogicallyDeletedSites() {
        siteMapper.insert(site(106L, TENANT_A, "closed", "closed.example.test", CmsSupport.DISABLED, 0));
        siteMapper.insert(site(107L, TENANT_A, "removed", "removed.example.test", CmsSupport.ENABLED, 1));

        assertThatThrownBy(() -> siteService.resolveSite(siteCodeQuery("closed")))
                .isInstanceOf(BizException.class)
                .hasMessage("站点不存在或不唯一");
        assertThatThrownBy(() -> siteService.resolveSite(siteCodeQuery("removed")))
                .isInstanceOf(BizException.class)
                .hasMessage("站点不存在或不唯一");
    }

    @Test
    void resolveSite_siteCodeCannotReachAnotherTenant() {
        siteMapper.insert(site(108L, TENANT_A, "main", "a.example.test", CmsSupport.ENABLED, 0));
        siteMapper.insert(site(109L, TENANT_B, "main", "b.example.test", CmsSupport.ENABLED, 0));
        siteMapper.insert(site(110L, TENANT_B, "only-b", "c.example.test", CmsSupport.ENABLED, 0));
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId(TENANT_B));

        SiteResolveVO result = siteService.resolveSite(siteCodeQuery("main"));
        SiteResolveVO tenantOnly = siteService.resolveSite(siteCodeQuery("only-b"));

        assertThat(result)
                .extracting(SiteResolveVO::getSiteId, SiteResolveVO::getSiteCode)
                .containsExactly(109L, "main");
        assertThat(tenantOnly.getSiteId()).isEqualTo(110L);
        assertThatThrownBy(() -> siteService.resolveSite(siteCodeQuery("missing-in-tenant")))
                .isInstanceOf(BizException.class)
                .hasMessage("站点不存在或不唯一");
    }

    private static SiteResolveQuery siteCodeQuery(String siteCode) {
        return query(siteCode, null);
    }

    private static SiteResolveQuery domainQuery(String domain) {
        return query(null, domain);
    }

    private static SiteResolveQuery query(String siteCode, String domain) {
        SiteResolveQuery query = new SiteResolveQuery();
        query.setSiteCode(siteCode);
        query.setDomain(domain);
        return query;
    }

    private static CmsSiteEntity site(Long id,
                                      String tenantId,
                                      String siteCode,
                                      String domain,
                                      String status,
                                      int deleted) {
        CmsSiteEntity entity = new CmsSiteEntity();
        entity.setId(id);
        entity.setTenantId(tenantId);
        entity.setOrgId(1L);
        entity.setSiteName("Main Site");
        entity.setSiteCode(siteCode);
        entity.setDomain(domain);
        entity.setStatus(status);
        entity.setDeleted(deleted);
        return entity;
    }

    @Configuration(proxyBeanMethods = false)
    @MapperScan("io.mango.cms.core.mapper")
    @Import(CmsSiteService.class)
    static class TestConfiguration {
    }
}
