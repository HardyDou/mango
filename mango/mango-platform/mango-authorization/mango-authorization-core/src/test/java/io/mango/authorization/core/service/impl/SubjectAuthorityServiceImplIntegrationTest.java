package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.core.mapper.SubjectRoleBindingMapper;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        SubjectAuthorityServiceImplIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:subject_authority_service;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false"
})
@DisplayName("SubjectAuthorityServiceImpl 集成测试")
class SubjectAuthorityServiceImplIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SubjectAuthorityServiceImpl service;

    @BeforeEach
    void setUp() {
        resetSchema();
    }

    @Test
    @DisplayName("listSubjectPermissions should use menu permissions through real mappers")
    void listSubjectPermissionsUsesMenuPermissionsThroughRealMappers() {
        seedSubjectRole(1L, 1L, 1001L, 10L);
        seedRoleMenu(1L, 1L, 10L, 100L);
        seedRoleMenu(2L, 1L, 10L, 101L);
        seedMenu(100L, 1L, "workflow:start-process:definition-list", "workflow:definition:list", 2, 1);
        seedMenu(101L, 1L, "workflow:start-process", "workflow:definition:list,workflow:process:start", 3, 1);

        List<String> permissions = service.listSubjectPermissions(query("1"));

        assertThat(permissions).containsExactly("workflow:definition:list", "workflow:process:start");
    }

    @Test
    @DisplayName("listSubjectPermissions should fallback to menu code for legacy button menus")
    void listSubjectPermissionsFallsBackToMenuCodeForLegacyButtons() {
        seedSubjectRole(1L, 1L, 1001L, 10L);
        seedRoleMenu(1L, 1L, 10L, 100L);
        seedMenu(100L, 1L, "system:user:view", null, 3, 1);

        List<String> permissions = service.listSubjectPermissions(query("1"));

        assertThat(permissions).containsExactly("system:user:view");
    }

    @Test
    @DisplayName("listSubjectPermissions should ignore directory menu permissions")
    void listSubjectPermissionsIgnoresDirectoryMenuPermissions() {
        seedSubjectRole(1L, 1L, 1001L, 10L);
        seedRoleMenu(1L, 1L, 10L, 100L);
        seedRoleMenu(2L, 1L, 10L, 101L);
        seedMenu(100L, 1L, "payment:directory", "payment:directory", 1, 1);
        seedMenu(101L, 1L, "payment:orders", "payment:order:list", 2, 1);

        List<String> permissions = service.listSubjectPermissions(query("1"));

        assertThat(permissions).containsExactly("payment:order:list");
    }

    @Test
    @DisplayName("listSubjectPermissions should return empty for invalid tenant id without mapper shortcuts")
    void listSubjectPermissionsInvalidTenantReturnsEmpty() {
        seedSubjectRole(1L, 1L, 1001L, 10L);
        seedRoleMenu(1L, 1L, 10L, 100L);
        seedMenu(100L, 1L, "system:user:view", null, 3, 1);

        List<String> permissions = service.listSubjectPermissions(query("not-a-number"));

        assertThat(permissions).isEmpty();
    }

    private void resetSchema() {
        jdbcTemplate.execute("drop table if exists authorization_subject_role");
        jdbcTemplate.execute("drop table if exists authorization_role_menu");
        jdbcTemplate.execute("drop table if exists authorization_menu");
        jdbcTemplate.execute("drop table if exists authorization_role");
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
        jdbcTemplate.execute("""
                create table authorization_role_menu (
                    id bigint primary key,
                    tenant_id bigint not null default 1,
                    role_id bigint not null,
                    menu_id bigint not null
                )
                """);
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

    private AuthorizationQuery query(String tenantId) {
        return AuthorizationQuery.member(1001L)
                .withTenantId(tenantId)
                .withSystemCode("internal-admin")
                .withRealm("INTERNAL")
                .withActorType("INTERNAL_USER")
                .withParty("INTERNAL_ORG", 1L);
    }

    private void seedSubjectRole(Long id, Long tenantId, Long subjectId, Long roleId) {
        jdbcTemplate.update("""
                        insert into authorization_subject_role
                        (id, tenant_id, subject_id, subject_type, app_code, realm, actor_type, party_type, party_id, role_id)
                        values (?, ?, ?, 'TENANT_MEMBER', 'internal-admin', 'INTERNAL', 'INTERNAL_USER', 'INTERNAL_ORG', 1, ?)
                        """,
                id, tenantId, subjectId, roleId);
    }

    private void seedRoleMenu(Long id, Long tenantId, Long roleId, Long menuId) {
        jdbcTemplate.update("""
                        insert into authorization_role_menu (id, tenant_id, role_id, menu_id)
                        values (?, ?, ?, ?)
                        """,
                id, tenantId, roleId, menuId);
    }

    private void seedMenu(Long id,
                          Long tenantId,
                          String menuCode,
                          String permissions,
                          Integer menuType,
                          Integer status) {
        jdbcTemplate.update("""
                        insert into authorization_menu
                        (id, tenant_id, app_code, menu_name, menu_code, permissions, menu_type, status)
                        values (?, ?, 'internal-admin', ?, ?, ?, ?, ?)
                        """,
                id, tenantId, menuCode, menuCode, permissions, menuType, status);
    }

    @Configuration
    @MapperScan(basePackageClasses = SubjectRoleBindingMapper.class)
    @Import(SubjectAuthorityServiceImpl.class)
    static class TestConfig {
    }
}
