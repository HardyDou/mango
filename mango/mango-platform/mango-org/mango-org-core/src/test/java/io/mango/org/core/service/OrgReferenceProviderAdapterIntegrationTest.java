package io.mango.org.core.service;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.org.core.mapper.PostMapper;
import io.mango.org.core.mapper.SysOrgMapper;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        OrgReferenceProviderAdapterIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:org_reference_provider;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=true",
        "mango.persistence.mybatis-plus.tenant.default-tenant-id=1"
})
class OrgReferenceProviderAdapterIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OrgReferenceProviderAdapter provider;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("drop table if exists org_post");
        jdbcTemplate.execute("drop table if exists sys_org");
        jdbcTemplate.execute("create table sys_org (id bigint primary key, tenant_id bigint not null, org_code varchar(64))");
        jdbcTemplate.execute("create table org_post (id bigint primary key, tenant_id bigint not null, post_code varchar(64))");
        jdbcTemplate.update("insert into sys_org (id, tenant_id, org_code) values (?, ?, ?)", 201L, 2L, "COMPANY_A_ROOT");
        jdbcTemplate.update("insert into org_post (id, tenant_id, post_code) values (?, ?, ?)", 301L, 2L, "COMPANY_A_ADMIN");
    }

    @Test
    void resolvesExplicitTenantReferencesOutsideCurrentTenant() {
        assertThat(provider.resolveOrgId(2L, " COMPANY_A_ROOT ")).isEqualTo(201L);
        assertThat(provider.resolvePostId(2L, " COMPANY_A_ADMIN ")).isEqualTo(301L);
        assertThat(provider.resolveOrgId(1L, "COMPANY_A_ROOT")).isNull();
    }

    @Configuration
    @MapperScan(basePackageClasses = {SysOrgMapper.class, PostMapper.class})
    @Import(OrgReferenceProviderAdapter.class)
    static class TestConfig {
    }
}
