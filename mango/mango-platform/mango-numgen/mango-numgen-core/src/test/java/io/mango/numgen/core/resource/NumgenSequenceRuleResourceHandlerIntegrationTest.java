package io.mango.numgen.core.resource;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.infra.kv.api.ILocker;
import io.mango.infra.kv.core.capability.KvStoreLocker;
import io.mango.infra.kv.core.jdbc.JdbcKvStore;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.numgen.core.mapper.NumgenGeneratorMapper;
import io.mango.numgen.core.mapper.NumgenRuleMapper;
import io.mango.numgen.core.mapper.NumgenRuleSegmentMapper;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        NumgenSequenceRuleResourceHandlerIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:numgen_resource;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false",
        "mybatis-plus.mapper-locations=classpath:/mapper/resource/*.xml,classpath:/mapper/numgen/*.xml"
})
class NumgenSequenceRuleResourceHandlerIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ResourceRegistrySyncService syncService;

    @Autowired
    private DatabaseResourceProvider provider;

    @BeforeEach
    void setUp() {
        rebuildTables();
        provider.setDeclarations(List.of(sequenceRuleDeclaration(1, "SO", "销售订单号", 1)));
    }

    @Test
    void syncCreatesGeneratorRuleSegmentsAndRegistryLogs() {
        syncService.sync();

        assertThat(stringValue("numgen_generator", "gen_name", "id = 100")).isEqualTo("销售订单号");
        assertThat(stringValue("numgen_generator", "domain_code", "id = 100")).isEqualTo("ORDER");
        assertThat(intValue("numgen_generator", "current_rule_version", "id = 100")).isEqualTo(1);
        assertThat(stringValue("numgen_rule", "version_state", "id = 110")).isEqualTo("ACTIVE");
        assertThat(intValue("numgen_rule_segment", "count(*)", "rule_id = 110")).isEqualTo(3);
        assertThat(stringValue("numgen_rule_segment", "literal_value", "id = 111")).isEqualTo("SO");
        assertThat(longValue("resource_registry", "target_id", "resource_id = '2026061800600000100'"))
                .isEqualTo(100L);
        assertThat(stringValue("resource_registry", "target_table", "resource_id = '2026061800600000100'"))
                .isEqualTo("numgen_generator");
    }

    @Test
    void syncUpdatesRuleAndReplacesSegmentsWhenVersionChanges() {
        syncService.sync();
        provider.setDeclarations(List.of(sequenceRuleDeclaration(2, "PO", "采购订单号", 1)));

        syncService.sync();

        assertThat(stringValue("numgen_generator", "gen_name", "id = 100")).isEqualTo("采购订单号");
        assertThat(intValue("numgen_generator", "current_rule_version", "id = 100")).isEqualTo(1);
        assertThat(stringValue("numgen_rule", "rule_name", "id = 110")).isEqualTo("采购订单号默认规则");
        assertThat(stringValue("numgen_rule_segment", "literal_value", "id = 111")).isEqualTo("PO");
        assertThat(intValue("numgen_rule_segment", "seq_width", "id = 113")).isEqualTo(6);
        assertThat(intValue("resource_registry", "resource_version", "resource_id = '2026061800600000100'"))
                .isEqualTo(2);
    }

    @Test
    void syncDisablesSequenceRuleWhenDeclarationIsMissing() {
        syncService.sync();
        provider.setDeclarations(List.of());

        syncService.sync();

        assertThat(intValue("numgen_generator", "status", "id = 100")).isZero();
        assertThat(intValue("numgen_rule", "status", "id = 110")).isZero();
        assertThat(stringValue("resource_registry", "status", "resource_id = '2026061800600000100'"))
                .isEqualTo("REMOVED");
    }

    @Test
    void deleteResourcePhysicallyDeletesGeneratorRuleAndSegments() {
        syncService.sync();

        syncService.deleteResource("2026061800600000100", true);

        assertThat(count("numgen_generator")).isZero();
        assertThat(count("numgen_rule")).isZero();
        assertThat(count("numgen_rule_segment")).isZero();
        assertThat(stringValue("resource_registry", "status", "resource_id = '2026061800600000100'"))
                .isEqualTo("REMOVED");
    }

    @Test
    void syncClasspathPaymentNumgenResourcesWritesAllOldFlywaySeedRows() {
        ResourceRegistryProperties properties = new ResourceRegistryProperties();
        properties.setLocations(List.of(
                Path.of("../../mango-payment/mango-payment-starter/src/main/resources/META-INF/mango/resources/payment-common-numgen.yml")
                        .toUri().toString()
        ));
        ResourceDeclarationLoader loader = new ResourceDeclarationLoader(new ObjectMapper(), properties);
        provider.setDeclarations(new FileResourceProvider(loader).provide());

        syncService.sync();

        assertThat(count("resource_registry")).isEqualTo(20);
        assertThat(count("numgen_generator")).isEqualTo(20);
        assertThat(count("numgen_rule")).isEqualTo(20);
        assertThat(count("numgen_rule_segment")).isEqualTo(60);
        assertThat(stringValue("numgen_generator", "gen_name", "gen_key = 'PAY_ORDER_NO'")).isEqualTo("支付订单号");
        assertThat(stringValue("numgen_rule_segment", "literal_value", "id = 900000020021")).isEqualTo("PO");
        assertThat(intValue("numgen_rule_segment", "sequence_scope", "id = 900000020022")).isEqualTo(1);
        assertThat(intValue("numgen_rule_segment", "seq_width", "id = 900000020023")).isEqualTo(8);
    }

    private ResourceDeclaration sequenceRuleDeclaration(int version, String prefix, String name, int ruleVersion) {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId("2026061800600000100");
        declaration.setVersion(version);
        declaration.setResourceType(ResourceTypes.SEQUENCE_RULE);
        declaration.setModuleCode("order");
        declaration.setBizKey("order.numgen.sales-order-no");
        declaration.setName(name);
        declaration.setTargetModule("numgen");
        declaration.setFields(new LinkedHashMap<>());
        field(declaration, "generatorId", ResourceFieldType.LONG, 100L);
        field(declaration, "ruleId", ResourceFieldType.LONG, 110L);
        field(declaration, "tenantId", ResourceFieldType.LONG, 1L);
        field(declaration, "genKey", ResourceFieldType.STRING, "ORDER_NO");
        field(declaration, "genName", ResourceFieldType.STRING, name);
        field(declaration, "domainCode", ResourceFieldType.STRING, "ORDER");
        field(declaration, "ruleName", ResourceFieldType.STRING, name + "默认规则");
        field(declaration, "ruleVersion", ResourceFieldType.INT, ruleVersion);
        field(declaration, "status", ResourceFieldType.INT, 1);
        field(declaration, "publishStatus", ResourceFieldType.INT, 1);
        field(declaration, "versionState", ResourceFieldType.STRING, "ACTIVE");
        field(declaration, "segments", ResourceFieldType.LIST, List.of(
                Map.of(
                        "id", 111L,
                        "sortOrder", 1,
                        "segmentType", "TEXT",
                        "segmentName", "业务前缀",
                        "literalValue", prefix,
                        "padChar", "0",
                        "sequenceScope", 0
                ),
                Map.of(
                        "id", 112L,
                        "sortOrder", 2,
                        "segmentType", "DATE",
                        "segmentName", "日期",
                        "dateFormat", "yyyyMMdd",
                        "padChar", "0",
                        "sequenceScope", 1
                ),
                Map.of(
                        "id", 113L,
                        "sortOrder", 3,
                        "segmentType", "SEQ",
                        "segmentName", "日内序号",
                        "seqWidth", version == 1 ? 4 : 6,
                        "padChar", "0",
                        "sequenceScope", 0
                )
        ));
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
        jdbcTemplate.execute("drop table if exists numgen_rule_segment");
        jdbcTemplate.execute("drop table if exists numgen_rule");
        jdbcTemplate.execute("drop table if exists numgen_generator");
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
                create table numgen_generator (
                    id bigint not null,
                    gen_key varchar(128) not null,
                    gen_name varchar(128) not null,
                    domain_code varchar(64) not null default 'NUMGEN',
                    status tinyint not null default 1,
                    current_rule_version int,
                    current_publish_status tinyint not null default 0,
                    tenant_id bigint not null default 0,
                    create_by varchar(64),
                    update_by varchar(64),
                    create_time timestamp not null default current_timestamp,
                    update_time timestamp not null default current_timestamp,
                    del_flag tinyint not null default 0,
                    primary key (id),
                    unique key uk_numgen_generator_tenant_key (tenant_id, gen_key, del_flag)
                )
                """);
        jdbcTemplate.execute("""
                create table numgen_rule (
                    id bigint not null,
                    gen_key varchar(128) not null,
                    rule_name varchar(128) not null,
                    version int not null default 1,
                    status tinyint not null default 1,
                    publish_status tinyint not null default 0,
                    version_state varchar(16) not null default 'DRAFT',
                    tenant_id bigint not null default 0,
                    create_by varchar(64),
                    update_by varchar(64),
                    create_time timestamp not null default current_timestamp,
                    update_time timestamp not null default current_timestamp,
                    del_flag tinyint not null default 0,
                    primary key (id),
                    unique key uk_numgen_rule_tenant_key_version (tenant_id, gen_key, version, del_flag)
                )
                """);
        jdbcTemplate.execute("""
                create table numgen_rule_segment (
                    id bigint not null,
                    rule_id bigint not null,
                    sort_order int not null,
                    segment_type varchar(32) not null,
                    segment_name varchar(128) not null,
                    literal_value varchar(128),
                    variable_key varchar(128),
                    date_format varchar(64),
                    seq_width int,
                    pad_char varchar(1) default '0',
                    sequence_scope tinyint not null default 0,
                    tenant_id bigint not null default 0,
                    create_by varchar(64),
                    update_by varchar(64),
                    create_time timestamp not null default current_timestamp,
                    update_time timestamp not null default current_timestamp,
                    primary key (id)
                )
                """);
        jdbcTemplate.execute("""
                create table infra_kv_entry (
                    id bigint not null,
                    kv_key varchar(200) not null,
                    kv_value text,
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
    @Import(NumgenSequenceRuleResourceHandler.class)
    @MapperScan(basePackageClasses = {
            ResourceRegistryMapper.class,
            NumgenGeneratorMapper.class,
            NumgenRuleMapper.class,
            NumgenRuleSegmentMapper.class
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
            return List.of("order", "payment");
        }

        void setDeclarations(List<ResourceDeclaration> declarations) {
            this.declarations = declarations;
        }
    }
}
