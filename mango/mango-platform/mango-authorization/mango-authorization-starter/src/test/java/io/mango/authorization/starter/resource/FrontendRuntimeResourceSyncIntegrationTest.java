package io.mango.authorization.starter.resource;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.AuthorizationResourceTypes;
import io.mango.authorization.api.command.TenantAppBindingCommand;
import io.mango.authorization.api.vo.AppRuntimeDescriptorVO;
import io.mango.authorization.api.vo.ButtonDisplayRuleVO;
import io.mango.authorization.api.vo.TenantAppBindingVO;
import io.mango.authorization.core.config.FrontendRuntimeProperties;
import io.mango.authorization.core.mapper.AuthorizationAppLoginContextMapper;
import io.mango.authorization.core.mapper.AuthorizationAppMapper;
import io.mango.authorization.core.mapper.FrontendAppRegistryMapper;
import io.mango.authorization.core.mapper.FrontendModuleRuntimeStrategyMapper;
import io.mango.authorization.core.service.ISubjectAuthorityService;
import io.mango.authorization.core.service.ITenantAppBindingService;
import io.mango.authorization.core.service.impl.AuthorizationAppServiceImpl;
import io.mango.authorization.core.service.impl.FrontendRuntimeStrategyServiceImpl;
import io.mango.infra.kv.api.ILocker;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.resource.api.ResourceHandler;
import io.mango.resource.api.ResourceProvider;
import io.mango.resource.api.ResourceTargetDispatcher;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        FrontendRuntimeResourceSyncIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:frontend_runtime_resource;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false",
        "mybatis-plus.mapper-locations=classpath:/mapper/resource/*.xml",
        "mango.frontend.deploy-profile=hybrid"
})
class FrontendRuntimeResourceSyncIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ResourceRegistrySyncService syncService;

    @Autowired
    private AuthorizationAppServiceImpl appService;

    @BeforeEach
    void setUp() {
        rebuildTables();
        jdbcTemplate.update("""
                insert into authorization_app (
                    id, app_code, app_name, icon, sort, status, remark, create_time, update_time
                ) values (
                    100, 'internal-admin', 'Internal Admin', null, 1, 1, null, current_timestamp, current_timestamp
                )
                """);
    }

    @Test
    void resourceSyncWritesFrontendRuntimeTablesAndFeedsRuntimeDescriptor() {
        ResourceDeclaration runtimeUnit = frontendAppDeclaration();
        ResourceDeclaration strategy = runtimeStrategyDeclaration();

        syncService.syncRemote("platform-admin", "frontend-manifest", List.of(runtimeUnit, strategy));

        assertThat(stringValue("authorization_frontend_app_registry", "app_code")).isEqualTo("guarantee-remote");
        assertThat(stringValue("authorization_frontend_app_registry", "app_type")).isEqualTo("MICRO_APP");
        assertThat(stringValue("authorization_frontend_module_runtime_strategy", "runtime_code"))
                .isEqualTo("guarantee-remote");
        assertThat(count("resource_registry")).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("""
                select target_table from resource_registry
                where resource_type = 'FRONTEND_APP_REGISTRY'
                """, String.class)).isEqualTo("authorization_frontend_app_registry");

        AppRuntimeDescriptorVO descriptor = appService.runtimeDescriptor(
                new AuthorizationQuery(10L, AuthorizationQuery.SUBJECT_TYPE_TENANT_MEMBER, "1", null),
                "internal-admin");

        assertThat(descriptor.getDeployProfile()).isEqualTo("hybrid");
        assertThat(descriptor.getApps())
                .extracting("appCode")
                .containsExactly("internal-admin", "guarantee-remote");
        assertThat(descriptor.getApps())
                .filteredOn(app -> "guarantee-remote".equals(app.getAppCode()))
                .singleElement()
                .satisfies(app -> {
                    assertThat(app.getAppType()).isEqualTo("MICRO_APP");
                    assertThat(app.getDeployMode()).isEqualTo("REMOTE");
                    assertThat(app.getEntryUrl()).isEqualTo("https://cdn.example.com/guarantee/entry.js");
                    assertThat(app.getMountPath()).isEqualTo("/guarantee");
                });
        assertThat(descriptor.getModuleStrategies())
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.getAppCode()).isEqualTo("internal-admin");
                    assertThat(item.getModuleCode()).isEqualTo("mango-guarantee");
                    assertThat(item.getDeployProfile()).isEqualTo("hybrid");
                    assertThat(item.getPageType()).isEqualTo("MICRO_ROUTE");
                    assertThat(item.getRuntimeCode()).isEqualTo("guarantee-remote");
                });
    }

    private ResourceDeclaration frontendAppDeclaration() {
        ResourceDeclaration declaration = baseDeclaration(
                "2000000000000000001",
                AuthorizationResourceTypes.FRONTEND_APP_REGISTRY,
                "frontend.guarantee.remote",
                "Guarantee remote runtime");
        declaration.setTargetModule("authorization");
        declaration.getFields().put("appCode", field("guarantee-remote"));
        declaration.getFields().put("appType", field("MICRO_APP"));
        declaration.getFields().put("deployMode", field("REMOTE"));
        declaration.getFields().put("entryUrl", field("https://cdn.example.com/guarantee/entry.js"));
        declaration.getFields().put("mountPath", field("/guarantee"));
        declaration.getFields().put("activeRule", field("/guarantee/**"));
        declaration.getFields().put("framework", field("vue3"));
        declaration.getFields().put("version", field("1.0.0"));
        declaration.getFields().put("sandboxEnabled", field(Boolean.TRUE));
        declaration.getFields().put("styleIsolation", field("SCOPED"));
        return declaration;
    }

    private ResourceDeclaration runtimeStrategyDeclaration() {
        ResourceDeclaration declaration = baseDeclaration(
                "2000000000000000002",
                AuthorizationResourceTypes.FRONTEND_MODULE_RUNTIME_STRATEGY,
                "frontend.guarantee.strategy",
                "Guarantee runtime strategy");
        declaration.setTargetModule("authorization");
        declaration.getFields().put("appCode", field("internal-admin"));
        declaration.getFields().put("moduleCode", field("mango-guarantee"));
        declaration.getFields().put("deployProfile", field("hybrid"));
        declaration.getFields().put("pageType", field("MICRO_ROUTE"));
        declaration.getFields().put("runtimeCode", field("guarantee-remote"));
        declaration.getFields().put("status", field(1));
        declaration.getFields().put("sort", field(2));
        return declaration;
    }

    private ResourceDeclaration baseDeclaration(String id, String resourceType, String bizKey, String name) {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId(id);
        declaration.setVersion(1);
        declaration.setResourceType(resourceType);
        declaration.setModuleCode("mango-guarantee");
        declaration.setBizKey(bizKey);
        declaration.setName(name);
        declaration.setFields(new LinkedHashMap<>());
        return declaration;
    }

    private ResourceField field(Object value) {
        ResourceField field = new ResourceField();
        field.setType(value instanceof Boolean ? ResourceFieldType.BOOLEAN : ResourceFieldType.STRING);
        field.setValue(value);
        return field;
    }

    private void rebuildTables() {
        jdbcTemplate.execute("drop table if exists resource_change_log");
        jdbcTemplate.execute("drop table if exists resource_sync_log");
        jdbcTemplate.execute("drop table if exists resource_registry");
        jdbcTemplate.execute("drop table if exists authorization_frontend_module_runtime_strategy");
        jdbcTemplate.execute("drop table if exists authorization_frontend_app_registry");
        jdbcTemplate.execute("drop table if exists authorization_app_login_context");
        jdbcTemplate.execute("drop table if exists authorization_app");
        jdbcTemplate.execute("""
                create table authorization_app (
                    id bigint primary key,
                    app_code varchar(64) not null,
                    app_name varchar(128) not null,
                    icon varchar(255),
                    sort int not null,
                    status tinyint not null,
                    remark varchar(255),
                    create_time timestamp,
                    update_time timestamp
                )
                """);
        jdbcTemplate.execute("create unique index uk_authorization_app_code on authorization_app(app_code)");
        jdbcTemplate.execute("""
                create table authorization_app_login_context (
                    id bigint primary key,
                    app_id bigint not null,
                    app_code varchar(64) not null,
                    realm varchar(64) not null,
                    actor_type varchar(64) not null,
                    default_flag tinyint not null,
                    status tinyint not null,
                    sort int not null,
                    create_time timestamp,
                    update_time timestamp
                )
                """);
        jdbcTemplate.execute("""
                create table authorization_frontend_app_registry (
                    id bigint primary key,
                    app_code varchar(64) not null,
                    app_type varchar(32) not null,
                    deploy_mode varchar(32) not null,
                    entry_url varchar(512),
                    mount_path varchar(128),
                    active_rule varchar(255),
                    framework varchar(64),
                    version varchar(64),
                    health_check_url varchar(512),
                    sandbox_enabled boolean not null,
                    style_isolation varchar(32) not null,
                    create_time timestamp,
                    update_time timestamp
                )
                """);
        jdbcTemplate.execute("""
                create table authorization_frontend_module_runtime_strategy (
                    id bigint primary key,
                    app_code varchar(64) not null,
                    module_code varchar(128) not null,
                    deploy_profile varchar(32) not null,
                    page_type varchar(32) not null,
                    runtime_code varchar(64) not null,
                    status tinyint not null,
                    sort int not null,
                    create_time timestamp,
                    update_time timestamp
                )
                """);
        jdbcTemplate.execute("""
                create table resource_registry (
                    id bigint primary key,
                    resource_id varchar(64) not null,
                    resource_version int not null,
                    app_code varchar(128) not null default 'local',
                    service_code varchar(128) not null default 'local',
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
    }

    private long count(String tableName) {
        return jdbcTemplate.queryForObject("select count(*) from " + tableName, Long.class);
    }

    private String stringValue(String tableName, String columnName) {
        return jdbcTemplate.queryForObject("select " + columnName + " from " + tableName + " limit 1", String.class);
    }

    @Configuration
    @MapperScan(basePackageClasses = {
            AuthorizationAppMapper.class,
            AuthorizationAppLoginContextMapper.class,
            FrontendAppRegistryMapper.class,
            FrontendModuleRuntimeStrategyMapper.class,
            ResourceRegistryMapper.class,
            ResourceSyncLogMapper.class,
            ResourceChangeLogMapper.class
    })
    @Import({
            AuthorizationAppServiceImpl.class,
            FrontendRuntimeStrategyServiceImpl.class,
            FrontendAppRegistryResourceHandler.class,
            FrontendModuleRuntimeStrategyResourceHandler.class,
            ResourceRegistryRepository.class,
            ResourceRegistryLock.class,
            ResourceRegistrySyncService.class
    })
    static class TestConfig {

        @Bean
        FrontendRuntimeProperties frontendRuntimeProperties() {
            FrontendRuntimeProperties properties = new FrontendRuntimeProperties();
            properties.setDeployProfile("hybrid");
            return properties;
        }

        @Bean
        ResourceRegistryProperties resourceRegistryProperties() {
            ResourceRegistryProperties properties = new ResourceRegistryProperties();
            properties.setLocations(List.of());
            properties.setInstanceId("frontend-runtime-test");
            return properties;
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
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        ILocker locker() {
            return new InMemoryLocker();
        }

        @Bean
        ResourceTargetDispatcher resourceTargetDispatcher() {
            return new NoopResourceTargetDispatcher();
        }

        @Bean
        ITenantAppBindingService tenantAppBindingService() {
            return new AlwaysEnabledTenantAppBindingService();
        }

        @Bean
        ISubjectAuthorityService subjectAuthorityService() {
            return new AlwaysAuthorizedSubjectAuthorityService();
        }
    }

    static class InMemoryLocker implements ILocker {

        private final Map<String, Boolean> locks = new ConcurrentHashMap<>();

        @Override
        public boolean tryLock(String key, long ttlSeconds) {
            return locks.putIfAbsent(key, Boolean.TRUE) == null;
        }

        @Override
        public void unlock(String key) {
            locks.remove(key);
        }
    }

    static class NoopResourceTargetDispatcher implements ResourceTargetDispatcher {

        @Override
        public boolean supports(String targetModule) {
            return false;
        }

        @Override
        public Map<String, io.mango.resource.api.model.ResourceSyncResult> upsertBatch(
                List<ResourceDeclaration> declarations,
                List<ResourceDeclaration> completeBatch) {
            return Map.of();
        }

        @Override
        public io.mango.resource.api.model.ResourceSyncResult disable(ResourceDeclaration declaration) {
            return null;
        }

        @Override
        public io.mango.resource.api.model.ResourceSyncResult delete(ResourceDeclaration declaration) {
            return null;
        }
    }

    static class AlwaysEnabledTenantAppBindingService implements ITenantAppBindingService {

        @Override
        public List<TenantAppBindingVO> list(Long tenantId, String appCode, Integer status) {
            return List.of();
        }

        @Override
        public Long enable(TenantAppBindingCommand command) {
            return 1L;
        }

        @Override
        public Boolean disable(Long tenantId, String appCode) {
            return true;
        }

        @Override
        public void ensureEnabled(Long tenantId, String appCode) {
        }

        @Override
        public boolean isEnabled(Long tenantId, String appCode) {
            return true;
        }
    }

    static class AlwaysAuthorizedSubjectAuthorityService implements ISubjectAuthorityService {

        @Override
        public List<String> listSubjectRoles(AuthorizationQuery query) {
            return List.of("admin");
        }

        @Override
        public List<String> listSubjectPermissions(AuthorizationQuery query) {
            return List.of();
        }

        @Override
        public List<ButtonDisplayRuleVO> listSubjectButtonRules(AuthorizationQuery query) {
            return List.of();
        }
    }
}
