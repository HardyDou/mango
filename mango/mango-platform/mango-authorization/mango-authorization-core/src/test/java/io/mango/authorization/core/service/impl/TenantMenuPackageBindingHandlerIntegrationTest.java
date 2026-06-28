package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.mango.authorization.core.entity.RoleMenu;
import io.mango.authorization.core.mapper.MenuMapper;
import io.mango.authorization.core.mapper.RoleMapper;
import io.mango.authorization.core.mapper.RoleMenuMapper;
import io.mango.authorization.core.service.IMenuPackageService;
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
        TenantMenuPackageBindingHandlerIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:tenant_menu_package_binding;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false"
})
@DisplayName("TenantMenuPackageBindingHandler 集成测试")
class TenantMenuPackageBindingHandlerIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RoleMenuMapper roleMenuMapper;

    @Autowired
    private TenantMenuPackageBindingHandler handler;

    @BeforeEach
    void setUp() {
        resetSchema();
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("1"));
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    @DisplayName("bindPackage should rebuild tenant admin menu bindings through real mappers and restore context")
    void bindPackageRebuildsRoleMenusThroughRealMappersAndRestoresContext() {
        seedRole(20L, 2L, AuthorizationTenantProvisioner.TENANT_ADMIN_ROLE);
        seedMenu(100L, 0L);
        seedMenu(200L, 100L);
        seedRoleMenu(1L, 2L, 20L, 999L);

        handler.bindPackage(2L, 10L);

        assertThat(MangoContextHolder.tenantId()).isEqualTo("1");
        List<RoleMenu> roleMenus = roleMenuMapper.selectList(null);
        assertThat(roleMenus).extracting(RoleMenu::getTenantId).containsOnly(2L);
        assertThat(roleMenus).extracting(RoleMenu::getRoleId).containsOnly(20L);
        assertThat(roleMenus).extracting(RoleMenu::getMenuId).containsExactlyInAnyOrder(100L, 200L);
    }

    private void resetSchema() {
        jdbcTemplate.execute("drop table if exists authorization_role_menu");
        jdbcTemplate.execute("drop table if exists authorization_menu");
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
                    remark varchar(500)
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
                create table authorization_role_menu (
                    id bigint primary key,
                    tenant_id bigint not null default 1,
                    role_id bigint not null,
                    menu_id bigint not null
                )
                """);
    }

    private void seedRole(Long roleId, Long tenantId, String roleCode) {
        jdbcTemplate.update("""
                        insert into authorization_role
                        (id, tenant_id, app_code, realm, actor_type, role_code, role_name)
                        values (?, ?, 'internal-admin', 'INTERNAL', 'INTERNAL_USER', ?, ?)
                        """,
                roleId, tenantId, roleCode, roleCode);
    }

    private void seedMenu(Long menuId, Long parentId) {
        jdbcTemplate.update("""
                        insert into authorization_menu
                        (id, tenant_id, app_code, parent_id, menu_name, menu_code, status, del_flag)
                        values (?, 2, 'internal-admin', ?, ?, ?, 1, 0)
                        """,
                menuId, parentId, "TEST_MENU_" + menuId, "TEST_MENU_" + menuId);
    }

    private void seedRoleMenu(Long id, Long tenantId, Long roleId, Long menuId) {
        jdbcTemplate.update("""
                        insert into authorization_role_menu (id, tenant_id, role_id, menu_id)
                        values (?, ?, ?, ?)
                        """,
                id, tenantId, roleId, menuId);
    }

    @Configuration
    @MapperScan(basePackageClasses = RoleMapper.class)
    @Import(TenantMenuPackageBindingHandler.class)
    static class TestConfig {

        @Bean
        IMenuPackageService menuPackageService() {
            return new TestMenuPackageService();
        }
    }

    static class TestMenuPackageService implements IMenuPackageService {

        @Override
        public List<io.mango.authorization.api.vo.MenuPackageVO> listPackages(String appCode,
                                                                              String keyword,
                                                                              Integer status) {
            return List.of();
        }

        @Override
        public io.mango.authorization.api.vo.MenuPackageVO getById(Long packageId) {
            return null;
        }

        @Override
        public Long create(io.mango.authorization.api.command.MenuPackageCommand command) {
            return null;
        }

        @Override
        public boolean update(io.mango.authorization.api.command.MenuPackageCommand command) {
            return false;
        }

        @Override
        public boolean delete(Long packageId) {
            return false;
        }

        @Override
        public List<Long> listMenuIds(Long packageId) {
            return List.of(200L);
        }
    }
}
