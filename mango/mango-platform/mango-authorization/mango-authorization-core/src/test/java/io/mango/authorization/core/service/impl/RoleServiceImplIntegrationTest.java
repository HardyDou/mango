package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.command.AssignSubjectRolesCommand;
import io.mango.authorization.api.command.RoleCommand;
import io.mango.authorization.api.vo.ButtonDisplayRuleVO;
import io.mango.authorization.api.vo.MenuVO;
import io.mango.authorization.core.entity.Menu;
import io.mango.authorization.core.entity.Role;
import io.mango.authorization.core.mapper.RoleMapper;
import io.mango.authorization.core.service.IMenuService;
import io.mango.authorization.core.service.ISubjectAuthorityService;
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
        RoleServiceImplIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:role_service;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false"
})
@DisplayName("RoleServiceImpl 集成测试")
class RoleServiceImplIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private RoleServiceImpl service;

    @BeforeEach
    void setUp() {
        resetSchema();
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1L,
                1001L,
                "1",
                "admin",
                "INTERNAL",
                "INTERNAL_USER",
                "INTERNAL_ORG",
                1L,
                "internal-admin"));
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    @DisplayName("create should persist role without manually setting tenant id")
    void createPersistsRoleWithoutManuallySettingTenantId() {
        Long roleId = service.create(roleCommand(null, "ROLE_TEST", "Test Role", 1));
        Role created = roleMapper.selectById(roleId);

        assertThat(roleId).isNotNull();
        assertThat(created.getRoleCode()).isEqualTo("ROLE_TEST");
        assertThat(created.getTenantId()).isNull();
    }

    @Test
    @DisplayName("update should persist current tenant role through real mapper")
    void updatePersistsCurrentTenantRoleThroughRealMapper() {
        seedRole(10L, 1L, "ROLE_TEST");

        RoleCommand update = roleCommand(10L, "ROLE_TEST_UPDATED", "Updated Role", 0);
        Boolean updated = service.update(update);
        Role persisted = roleMapper.selectById(10L);

        assertThat(updated).isTrue();
        assertThat(persisted.getRoleCode()).isEqualTo("ROLE_TEST_UPDATED");
        assertThat(persisted.getRoleName()).isEqualTo("Updated Role");
        assertThat(persisted.getStatus()).isZero();
    }

    @Test
    @DisplayName("delete should remove role and owned relationships through real mappers")
    void deleteRemovesRoleAndRelationshipsThroughRealMappers() {
        seedRole(10L, 1L, "ROLE_DELETE");
        seedRoleMenu(1L, 1L, 10L, 100L);
        seedSubjectRole(1L, 1L, 1001L, 10L);

        Boolean deleted = service.delete(10L);

        assertThat(deleted).isTrue();
        assertThat(roleMapper.selectById(10L)).isNull();
        assertThat(countRows("authorization_role_menu")).isZero();
        assertThat(countRows("authorization_subject_role")).isZero();
    }

    @Test
    @DisplayName("assignRoles should replace subject role bindings through real mapper")
    void assignRolesReplacesSubjectRoleBindingsThroughRealMapper() {
        seedRole(10L, 1L, "ROLE_OLD");
        seedRole(20L, 1L, "ROLE_NEW_A");
        seedRole(30L, 1L, "ROLE_NEW_B");
        seedSubjectRole(1L, 1L, 1001L, 10L);

        Boolean assigned = service.assignRoles(assignSubjectRolesCommand(1001L, List.of(20L, 30L)));

        assertThat(assigned).isTrue();
        assertThat(subjectRoleIds()).containsExactlyInAnyOrder(20L, 30L);
    }

    @Test
    @DisplayName("assignMenus should reject non assignable menu and persist assignable menu")
    void assignMenusRejectsEscalationAndPersistsAssignableMenu() {
        seedRole(10L, 1L, "ROLE_MENU");
        seedMenu(100L, 0L, "system:allowed");
        seedMenu(200L, 999L, "system:orphan");
        seedRoleMenu(1L, 1L, 10L, 999L);

        Boolean rejected = service.assignMenus(10L, List.of(200L));
        Boolean assigned = service.assignMenus(10L, List.of(100L));

        assertThat(rejected).isFalse();
        assertThat(assigned).isTrue();
        assertThat(roleMenuIds()).containsExactly(100L);
    }

    private void resetSchema() {
        jdbcTemplate.execute("drop table if exists authorization_subject_role");
        jdbcTemplate.execute("drop table if exists authorization_role_menu");
        jdbcTemplate.execute("drop table if exists authorization_menu");
        jdbcTemplate.execute("drop table if exists authorization_role");
        jdbcTemplate.execute("""
                create table authorization_role (
                    id bigint primary key,
                    tenant_id bigint,
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
                create table authorization_subject_role (
                    id bigint primary key,
                    tenant_id bigint,
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
                    tenant_id bigint,
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
    }

    private RoleCommand roleCommand(Long roleId, String roleCode, String roleName, Integer status) {
        RoleCommand command = new RoleCommand();
        command.setRoleId(roleId);
        command.setAppCode("internal-admin");
        command.setRealm("INTERNAL");
        command.setActorType("INTERNAL_USER");
        command.setRoleCode(roleCode);
        command.setRoleName(roleName);
        command.setRoleType(1);
        command.setStatus(status);
        command.setSort(1);
        return command;
    }

    private AssignSubjectRolesCommand assignSubjectRolesCommand(Long subjectId, List<Long> roleIds) {
        AssignSubjectRolesCommand command = new AssignSubjectRolesCommand();
        command.setSubjectId(subjectId);
        command.setAppCode("internal-admin");
        command.setRealm("INTERNAL");
        command.setActorType("INTERNAL_USER");
        command.setPartyType("INTERNAL_ORG");
        command.setPartyId(1L);
        command.setRoleIds(roleIds);
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

    private void seedMenu(Long menuId, Long parentId, String menuCode) {
        jdbcTemplate.update("""
                        insert into authorization_menu
                        (id, tenant_id, app_code, parent_id, menu_type, menu_name, menu_code, status, sort, del_flag)
                        values (?, 1, 'internal-admin', ?, 2, ?, ?, 1, 1, 0)
                        """,
                menuId, parentId, menuCode, menuCode);
    }

    private List<Long> subjectRoleIds() {
        return jdbcTemplate.queryForList(
                "select role_id from authorization_subject_role order by role_id",
                Long.class);
    }

    private List<Long> roleMenuIds() {
        return jdbcTemplate.queryForList(
                "select menu_id from authorization_role_menu order by menu_id",
                Long.class);
    }

    private Long countRows(String tableName) {
        return jdbcTemplate.queryForObject("select count(*) from " + tableName, Long.class);
    }

    @Configuration
    @MapperScan(basePackageClasses = RoleMapper.class)
    @Import(RoleServiceImpl.class)
    static class TestConfig {

        @Bean
        IMenuService menuService() {
            return new TreeOnlyMenuService();
        }

        @Bean
        ISubjectAuthorityService subjectAuthorityService() {
            return new AllPermissionSubjectAuthorityService();
        }
    }

    static class TreeOnlyMenuService implements IMenuService {

        @Override
        public List<MenuVO> listMenus(String appCode,
                                      String moduleCode,
                                      Integer type,
                                      Long parentId,
                                      String menuName,
                                      Integer status,
                                      boolean tree) {
            return List.of();
        }

        @Override
        public List<MenuVO> listUserMenus(String appCode,
                                          Integer type,
                                          Long parentId,
                                          AuthorizationQuery query,
                                          boolean tree) {
            return List.of();
        }

        @Override
        public Menu getById(Long menuId) {
            return null;
        }

        @Override
        public List<Menu> listByParentId(Long parentId) {
            return List.of();
        }

        @Override
        public List<MenuVO> buildMenuTree(List<Menu> menus) {
            return List.of();
        }

        @Override
        public boolean addMenu(Menu menu) {
            return false;
        }

        @Override
        public boolean updateMenu(Long menuId, Menu menu) {
            return false;
        }

        @Override
        public boolean deleteMenu(Long menuId) {
            return false;
        }
    }

    static class AllPermissionSubjectAuthorityService implements ISubjectAuthorityService {

        @Override
        public List<String> listSubjectRoles(AuthorizationQuery query) {
            return List.of();
        }

        @Override
        public List<String> listSubjectPermissions(AuthorizationQuery query) {
            return List.of("*:*");
        }

        @Override
        public List<ButtonDisplayRuleVO> listSubjectButtonRules(AuthorizationQuery query) {
            return List.of();
        }
    }
}
