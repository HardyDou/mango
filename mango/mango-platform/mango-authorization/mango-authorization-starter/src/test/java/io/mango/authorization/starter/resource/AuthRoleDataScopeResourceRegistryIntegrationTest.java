package io.mango.authorization.starter.resource;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.authorization.core.mapper.RoleMapper;
import io.mango.infra.bootstrap.api.BootstrapGenerationFence;
import io.mango.infra.kv.api.ILeaseLocker;
import io.mango.infra.kv.api.LockLease;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.resource.api.command.RegisterResourceDeclarationsCommand;
import io.mango.resource.api.command.ResourceModuleManifestCommand;
import io.mango.resource.api.enums.ResourceApplyMode;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.core.diagnostic.ResourceModuleSyncStatusRegistry;
import io.mango.resource.core.mapper.ResourceRegistryMapper;
import io.mango.resource.core.service.IResourceRegistryService;
import io.mango.resource.core.service.impl.ResourceRegistryService;
import io.mango.resource.core.sync.ResourceContentHasher;
import io.mango.resource.core.sync.ResourceModuleReceiptRepository;
import io.mango.resource.core.sync.ResourceRegistryLock;
import io.mango.resource.core.sync.ResourceRegistryRepository;
import io.mango.resource.support.ResourceProvider;
import io.mango.resource.support.ResourceTargetDispatcher;
import io.mango.resource.support.ResourceTypes;
import io.mango.resource.support.config.ResourceRegistryProperties;
import io.mango.resource.support.declaration.ResourceDeclarationCollector;
import io.mango.resource.support.declaration.ResourceDeclarationLoader;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceField;
import io.mango.resource.support.model.ResourceSyncResult;
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

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        AuthRoleDataScopeResourceRegistryIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth_role_data_scope_registry;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=true",
        "mango.persistence.mybatis-plus.tenant.default-tenant-id=1",
        "mybatis-plus.mapper-locations=classpath*:/mapper/**/*.xml"
})
class AuthRoleDataScopeResourceRegistryIntegrationTest {

    private static final String ENVIRONMENT = "issue-835-test";
    private static final String APP_CODE = "baohan";
    private static final String SERVICE_CODE = "baohan-api";
    private static final String MODULE_CODE = "guarantee";
    private static final String MANIFEST_FINGERPRINT = "8".repeat(64);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private IResourceRegistryService registryService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ResourceContentHasher contentHasher;

    @BeforeEach
    void setUp() {
        rebuildTables();
        AuthorizationStarterTestSchema.ensureCanonicalColumns(jdbcTemplate);
        jdbcTemplate.update("""
                        insert into authorization_role
                        (id, tenant_id, app_code, realm, actor_type, role_code, role_name)
                        values (?, ?, ?, 'INTERNAL', 'INTERNAL_USER', ?, ?)
                        """,
                2001L, 2L, "internal-admin", "ROLE_UPGRADE", "Upgrade role");
    }

    @Test
    void finalize_missingDeclaration_disablesTargetAndRemovesRegistry() throws Exception {
        ResourceDeclaration declaration = roleDataScopeDeclaration();

        assertThat(registryService.registerDeclarations(command(1L, List.of(declaration)))).isTrue();
        Long targetId = jdbcTemplate.queryForObject("""
                select target_id from resource_registry where resource_id = ?
                """, Long.class, declaration.getId());
        assertThat(targetId).isNotNull();
        assertThat(scopeStatus(targetId)).isEqualTo(1);
        assertThat(registryStatus(declaration.getId())).isEqualTo("ACTIVE");

        assertThat(registryService.registerDeclarations(command(2L, List.of()))).isTrue();

        assertThat(scopeStatus(targetId)).isZero();
        assertThat(registryStatus(declaration.getId())).isEqualTo("REMOVED");
        assertThat(jdbcTemplate.queryForObject("""
                select result from resource_sync_log
                where sync_type = 'DISABLE'
                order by created_at desc limit 1
                """, String.class)).isEqualTo("SUCCESS");
    }

