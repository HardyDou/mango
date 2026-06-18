package io.mango.domain.core.resource;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.mango.domain.core.mapper.DomainMapper;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
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

import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        DomainResourceHandlerIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:domain_resource;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false",
        "mybatis-plus.mapper-locations=classpath:/mapper/domain/*.xml"
})
class DomainResourceHandlerIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DomainResourceHandler handler;

    @BeforeEach
    void setUp() {
        rebuildTables();
    }

    @Test
    void upsertCreatesBusinessDomain() {
        handler.upsert(domainDeclaration("工作流域", 1));

        assertThat(stringValue("biz_domain", "domain_name", "id = 110")).isEqualTo("工作流域");
        assertThat(stringValue("biz_domain", "domain_code", "id = 110")).isEqualTo("WORKFLOW");
        assertThat(stringValue("biz_domain", "domain_short_code", "id = 110")).isEqualTo("WF");
    }

    @Test
    void upsertUpdatesBusinessDomain() {
        handler.upsert(domainDeclaration("工作流域", 1));

        handler.upsert(domainDeclaration("工作流审批域", 2));

        assertThat(stringValue("biz_domain", "domain_name", "id = 110")).isEqualTo("工作流审批域");
        assertThat(intValue("biz_domain", "sort", "id = 110")).isEqualTo(2);
    }

    @Test
    void disableMarksDomainDisabled() {
        ResourceDeclaration declaration = domainDeclaration("工作流域", 1);
        handler.upsert(declaration);

        handler.disable(declaration);

        assertThat(intValue("biz_domain", "status", "id = 110")).isZero();
    }

    @Test
    void deletePhysicallyDeletesDomain() {
        ResourceDeclaration declaration = domainDeclaration("工作流域", 1);
        handler.upsert(declaration);

        handler.delete(declaration);

        assertThat(count("biz_domain")).isZero();
    }

    private ResourceDeclaration domainDeclaration(String domainName, int sort) {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId("2026061800200000110");
        declaration.setVersion(1);
        declaration.setResourceType(ResourceTypes.BUSINESS_DOMAIN);
        declaration.setModuleCode("workflow");
        declaration.setBizKey("workflow.domain.workflow");
        declaration.setName(domainName);
        declaration.setTargetModule("domain");
        declaration.setFields(new LinkedHashMap<>());
        field(declaration, "domainId", ResourceFieldType.LONG, 110L);
        field(declaration, "tenantId", ResourceFieldType.STRING, "1");
        field(declaration, "domainCode", ResourceFieldType.STRING, "WORKFLOW");
        field(declaration, "domainShortCode", ResourceFieldType.STRING, "WF");
        field(declaration, "domainName", ResourceFieldType.STRING, domainName);
        field(declaration, "parentId", ResourceFieldType.LONG, 0L);
        field(declaration, "sort", ResourceFieldType.INT, sort);
        field(declaration, "status", ResourceFieldType.INT, 1);
        field(declaration, "remark", ResourceFieldType.STRING, "工作流与审批业务域");
        return declaration;
    }

    private void field(ResourceDeclaration declaration, String name, ResourceFieldType type, Object value) {
        ResourceField field = new ResourceField();
        field.setType(type);
        field.setValue(value);
        declaration.getFields().put(name, field);
    }

    private void rebuildTables() {
        jdbcTemplate.execute("drop table if exists biz_domain");
        jdbcTemplate.execute("""
                create table biz_domain (
                    id bigint primary key,
                    tenant_id varchar(64) not null default '1',
                    org_id bigint,
                    domain_code varchar(64) not null,
                    domain_short_code varchar(64) not null,
                    domain_name varchar(128) not null,
                    parent_id bigint not null default 0,
                    sort int not null default 0,
                    status tinyint not null default 1,
                    remark varchar(512) not null default '',
                    create_time timestamp not null default current_timestamp,
                    update_time timestamp not null default current_timestamp,
                    created_by bigint,
                    created_at timestamp not null default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp not null default current_timestamp,
                    deleted tinyint not null default 0
                )
                """);
        jdbcTemplate.execute("create unique index uk_biz_domain_tenant_code on biz_domain(tenant_id, domain_code)");
        jdbcTemplate.execute("create unique index uk_biz_domain_tenant_short_code on biz_domain(tenant_id, domain_short_code)");
    }

    private long count(String tableName) {
        return jdbcTemplate.queryForObject("select count(*) from " + tableName, Long.class);
    }

    private String stringValue(String tableName, String columnName, String whereClause) {
        return jdbcTemplate.queryForObject("select " + columnName + " from " + tableName + " where " + whereClause,
                String.class);
    }

    private int intValue(String tableName, String columnName, String whereClause) {
        Integer value = jdbcTemplate.queryForObject("select " + columnName + " from " + tableName + " where " + whereClause,
                Integer.class);
        return value == null ? 0 : value;
    }

    @Configuration
    @Import(DomainResourceHandler.class)
    @MapperScan(basePackageClasses = DomainMapper.class)
    static class TestConfig {
    }
}
