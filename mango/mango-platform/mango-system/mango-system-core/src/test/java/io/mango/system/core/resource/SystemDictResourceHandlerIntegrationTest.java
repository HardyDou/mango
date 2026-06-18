package io.mango.system.core.resource;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import io.mango.resource.support.config.ResourceRegistryProperties;
import io.mango.resource.support.declaration.ResourceDeclarationCollector;
import io.mango.resource.support.declaration.ResourceDeclarationLoader;
import io.mango.resource.core.mapper.ResourceChangeLogMapper;
import io.mango.resource.core.mapper.ResourceRegistryMapper;
import io.mango.resource.core.mapper.ResourceSyncLogMapper;
import io.mango.resource.core.sync.ResourceContentHasher;
import io.mango.resource.core.sync.ResourceRegistryLock;
import io.mango.resource.core.sync.ResourceRegistryRepository;
import io.mango.resource.core.sync.ResourceRegistrySyncService;
import io.mango.resource.support.declaration.FileResourceProvider;
import io.mango.system.core.mapper.DictDataMapper;
import io.mango.system.core.mapper.DictTypeMapper;
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
        SystemDictResourceHandlerIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:system_dict_resource;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false",
        "mybatis-plus.mapper-locations=classpath:/mapper/resource/*.xml"
})
class SystemDictResourceHandlerIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ResourceRegistrySyncService syncService;

    @Autowired
    private DatabaseResourceProvider provider;

    @BeforeEach
    void setUp() {
        rebuildTables();
        provider.setDeclarations(List.of(dictDeclaration(1, "授权角色类型", List.of(
                item(100L, "系统角色", "1", 1, 1),
                item(101L, "业务角色", "2", 2, 1)
        ))));
    }

    @Test
    void syncCreatesSystemDictAndRegistryLogs() {
        syncService.sync();

        assertThat(stringValue("sys_dict_type", "dict_name", "dict_type = 'authorization_role_type'"))
                .isEqualTo("授权角色类型");
        assertThat(stringValue("sys_dict_type", "domain_code", "dict_type = 'authorization_role_type'"))
                .isEqualTo("AUTHORIZATION");
        assertThat(stringValue("sys_dict_data", "dict_label", "id = 100"))
                .isEqualTo("系统角色");
        assertThat(stringValue("resource_registry", "target_table", "resource_id = '2026061800100000001'"))
                .isEqualTo("sys_dict_type");
        assertThat(longValue("resource_registry", "target_id", "resource_id = '2026061800100000001'"))
                .isEqualTo(10L);
        assertThat(count("resource_sync_log")).isEqualTo(1);
        assertThat(count("resource_change_log")).isEqualTo(1);
    }

    @Test
    void syncUpdatesDictWhenVersionChangesAndDisablesRemovedItems() {
        syncService.sync();
        provider.setDeclarations(List.of(dictDeclaration(2, "授权角色类型新版", List.of(
                item(100L, "系统内置角色", "1", 1, 1)
        ))));

        syncService.sync();

        assertThat(stringValue("sys_dict_type", "dict_name", "id = 10")).isEqualTo("授权角色类型新版");
        assertThat(stringValue("sys_dict_data", "dict_label", "id = 100")).isEqualTo("系统内置角色");
        assertThat(intValue("sys_dict_data", "status", "id = 101")).isZero();
        assertThat(intValue("resource_registry", "resource_version", "resource_id = '2026061800100000001'"))
                .isEqualTo(2);
    }

    @Test
    void syncDisablesDictWhenDeclarationIsMissing() {
        syncService.sync();
        provider.setDeclarations(List.of());

        syncService.sync();

        assertThat(intValue("sys_dict_type", "status", "id = 10")).isZero();
        assertThat(intValue("sys_dict_data", "status", "id = 100")).isZero();
        assertThat(stringValue("resource_registry", "status", "resource_id = '2026061800100000001'"))
                .isEqualTo("REMOVED");
    }

    @Test
    void syncClasspathDictResources_withSystemAndOrgDeclarations_writesDictsThroughResourceRegistry() {
        ResourceRegistryProperties properties = new ResourceRegistryProperties();
        properties.setLocations(List.of(
                Path.of("../mango-system-starter/src/main/resources/META-INF/mango/resources/system-common-dict.yml")
                        .toUri().toString(),
                Path.of("../../mango-org/mango-org-starter/src/main/resources/META-INF/mango/resources/org-common-dict.yml")
                        .toUri().toString()
        ));
        ResourceDeclarationLoader loader = new ResourceDeclarationLoader(new ObjectMapper(), properties);
        FileResourceProvider fileProvider = new FileResourceProvider(loader);
        provider.setDeclarations(fileProvider.provide());

        syncService.sync();

        assertThat(count("resource_registry")).isEqualTo(13);
        assertThat(count("sys_dict_type")).isEqualTo(13);
        assertThat(count("sys_dict_data")).isEqualTo(43);
        assertThat(stringValue("sys_dict_type", "dict_name", "dict_type = 'sys_user_sex'"))
                .isEqualTo("用户性别");
        assertThat(stringValue("sys_dict_type", "domain_code", "dict_type = 'org_type'"))
                .isEqualTo("ORG");
        assertThat(stringValue("sys_dict_data", "dict_label", "id = 170"))
                .isEqualTo("公司");
        assertThat(stringValue("resource_registry", "module_code", "resource_id = '2026061800600000001'"))
                .isEqualTo("org");
    }

    private ResourceDeclaration dictDeclaration(int version, String dictName, List<Map<String, Object>> items) {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId("2026061800100000001");
        declaration.setVersion(version);
        declaration.setResourceType(ResourceTypes.SYSTEM_DICT);
        declaration.setModuleCode("authorization");
        declaration.setBizKey("authorization.dict.role-type");
        declaration.setName("授权角色类型字典");
        declaration.setTargetModule("system");
        declaration.setFields(new LinkedHashMap<>());
        field(declaration, "typeId", ResourceFieldType.LONG, 10L);
        field(declaration, "dictType", ResourceFieldType.STRING, "authorization_role_type");
        field(declaration, "dictName", ResourceFieldType.STRING, dictName);
        field(declaration, "domainCode", ResourceFieldType.STRING, "AUTHORIZATION");
        field(declaration, "remark", ResourceFieldType.STRING, "授权模块角色类型");
        field(declaration, "items", ResourceFieldType.LIST, items);
        return declaration;
    }

    private void field(ResourceDeclaration declaration, String name, ResourceFieldType type, Object value) {
        ResourceField field = new ResourceField();
        field.setType(type);
        field.setValue(value);
        declaration.getFields().put(name, field);
    }

    private Map<String, Object> item(Long id, String label, String value, int sort, int status) {
        return Map.of(
                "id", id,
                "label", label,
                "value", value,
                "sort", sort,
                "status", status
        );
    }

    private void rebuildTables() {
        jdbcTemplate.execute("drop table if exists resource_change_log");
        jdbcTemplate.execute("drop table if exists resource_sync_log");
        jdbcTemplate.execute("drop table if exists resource_registry");
        jdbcTemplate.execute("drop table if exists sys_dict_data");
        jdbcTemplate.execute("drop table if exists sys_dict_type");
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
                create table sys_dict_type (
                    id bigint primary key,
                    dict_type varchar(50) not null,
                    dict_name varchar(100) not null,
                    domain_code varchar(64) not null default 'COMMON',
                    status tinyint not null default 1,
                    remark varchar(500),
                    create_by varchar(64),
                    update_by varchar(64),
                    create_time datetime not null default current_timestamp,
                    update_time datetime not null default current_timestamp,
                    unique key uk_sys_dict_type (dict_type)
                )
                """);
        jdbcTemplate.execute("""
                create table sys_dict_data (
                    id bigint primary key,
                    dict_type varchar(50) not null,
                    dict_label varchar(100) not null,
                    dict_value varchar(100) not null,
                    sort int not null default 0,
                    status tinyint not null default 1,
                    remark varchar(500),
                    create_by varchar(64),
                    update_by varchar(64),
                    create_time datetime not null default current_timestamp,
                    update_time datetime not null default current_timestamp
                )
                """);
        jdbcTemplate.execute("""
                create table infra_kv_entry (
                    id          bigint not null,
                    kv_key      varchar(200) not null,
                    kv_value    text,
                    expire_time datetime not null,
                    create_time datetime not null default current_timestamp,
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
    @Import(SystemDictResourceHandler.class)
    @MapperScan(basePackageClasses = {
            ResourceRegistryMapper.class,
            DictTypeMapper.class,
            DictDataMapper.class
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
        ResourceDeclarationCollector resourceDeclarationCollector(ObjectProvider<ResourceProvider> providers,
                                                                  ResourceDeclarationLoader loader) {
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
            return List.of("authorization");
        }

        void setDeclarations(List<ResourceDeclaration> declarations) {
            this.declarations = declarations;
        }
    }
}
