package io.mango.authorization.starter.resource;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.mango.authorization.core.entity.RoleEntity;
import io.mango.authorization.core.mapper.RoleMapper;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
import io.mango.resource.api.model.ResourceSyncResult;
import io.mango.system.api.tenant.TenantPackageBindingHandler;
import io.mango.system.api.tenant.TenantPackageBindingProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.LinkedHashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        AuthRoleResourceHandlerIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth_role_resource_handler;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=true",
        "mango.persistence.mybatis-plus.tenant.default-tenant-id=1"
})
class AuthRoleResourceHandlerIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private AuthRoleResourceHandler handler;

    @Autowired
    private RecordingTenantPackageBindingHandler packageBindingHandler;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("drop table if exists authorization_role");
        jdbcTemplate.execute("""
                create table authorization_role (
                    id bigint primary key,
                    tenant_id bigint not null default 1,
                    app_code varchar(64) not null default 'internal-admin',
                    realm varchar(32) not null default 'INTERNAL',
                    actor_type varchar(32),
                    role_code varchar(100) not null,
                    role_name varchar(50) not null,
                    role_type tinyint not null default 1,
                    status tinyint not null default 1,
                    sort int not null default 0,
                    create_time timestamp not null default current_timestamp,
                    update_time timestamp not null default current_timestamp,
                    remark varchar(500),
                    del_flag tinyint not null default 0,
                    created_by bigint,
                    created_at timestamp not null default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp not null default current_timestamp
                )
                """);
        jdbcTemplate.execute("""
                create unique index uk_authorization_role_tenant_app_role_code
                on authorization_role(tenant_id, app_code, role_code)
                """);
        AuthorizationStarterTestSchema.ensureCanonicalColumns(jdbcTemplate);
        packageBindingHandler.calls.clear();
    }

    @Test
    void upsertCreatesRoleFromDeclarationThroughRealMapper() {
        ResourceDeclaration resource = resource();

        ResourceSyncResult result = handler.upsert(resource);

        assertThat(handler.resourceType()).isEqualTo(ResourceTypes.AUTH_ROLE);
        assertThat(handler.dependsOnResourceTypes())
                .containsExactly(ResourceTypes.AUTH_APP, ResourceTypes.SYSTEM_TENANT);
        assertThat(result.getTargetTable()).isEqualTo("authorization_role");
        assertThat(result.getTargetId()).isNotNull();

        RoleEntity persisted = roleMapper.selectById(result.getTargetId());
        assertThat(persisted).isNotNull();
        assertThat(persisted.getTenantId()).isEqualTo("1");
        assertThat(persisted.getAppCode()).isEqualTo("internal-admin");
        assertThat(persisted.getRealm()).isEqualTo("INTERNAL");
        assertThat(persisted.getActorType()).isEqualTo("INTERNAL_USER");
        assertThat(persisted.getRoleCode()).isEqualTo("ROLE_DEMO");
        assertThat(persisted.getRoleName()).isEqualTo("Demo RoleEntity");
        assertThat(persisted.getRoleType()).isEqualTo(2);
        assertThat(persisted.getStatus()).isEqualTo(1);
    }

    @Test
    void upsertUpdatesExistingRoleByBusinessKeyThroughRealMapper() {
        ResourceSyncResult created = handler.upsert(resource());
        ResourceDeclaration update = resource();
        update.getFields().put("roleName", field(ResourceFieldType.STRING, "Updated Demo RoleEntity"));
        update.getFields().put("status", field(ResourceFieldType.INT, 0));
        update.getFields().put("remark", field(ResourceFieldType.STRING, "updated by resource sync"));

        ResourceSyncResult updated = handler.upsert(update);

        assertThat(updated.getTargetId()).isEqualTo(created.getTargetId());
        RoleEntity persisted = roleMapper.selectById(created.getTargetId());
        assertThat(persisted.getRoleName()).isEqualTo("Updated Demo RoleEntity");
        assertThat(persisted.getStatus()).isZero();
        assertThat(persisted.getRemark()).isEqualTo("updated by resource sync");
        assertThat(countRoles()).isEqualTo(1L);
    }

    @Test
    void disableUpdatesExistingRoleStatusThroughRealMapper() {
        ResourceSyncResult created = handler.upsert(resource());

        ResourceSyncResult disabled = handler.disable(resource());

        assertThat(disabled.getTargetId()).isEqualTo(created.getTargetId());
        RoleEntity persisted = roleMapper.selectById(created.getTargetId());
        assertThat(persisted.getStatus()).isZero();
        assertThat(countRoles()).isEqualTo(1L);
    }

    @Test
    void upsertUsesDeclarationTenantInsteadOfAmbientDefaultTenant() {
        ResourceDeclaration resource = resource();
        put(resource, "tenantId", ResourceFieldType.LONG, 2L);

        handler.upsert(resource);

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from authorization_role where tenant_id = 2 and role_code = 'ROLE_DEMO'",
                Long.class)).isEqualTo(1L);
    }

    @Test
    void upsertAdminRoleRefreshesPackageBindingCreatedBeforeRole() {
        ResourceDeclaration resource = resource();
        put(resource, "tenantId", ResourceFieldType.LONG, 2L);
        put(resource, "roleCode", ResourceFieldType.STRING, "ROLE_ADMIN");

        handler.upsert(resource);

        assertThat(packageBindingHandler.calls).containsExactly("2:22");
    }

    private ResourceDeclaration resource() {
        ResourceDeclaration resource = new ResourceDeclaration();
        resource.setResourceType(ResourceTypes.AUTH_ROLE);
        resource.setFields(new LinkedHashMap<>());
        put(resource, "tenantId", ResourceFieldType.LONG, 1L);
        put(resource, "roleCode", ResourceFieldType.STRING, "ROLE_DEMO");
        put(resource, "roleName", ResourceFieldType.STRING, "Demo RoleEntity");
        return resource;
    }

    private void put(ResourceDeclaration resource, String name, ResourceFieldType type, Object value) {
        resource.getFields().put(name, field(type, value));
    }

    private ResourceField field(ResourceFieldType type, Object value) {
        ResourceField field = new ResourceField();
        field.setType(type);
        field.setValue(value);
        return field;
    }

    private Long countRoles() {
        return jdbcTemplate.queryForObject("select count(*) from authorization_role", Long.class);
    }

    @Configuration
    @MapperScan(basePackageClasses = RoleMapper.class)
    @Import(AuthRoleResourceHandler.class)
    static class TestConfig {

        @Bean
        TenantPackageBindingProvider tenantPackageBindingProvider() {
            return new TenantPackageBindingProvider() {
                @Override
                public Long findPackageIdByTenantId(Long tenantId) {
                    return Long.valueOf(2L).equals(tenantId) ? 22L : null;
                }

                @Override
                public List<Long> listTenantIdsByPackage(Long packageId) {
                    return List.of();
                }
            };
        }

        @Bean
        RecordingTenantPackageBindingHandler recordingTenantPackageBindingHandler() {
            return new RecordingTenantPackageBindingHandler();
        }
    }

    static class RecordingTenantPackageBindingHandler implements TenantPackageBindingHandler {

        private final List<String> calls = new java.util.ArrayList<>();

        @Override
        public void bindPackage(Long tenantId, Long packageId) {
            calls.add(tenantId + ":" + packageId);
        }
    }
}
