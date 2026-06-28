package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.command.SaveRoleDataScopeCommand;
import io.mango.authorization.api.enums.DataScopeMode;
import io.mango.authorization.api.vo.EffectiveDataScopeVO;
import io.mango.authorization.core.entity.RoleDataScope;
import io.mango.authorization.core.mapper.RoleDataScopeMapper;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        RoleDataScopeServiceImplIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:role_data_scope_service;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false"
})
@DisplayName("RoleDataScopeServiceImpl 集成测试")
class RoleDataScopeServiceImplIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RoleDataScopeMapper roleDataScopeMapper;

    @Autowired
    private RoleDataScopeServiceImpl service;

    @BeforeEach
    void setUp() {
        resetSchema();
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1L,
                1001L,
                "2",
                "admin",
                "INTERNAL",
                "INTERNAL_USER",
                "INTERNAL_ORG",
                2L,
                "internal-admin"));
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    @DisplayName("resolve should fallback to SELF when subject has no role through real mapper")
    void resolveNoRolesReturnsSelfThroughRealMapper() {
        EffectiveDataScopeVO result = service.resolve(query(), "payment:order:list");

        assertThat(result.getScopeMode()).isEqualTo(DataScopeMode.SELF);
        assertThat(result.getSelfIncluded()).isTrue();
    }

    @Test
    @DisplayName("save should persist only role granted list resource through real mappers")
    void saveGrantedQueryResourcePersistsThroughRealMappers() {
        seedRole(2L, 2L, "ROLE_ADMIN");
        seedMenu(3000L, 2L, "authorization:role:list,authorization:role:query");
        seedRoleMenu(4000L, 2L, 2L, 3000L);

        Boolean saved = service.save(saveCommand("authorization:role:list", DataScopeMode.ORG, List.of("100", "200", "100")));
        Boolean rejected = service.save(saveCommand("authorization:role:query", DataScopeMode.ALL, List.of()));

        assertThat(saved).isTrue();
        assertThat(rejected).isFalse();
        assertThat(countRoleDataScopes()).isEqualTo(1L);
        RoleDataScope persisted = selectOnlyRoleDataScope();
        assertThat(persisted.getTenantId()).isEqualTo(2L);
        assertThat(persisted.getAppCode()).isEqualTo("internal-admin");
        assertThat(persisted.getRoleId()).isEqualTo(2L);
        assertThat(persisted.getResourceCode()).isEqualTo("authorization:role:list");
        assertThat(persisted.getScopeMode()).isEqualTo("ORG");
        assertThat(persisted.getScopeValues()).isEqualTo("[\"100\",\"200\"]");
    }

    @Test
    @DisplayName("resolve should merge persisted role scopes through subject-role and data-scope mappers")
    void resolveMergesPersistedScopesThroughRealMappers() {
        seedSubjectRole(100L, 2L, 1001L, 10L);
        seedSubjectRole(101L, 2L, 1001L, 20L);
        seedRoleDataScope(200L, 2L, 10L, "payment:order:list", "ORG", "[\"100\",\"200\"]", false, 1);
        seedRoleDataScope(201L, 2L, 20L, "payment:order:list", "SELF", "[]", false, 1);

        EffectiveDataScopeVO result = service.resolve(query(), "payment:order:list");

        assertThat(result.getScopeMode()).isEqualTo(DataScopeMode.ORG);
        assertThat(result.getScopeValues()).containsExactly("100", "200");
        assertThat(result.getSelfIncluded()).isTrue();
    }

    private void resetSchema() {
        jdbcTemplate.execute("drop table if exists authorization_role_data_scope");
        jdbcTemplate.execute("drop table if exists authorization_subject_role");
        jdbcTemplate.execute("drop table if exists authorization_role_menu");
        jdbcTemplate.execute("drop table if exists authorization_menu");
        jdbcTemplate.execute("drop table if exists authorization_role");
        createRoleTable();
        createMenuTable();
        createRoleMenuTable();
        createSubjectRoleTable();
        createRoleDataScopeTable();
    }

    private void createRoleTable() {
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
    }

    private void createMenuTable() {
        jdbcTemplate.execute("""
                create table authorization_menu (
                    id bigint primary key,
                    tenant_id bigint not null default 1,
                    app_code varchar(64) not null default 'internal-admin',
                    module_code varchar(64),
                    parent_id bigint not null default 0,
                    menu_type tinyint not null default 2,
                    menu_name varchar(100) not null,
                    menu_code varchar(128),
                    path varchar(255),
                    icon varchar(64),
                    sort int not null default 0,
                    status tinyint not null default 1,
                    visible tinyint not null default 1,
                    component varchar(255),
                    keep_alive tinyint not null default 0,
                    embedded tinyint not null default 0,
                    redirect varchar(255),
                    permissions varchar(512),
                    button_type varchar(32),
                    button_display_rule varchar(512),
                    create_by varchar(64),
                    update_by varchar(64),
                    create_time timestamp default current_timestamp,
                    update_time timestamp default current_timestamp,
                    remark varchar(500),
                    del_flag tinyint not null default 0
                )
                """);
    }

    private void createRoleMenuTable() {
        jdbcTemplate.execute("""
                create table authorization_role_menu (
                    id bigint primary key,
                    tenant_id bigint not null default 1,
                    role_id bigint not null,
                    menu_id bigint not null
                )
                """);
    }

    private void createSubjectRoleTable() {
        jdbcTemplate.execute("""
                create table authorization_subject_role (
                    id bigint primary key,
                    tenant_id bigint not null default 1,
                    subject_id bigint not null,
                    subject_type varchar(32) not null default 'TENANT_MEMBER',
                    app_code varchar(64),
                    realm varchar(32),
                    actor_type varchar(32),
                    party_type varchar(64),
                    party_id bigint,
                    role_id bigint not null
                )
                """);
    }

    private void createRoleDataScopeTable() {
        jdbcTemplate.execute("""
                create table authorization_role_data_scope (
                    id bigint primary key,
                    tenant_id bigint not null default 1,
                    app_code varchar(64) not null default 'internal-admin',
                    role_id bigint not null,
                    resource_code varchar(128) not null,
                    scope_mode varchar(32) not null default 'SELF',
                    scope_values varchar(1000),
                    include_children boolean not null default false,
                    status tinyint not null default 1,
                    create_time timestamp not null default current_timestamp,
                    update_time timestamp not null default current_timestamp
                )
                """);
    }

    private AuthorizationQuery query() {
        return AuthorizationQuery.member(1001L)
                .withTenantId("2")
                .withSystemCode("internal-admin")
                .withRealm("INTERNAL")
                .withActorType("INTERNAL_USER")
                .withParty("INTERNAL_ORG", 2L);
    }

    private SaveRoleDataScopeCommand saveCommand(String resourceCode, DataScopeMode mode, List<String> values) {
        SaveRoleDataScopeCommand command = new SaveRoleDataScopeCommand();
        command.setRoleId(2L);
        command.setResourceCode(resourceCode);
        command.setScopeMode(mode);
        command.setScopeValues(values);
        command.setStatus(1);
        return command;
    }

    private void seedRole(Long roleId, Long tenantId, String roleCode) {
        jdbcTemplate.update("""
                        insert into authorization_role
                        (id, tenant_id, app_code, realm, actor_type, role_code, role_name)
                        values (?, ?, 'internal-admin', 'INTERNAL', 'INTERNAL_USER', ?, ?)
                        """,
                roleId, tenantId, roleCode, roleCode);
    }

    private void seedMenu(Long menuId, Long tenantId, String permissions) {
        jdbcTemplate.update("""
                        insert into authorization_menu
                        (id, tenant_id, app_code, menu_name, menu_code, permissions, status)
                        values (?, ?, 'internal-admin', 'Role List', 'authorization:role', ?, 1)
                        """,
                menuId, tenantId, permissions);
    }

    private void seedRoleMenu(Long id, Long tenantId, Long roleId, Long menuId) {
        jdbcTemplate.update("""
                        insert into authorization_role_menu (id, tenant_id, role_id, menu_id)
                        values (?, ?, ?, ?)
                        """,
                id, tenantId, roleId, menuId);
    }

    private void seedSubjectRole(Long id, Long tenantId, Long subjectId, Long roleId) {
        jdbcTemplate.update("""
                        insert into authorization_subject_role
                        (id, tenant_id, subject_id, subject_type, app_code, realm, actor_type, party_type, party_id, role_id)
                        values (?, ?, ?, 'TENANT_MEMBER', 'internal-admin', 'INTERNAL', 'INTERNAL_USER', 'INTERNAL_ORG', 2, ?)
                        """,
                id, tenantId, subjectId, roleId);
    }

    private void seedRoleDataScope(Long id,
                                   Long tenantId,
                                   Long roleId,
                                   String resourceCode,
                                   String mode,
                                   String values,
                                   Boolean includeChildren,
                                   Integer status) {
        jdbcTemplate.update("""
                        insert into authorization_role_data_scope
                        (id, tenant_id, app_code, role_id, resource_code, scope_mode, scope_values, include_children, status)
                        values (?, ?, 'internal-admin', ?, ?, ?, ?, ?, ?)
                        """,
                id, tenantId, roleId, resourceCode, mode, values, includeChildren, status);
    }

    private Long countRoleDataScopes() {
        return jdbcTemplate.queryForObject("select count(*) from authorization_role_data_scope", Long.class);
    }

    private RoleDataScope selectOnlyRoleDataScope() {
        List<RoleDataScope> scopes = roleDataScopeMapper.selectList(null);
        assertThat(scopes).hasSize(1);
        return scopes.get(0);
    }

    @Configuration
    @MapperScan(basePackageClasses = RoleDataScopeMapper.class)
    @Import(RoleDataScopeServiceImpl.class)
    static class TestConfig {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
