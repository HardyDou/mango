package io.mango.authorization.starter.resource;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.authorization.core.entity.RoleDataScope;
import io.mango.authorization.core.mapper.RoleDataScopeMapper;
import io.mango.authorization.core.mapper.RoleMapper;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.org.core.mapper.SysOrgMapper;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.api.enums.ResourceStatus;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
import io.mango.resource.api.model.ResourceSyncResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        AuthRoleDataScopeResourceHandlerIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth_role_data_scope_resource_handler;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false"
})
class AuthRoleDataScopeResourceHandlerIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RoleDataScopeMapper scopeMapper;

    @Autowired
    private AuthRoleDataScopeResourceHandler handler;

    @BeforeEach
    void setUp() {
        resetSchema();
    }

    @Test
    void upsertStoresScopeValuesAsJsonArrayThroughRealMappers() {
        seedRole(1001L, 1L, "internal-admin", "ROLE_DEMO");

        ResourceSyncResult result = handler.upsert(resource());

        assertThat(handler.resourceType()).isEqualTo(ResourceTypes.AUTH_ROLE_DATA_SCOPE);
        assertThat(result.getTargetTable()).isEqualTo("authorization_role_data_scope");
        assertThat(result.getTargetId()).isNotNull();

        RoleDataScope scope = scopeMapper.selectById(result.getTargetId());
        assertThat(scope).isNotNull();
        assertThat(scope.getTenantId()).isEqualTo(1L);
        assertThat(scope.getAppCode()).isEqualTo("internal-admin");
        assertThat(scope.getRoleId()).isEqualTo(1001L);
        assertThat(scope.getResourceCode()).isEqualTo("order");
        assertThat(scope.getScopeMode()).isEqualTo("ORG");
        assertThat(scope.getScopeValues()).isEqualTo("[\"10\",\"20\"]");
        assertThat(scope.getIncludeChildren()).isTrue();
        assertThat(scope.getStatus()).isEqualTo(1);
        assertThat(countScopes()).isEqualTo(1L);
    }

    @Test
    void upsertUpdatesExistingScopeByRoleAndResourceThroughRealMappers() {
        seedRole(1001L, 1L, "internal-admin", "ROLE_DEMO");
        ResourceSyncResult created = handler.upsert(resource());
        ResourceDeclaration update = resource();
        put(update, "scopeValues", ResourceFieldType.JSON, List.of("30"));
        put(update, "includeChildren", ResourceFieldType.BOOLEAN, false);
        put(update, "status", ResourceFieldType.INT, 0);

        ResourceSyncResult updated = handler.upsert(update);

        assertThat(updated.getTargetId()).isEqualTo(created.getTargetId());
        RoleDataScope scope = scopeMapper.selectById(created.getTargetId());
        assertThat(scope.getScopeValues()).isEqualTo("[\"30\"]");
        assertThat(scope.getIncludeChildren()).isFalse();
        assertThat(scope.getStatus()).isZero();
        assertThat(countScopes()).isEqualTo(1L);
    }

    @Test
    void upsertRejectsMissingRoleReferenceThroughRealMapper() {
        assertThatThrownBy(() -> handler.upsert(resource()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("referenced role does not exist");
    }

    @Test
    void upsertResolvesOrgCodesToScopeValuesThroughRealMapper() {
        seedRole(1001L, 1L, "internal-admin", "ROLE_DEMO");
        seedOrg(10L, 1L, "HQ");
        seedOrg(20L, 1L, "BRANCH");
        ResourceDeclaration resource = resource();
        resource.getFields().remove("scopeValues");
        put(resource, "orgCodes", ResourceFieldType.JSON, List.of("HQ", "BRANCH"));

        ResourceSyncResult result = handler.upsert(resource);

        RoleDataScope scope = scopeMapper.selectById(result.getTargetId());
        assertThat(scope.getScopeValues()).isEqualTo("[\"10\",\"20\"]");
    }

    @Test
    void upsertRejectsMissingOrgCodeThroughRealMapper() {
        seedRole(1001L, 1L, "internal-admin", "ROLE_DEMO");
        ResourceDeclaration resource = resource();
        resource.getFields().remove("scopeValues");
        put(resource, "orgCodes", ResourceFieldType.JSON, List.of("UNKNOWN"));

        assertThatThrownBy(() -> handler.upsert(resource))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("referenced org does not exist");
    }

    @Test
    void disableMarksExistingScopeInactiveThroughRealMapper() {
        seedRole(1001L, 1L, "internal-admin", "ROLE_DEMO");
        ResourceSyncResult created = handler.upsert(resource());

        ResourceSyncResult disabled = handler.disable(resource());

        assertThat(disabled.getTargetId()).isEqualTo(created.getTargetId());
        RoleDataScope scope = scopeMapper.selectById(created.getTargetId());
        assertThat(scope.getStatus()).isZero();
        assertThat(countScopes()).isEqualTo(1L);
    }

    @Test
    void disabledResourceDeclarationCreatesInactiveScopeThroughRealMapper() {
        seedRole(1001L, 1L, "internal-admin", "ROLE_DEMO");
        ResourceDeclaration resource = resource();
        resource.setStatus(ResourceStatus.DISABLED);

        ResourceSyncResult result = handler.upsert(resource);

        RoleDataScope scope = scopeMapper.selectById(result.getTargetId());
        assertThat(scope.getStatus()).isZero();
    }

    private void resetSchema() {
        jdbcTemplate.execute("drop table if exists authorization_role_data_scope");
        jdbcTemplate.execute("drop table if exists authorization_role");
        jdbcTemplate.execute("drop table if exists sys_org");
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
                    remark varchar(500)
                )
                """);
        jdbcTemplate.execute("""
                create table authorization_role_data_scope (
                    id bigint primary key,
                    tenant_id bigint not null,
                    app_code varchar(64) not null default 'internal-admin',
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
                create table sys_org (
                    id bigint primary key,
                    org_name varchar(100) not null,
                    pid bigint,
                    org_code varchar(64) not null,
                    org_type int,
                    org_sort int,
                    org_status varchar(16),
                    tenant_id bigint not null
                )
                """);
    }

    private void seedRole(Long roleId, Long tenantId, String appCode, String roleCode) {
        jdbcTemplate.update("""
                        insert into authorization_role
                        (id, tenant_id, app_code, realm, actor_type, role_code, role_name)
                        values (?, ?, ?, 'INTERNAL', 'INTERNAL_USER', ?, ?)
                        """,
                roleId, tenantId, appCode, roleCode, roleCode);
    }

    private void seedOrg(Long orgId, Long tenantId, String orgCode) {
        jdbcTemplate.update("""
                        insert into sys_org
                        (id, tenant_id, org_name, org_code, org_type, org_sort, org_status)
                        values (?, ?, ?, ?, 3, 0, '1')
                        """,
                orgId, tenantId, orgCode, orgCode);
    }

    private ResourceDeclaration resource() {
        ResourceDeclaration resource = new ResourceDeclaration();
        resource.setResourceType(ResourceTypes.AUTH_ROLE_DATA_SCOPE);
        resource.setFields(new LinkedHashMap<>());
        put(resource, "tenantId", ResourceFieldType.LONG, 1L);
        put(resource, "appCode", ResourceFieldType.STRING, "internal-admin");
        put(resource, "roleCode", ResourceFieldType.STRING, "ROLE_DEMO");
        put(resource, "resourceCode", ResourceFieldType.STRING, "order");
        put(resource, "scopeMode", ResourceFieldType.STRING, "ORG");
        put(resource, "scopeValues", ResourceFieldType.JSON, List.of("10", "20"));
        put(resource, "includeChildren", ResourceFieldType.BOOLEAN, true);
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

    private Long countScopes() {
        return jdbcTemplate.queryForObject("select count(*) from authorization_role_data_scope", Long.class);
    }

    @Configuration
    @MapperScan(basePackageClasses = {RoleMapper.class, SysOrgMapper.class})
    @Import(AuthRoleDataScopeResourceHandler.class)
    static class TestConfig {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
