package io.mango.domain.core.resource;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.domain.core.mapper.DomainMapper;
import io.mango.infra.kv.api.ILocker;
import io.mango.infra.kv.core.capability.KvStoreLocker;
import io.mango.infra.kv.core.jdbc.JdbcKvStore;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.resource.api.ResourceHandler;
import io.mango.resource.api.ResourceProvider;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
import io.mango.resource.core.mapper.ResourceChangeLogMapper;
import io.mango.resource.core.mapper.ResourceRegistryMapper;
import io.mango.resource.core.mapper.ResourceSyncLogMapper;
import io.mango.resource.core.sync.ResourceContentHasher;
import io.mango.resource.core.sync.ResourceRegistryLock;
import io.mango.resource.core.sync.ResourceRegistryRepository;
import io.mango.resource.core.sync.ResourceRegistrySyncService;
import io.mango.resource.support.config.ResourceRegistryProperties;
import io.mango.resource.support.declaration.FileResourceProvider;
import io.mango.resource.support.declaration.ResourceDeclarationCollector;
import io.mango.resource.support.declaration.ResourceDeclarationLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;

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
        "mybatis-plus.mapper-locations=classpath:/mapper/resource/*.xml,classpath:/mapper/domain/*.xml"
})
class DomainResourceHandlerIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ResourceRegistrySyncService syncService;

    @Autowired
    private DatabaseResourceProvider provider;

    @BeforeEach
    void setUp() {
        rebuildTables();
        provider.setDeclarations(List.of(domainDeclaration(1, "工作流域", 1)));
    }

    @Test
    void syncCreatesBusinessDomainAndRegistryLogs() {
        syncService.sync();

        assertThat(stringValue("biz_domain", "domain_name", "id = 110")).isEqualTo("工作流域");
        assertThat(stringValue("biz_domain", "domain_code", "id = 110")).isEqualTo("WORKFLOW");
        assertThat(longValue("resource_registry", "target_id", "resource_id = '2026061800200000110'"))
                .isEqualTo(110L);
        assertThat(stringValue("resource_registry", "target_table", "resource_id = '2026061800200000110'"))
                .isEqualTo("biz_domain");
        assertThat(count("resource_sync_log")).isEqualTo(1);
        assertThat(count("resource_change_log")).isEqualTo(1);
    }

    @Test
    void syncUpdatesDomainWhenVersionChanges() {
        syncService.sync();
        provider.setDeclarations(List.of(domainDeclaration(2, "工作流审批域", 2)));

        syncService.sync();

        assertThat(stringValue("biz_domain", "domain_name", "id = 110")).isEqualTo("工作流审批域");
        assertThat(intValue("biz_domain", "sort", "id = 110")).isEqualTo(2);
        assertThat(intValue("resource_registry", "resource_version", "resource_id = '2026061800200000110'"))
                .isEqualTo(2);
    }

    @Test
    void syncDisablesDomainWhenDeclarationIsMissing() {
        syncService.sync();
        provider.setDeclarations(List.of());

        syncService.sync();

        assertThat(intValue("biz_domain", "status", "id = 110")).isZero();
        assertThat(stringValue("resource_registry", "status", "resource_id = '2026061800200000110'"))
                .isEqualTo("REMOVED");
    }

    @Test
    void forceSyncRebuildsDomainWhenTargetTableIsCleared() {
        syncService.sync();
        jdbcTemplate.update("delete from biz_domain");

        syncService.sync();

        assertThat(count("biz_domain")).isZero();

        syncService.sync(true);

        assertThat(stringValue("biz_domain", "domain_name", "id = 110")).isEqualTo("工作流域");
        assertThat(count("resource_registry")).isEqualTo(1);
    }

    @Test
    void deleteResourcePhysicallyDeletesDomainAndKeepsRegistryRemoved() {
        syncService.sync();

        syncService.deleteResource("2026061800200000110", true);

        assertThat(count("biz_domain")).isZero();
        assertThat(stringValue("resource_registry", "status", "resource_id = '2026061800200000110'"))
                .isEqualTo("REMOVED");
        assertThat(stringValue("resource_sync_log", "sync_type", "1 = 1 order by created_at desc limit 1"))
                .isEqualTo("DELETE");
        assertThat(stringValue("resource_change_log", "change_type", "1 = 1 order by created_at desc limit 1"))
                .isEqualTo("DELETE");
    }

    @Test
    void syncClasspathDomainResources_writesAllFlywayRebasedDomainsThroughResourceRegistry() {
        ResourceRegistryProperties properties = new ResourceRegistryProperties();
        properties.setLocations(List.of(
                Path.of("../mango-domain-starter/src/main/resources/META-INF/mango/resources/domain-common-domain.yml")
                        .toUri().toString(),
                Path.of("../../mango-workflow/mango-workflow-starter/src/main/resources/META-INF/mango/resources/workflow-common-domain.yml")
                        .toUri().toString(),
                Path.of("../../mango-notice/mango-notice-starter/src/main/resources/META-INF/mango/resources/notice-common-domain.yml")
                        .toUri().toString(),
                Path.of("../../mango-calendar/mango-calendar-starter/src/main/resources/META-INF/mango/resources/calendar-common-domain.yml")
                        .toUri().toString(),
                Path.of("../../mango-numgen/mango-numgen-starter/src/main/resources/META-INF/mango/resources/numgen-common-domain.yml")
                        .toUri().toString(),
                Path.of("../../mango-file/mango-file-starter/src/main/resources/META-INF/mango/resources/file-common-domain.yml")
                        .toUri().toString(),
                Path.of("../../mango-template/mango-template-starter/src/main/resources/META-INF/mango/resources/template-common-domain.yml")
                        .toUri().toString(),
                Path.of("../../mango-job/mango-job-starter/src/main/resources/META-INF/mango/resources/job-common-domain.yml")
                        .toUri().toString(),
                Path.of("../../mango-payment/mango-payment-starter/src/main/resources/META-INF/mango/resources/payment-common-domain.yml")
                        .toUri().toString()
        ));
        ResourceDeclarationLoader loader = new ResourceDeclarationLoader(new ObjectMapper(), properties);
        provider.setDeclarations(new FileResourceProvider(loader).provide());

        syncService.sync();

        assertThat(count("resource_registry")).isEqualTo(9);
        assertThat(count("biz_domain")).isEqualTo(9);
        assertThat(stringValue("biz_domain", "domain_name", "domain_code = 'COMMON'")).isEqualTo("通用域");
        assertThat(stringValue("biz_domain", "domain_name", "domain_code = 'PAYMENT'")).isEqualTo("支付域");
        assertThat(intValue("biz_domain", "sort", "domain_code = 'PAYMENT'")).isEqualTo(9);
    }

    private ResourceDeclaration domainDeclaration(int version, String domainName, int sort) {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId("2026061800200000110");
        declaration.setVersion(version);
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
        jdbcTemplate.execute("drop table if exists resource_change_log");
        jdbcTemplate.execute("drop table if exists resource_sync_log");
        jdbcTemplate.execute("drop table if exists resource_registry");
        jdbcTemplate.execute("drop table if exists biz_domain");
        jdbcTemplate.execute("drop table if exists infra_kv_entry");
        jdbcTemplate.execute("""
                create table resource_registry (
                    id bigint primary key,
                    resource_id varchar(64) not null,
                    resource_version int not null,
                    resource_type varchar(64) not null,
                    module_code varchar(64) not null,
                    biz_key varchar(128) not null,
                    name varchar(128),
                    target_module varchar(64) not null,
                    target_table varchar(128),
                    target_id bigint,
                    source_hash varchar(64),
                    sync_mode varchar(32) not null,
                    status varchar(32) not null,
                    last_sync_time timestamp,
                    created_by bigint,
                    created_at timestamp,
                    updated_by bigint,
                    updated_at timestamp
                )
                """);
        jdbcTemplate.execute("create unique index uk_resource_registry_resource_id on resource_registry(resource_id)");
        jdbcTemplate.execute("create unique index uk_resource_registry_type_biz_key on resource_registry(resource_type, biz_key)");
        jdbcTemplate.execute("""
                create table resource_sync_log (
                    id bigint primary key,
                    resource_id bigint,
                    sync_type varchar(32) not null,
                    result varchar(32) not null,
                    message clob,
                    created_at timestamp
                )
                """);
        jdbcTemplate.execute("""
                create table resource_change_log (
                    id bigint primary key,
                    resource_id bigint,
                    change_type varchar(32) not null,
                    operator_id bigint,
                    before_content clob,
                    after_content clob,
                    created_at timestamp
                )
                """);
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
        jdbcTemplate.execute("""
                create table infra_kv_entry (
                    id          bigint not null,
                    kv_key      varchar(200) not null,
                    kv_value    text,
                    expire_time timestamp not null,
                    create_time timestamp not null default current_timestamp,
                    primary key (id),
                    unique key uk_kv_key (kv_key)
                )
                """);
    }

    private long count(String tableName) {
        return jdbcTemplate.queryForObject("select count(*) from " + tableName, Long.class);
    }

    private String stringValue(String tableName, String columnName, String whereClause) {
        return jdbcTemplate.queryForObject("select " + columnName + " from " + tableName + " where " + whereClause,
                String.class);
    }

    private long longValue(String tableName, String columnName, String whereClause) {
        Long value = jdbcTemplate.queryForObject("select " + columnName + " from " + tableName + " where " + whereClause,
                Long.class);
        return value == null ? 0L : value;
    }

    private int intValue(String tableName, String columnName, String whereClause) {
        Integer value = jdbcTemplate.queryForObject("select " + columnName + " from " + tableName + " where " + whereClause,
                Integer.class);
        return value == null ? 0 : value;
    }

    @Configuration
    @Import(DomainResourceHandler.class)
    @MapperScan(basePackageClasses = {
            ResourceRegistryMapper.class,
            DomainMapper.class
    })
    static class TestConfig {

        @Bean
        ResourceRegistryProperties resourceRegistryProperties() {
            ResourceRegistryProperties properties = new ResourceRegistryProperties();
            properties.setLocations(List.of());
            return properties;
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        ResourceDeclarationLoader resourceDeclarationLoader(ObjectMapper objectMapper,
                                                            ResourceRegistryProperties properties) {
            return new ResourceDeclarationLoader(objectMapper, properties);
        }

        @Bean
        ResourceDeclarationCollector resourceDeclarationCollector(ObjectProvider<ResourceProvider> providers) {
            return new ResourceDeclarationCollector(providers);
        }

        @Bean
        ResourceContentHasher resourceContentHasher(ObjectMapper objectMapper) {
            return new ResourceContentHasher(objectMapper);
        }

        @Bean
        ResourceRegistryRepository resourceRegistryRepository(ResourceRegistryMapper registryMapper,
                                                             ResourceSyncLogMapper syncLogMapper,
                                                             ResourceChangeLogMapper changeLogMapper) {
            return new ResourceRegistryRepository(registryMapper, syncLogMapper, changeLogMapper);
        }

        @Bean
        ILocker locker(JdbcTemplate jdbcTemplate) {
            return new KvStoreLocker(new JdbcKvStore(jdbcTemplate));
        }

        @Bean
        ResourceRegistryLock resourceRegistryLock(ILocker locker) {
            return new ResourceRegistryLock(locker);
        }

        @Bean
        ResourceRegistrySyncService resourceRegistrySyncService(ResourceRegistryProperties properties,
                                                               ResourceDeclarationCollector collector,
                                                               ObjectProvider<ResourceHandler> handlers,
                                                               ResourceContentHasher hasher,
                                                               ResourceRegistryRepository repository,
                                                               ResourceRegistryLock lock,
                                                               ObjectMapper objectMapper) {
            return new ResourceRegistrySyncService(properties, collector, handlers, hasher, repository, lock, objectMapper);
        }

        @Bean
        DatabaseResourceProvider databaseResourceProvider() {
            return new DatabaseResourceProvider();
        }
    }

    static class DatabaseResourceProvider implements ResourceProvider {

        private List<ResourceDeclaration> declarations = List.of();

        @Override
        public List<ResourceDeclaration> provide() {
            return declarations;
        }

        @Override
        public List<String> moduleCodes() {
            return List.of("workflow");
        }

        void setDeclarations(List<ResourceDeclaration> declarations) {
            this.declarations = declarations;
        }
    }
}