    private RegisterResourceDeclarationsCommand command(long generation,
                                                        List<ResourceDeclaration> declarations)
            throws JsonProcessingException {
        ResourceModuleManifestCommand module = new ResourceModuleManifestCommand();
        module.setModuleCode(MODULE_CODE);
        module.setDependencies(List.of());
        module.setDeclarations(objectMapper.writeValueAsString(declarations));
        module.setDeclarationCount(declarations.size());
        module.setModuleHash(contentHasher.moduleHash(MODULE_CODE, List.of(), declarations));

        RegisterResourceDeclarationsCommand command = new RegisterResourceDeclarationsCommand();
        command.setAppCode(APP_CODE);
        command.setServiceCode(SERVICE_CODE);
        command.setEnvironmentKey(ENVIRONMENT);
        command.setGeneration(generation);
        command.setManifestFingerprint(MANIFEST_FINGERPRINT);
        command.setFencingToken(generation);
        command.setApplyMode(ResourceApplyMode.FINALIZE);
        command.setModuleManifests(List.of(module));
        return command;
    }

    private ResourceDeclaration roleDataScopeDeclaration() {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId("2083500000000000001");
        declaration.setVersion(1);
        declaration.setResourceType(ResourceTypes.AUTH_ROLE_DATA_SCOPE);
        declaration.setModuleCode(MODULE_CODE);
        declaration.setBizKey("guarantee.upgrade.data-scope");
        declaration.setName("Upgrade data scope");
        declaration.setTargetModule("authorization");
        declaration.setFields(new LinkedHashMap<>());
        put(declaration, "tenantId", ResourceFieldType.LONG, 2L);
        put(declaration, "appCode", ResourceFieldType.STRING, "internal-admin");
        put(declaration, "roleCode", ResourceFieldType.STRING, "ROLE_UPGRADE");
        put(declaration, "resourceCode", ResourceFieldType.STRING, "guarantee:upgrade:list");
        put(declaration, "scopeMode", ResourceFieldType.STRING, "ALL");
        return declaration;
    }

    private void put(ResourceDeclaration declaration, String name, ResourceFieldType type, Object value) {
        ResourceField field = new ResourceField();
        field.setType(type);
        field.setValue(value);
        declaration.putField(name, field);
    }

    private Integer scopeStatus(Long id) {
        return jdbcTemplate.queryForObject(
                "select status from authorization_role_data_scope where id = ?", Integer.class, id);
    }

    private String registryStatus(String resourceId) {
        return jdbcTemplate.queryForObject(
                "select status from resource_registry where resource_id = ?", String.class, resourceId);
    }

    private void rebuildTables() {
        jdbcTemplate.execute("drop table if exists resource_module_receipt");
        jdbcTemplate.execute("drop table if exists resource_change_log");
        jdbcTemplate.execute("drop table if exists resource_sync_log");
        jdbcTemplate.execute("drop table if exists resource_registry");
        jdbcTemplate.execute("drop table if exists authorization_role_data_scope");
        jdbcTemplate.execute("drop table if exists authorization_role");
        jdbcTemplate.execute("""
                create table authorization_role (
                    id bigint primary key,
                    tenant_id bigint not null,
                    app_code varchar(64) not null,
                    realm varchar(32) not null,
                    actor_type varchar(32),
                    role_code varchar(100) not null,
                    role_name varchar(50) not null,
                    role_type tinyint not null default 1,
                    status tinyint not null default 1,
                    sort int not null default 0,
                    create_time timestamp not null default current_timestamp,
                    update_time timestamp not null default current_timestamp,
                    remark varchar(500)
                )
                """);
        jdbcTemplate.execute("""
                create table authorization_role_data_scope (
                    id bigint primary key,
                    tenant_id bigint not null,
                    app_code varchar(64) not null,
                    role_id bigint not null,
                    resource_code varchar(128) not null,
                    scope_mode varchar(32) not null,
                    scope_values varchar(1000),
                    include_children boolean not null default false,
                    status tinyint not null default 1,
                    create_time timestamp not null default current_timestamp,
                    update_time timestamp not null default current_timestamp
                )
                """);
        jdbcTemplate.execute("""
                create table resource_registry (
                    id bigint primary key,
                    resource_id varchar(64) not null,
                    resource_version int not null,
                    app_code varchar(128) not null,
                    service_code varchar(128) not null,
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
                    tenant_id varchar(64),
                    org_id bigint,
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
                    tenant_id varchar(64),
                    org_id bigint,
                    created_by bigint,
                    created_at timestamp,
                    updated_by bigint,
                    updated_at timestamp
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
                    tenant_id varchar(64),
                    org_id bigint,
                    created_by bigint,
                    created_at timestamp,
                    updated_by bigint,
                    updated_at timestamp
                )
                """);
        jdbcTemplate.execute("""
                create table resource_module_receipt (
                    environment_key varchar(128) not null,
                    app_code varchar(128) not null,
                    service_code varchar(128) not null,
                    module_code varchar(64) not null,
                    module_hash varchar(64) not null,
                    generation bigint not null,
                    manifest_fingerprint varchar(64) not null,
                    state varchar(32) not null,
                    declaration_count int not null,
                    created_at timestamp not null default current_timestamp,
                    updated_at timestamp not null default current_timestamp,
                    primary key (environment_key, app_code, service_code, module_code)
                )
                """);
    }

    @Configuration
    @MapperScan(basePackageClasses = {
            RoleMapper.class,
            ResourceRegistryMapper.class
    })
    @Import({
            AuthRoleDataScopeResourceHandler.class,
            ResourceRegistryRepository.class,
            ResourceModuleReceiptRepository.class,
            ResourceRegistryLock.class,
            ResourceRegistryService.class
    })
    static class TestConfig {

        @Bean
        ResourceRegistryProperties resourceRegistryProperties() {
            ResourceRegistryProperties properties = new ResourceRegistryProperties();
            properties.setLocations(List.of());
            properties.setInstanceId("issue-835-test");
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
        ResourceModuleSyncStatusRegistry resourceModuleSyncStatusRegistry(ResourceContentHasher hasher) {
            return new ResourceModuleSyncStatusRegistry(hasher);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        ILeaseLocker leaseLocker() {
            return new InMemoryLeaseLocker();
        }

        @Bean
        BootstrapGenerationFence bootstrapGenerationFence() {
            return authority -> {
            };
        }

        @Bean
        ResourceTargetDispatcher resourceTargetDispatcher() {
            return new NoopResourceTargetDispatcher();
        }
    }

    static class InMemoryLeaseLocker implements ILeaseLocker {

        private final Map<String, String> leases = new ConcurrentHashMap<>();

        @Override
        public Optional<LockLease> tryAcquire(String key, String owner, long ttlSeconds) {
            String token = owner + ":" + UUID.randomUUID();
            if (leases.putIfAbsent(key, token) != null) {
                return Optional.empty();
            }
            Instant acquiredAt = Instant.now();
            return Optional.of(new LockLease(key, owner, token, acquiredAt, acquiredAt.plusSeconds(ttlSeconds)));
        }

        @Override
        public Optional<LockLease> renew(LockLease lease, long ttlSeconds) {
            if (!leases.getOrDefault(lease.key(), "").equals(lease.token())) {
                return Optional.empty();
            }
            return Optional.of(lease.renewedUntil(Instant.now().plusSeconds(ttlSeconds)));
        }

        @Override
        public boolean release(LockLease lease) {
            return leases.remove(lease.key(), lease.token());
        }
    }

    static class NoopResourceTargetDispatcher implements ResourceTargetDispatcher {

        @Override
        public boolean supports(String targetModule) {
            return false;
        }

        @Override
        public Map<String, ResourceSyncResult> upsertBatch(List<ResourceDeclaration> declarations,
                                                           List<ResourceDeclaration> completeBatch) {
            return Map.of();
        }

        @Override
        public ResourceSyncResult disable(ResourceDeclaration declaration) {
            return null;
        }

        @Override
        public ResourceSyncResult delete(ResourceDeclaration declaration) {
            return null;
        }
    }
}
