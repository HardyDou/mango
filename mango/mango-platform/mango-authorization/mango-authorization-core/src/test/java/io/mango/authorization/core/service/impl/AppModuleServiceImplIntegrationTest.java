package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.mango.authorization.api.command.AppModuleResourceManifestCommand;
import io.mango.authorization.core.entity.Menu;
import io.mango.authorization.core.mapper.AuthorizationAppModuleMapper;
import io.mango.authorization.core.mapper.MenuMapper;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.system.api.tenant.TenantPackageBindingProvider;
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

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        AppModuleServiceImplIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:app_module_service;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false"
})
@DisplayName("AppModuleServiceImpl 集成测试")
class AppModuleServiceImplIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MenuMapper menuMapper;

    @Autowired
    private AppModuleServiceImpl service;

    @Autowired
    private TestTenantMenuPackageBindingHandler bindingHandler;

    @BeforeEach
    void setUp() {
        resetSchema();
        bindingHandler.clear();
    }

    @Test
    @DisplayName("registerResourceManifest should upsert menu tree and permission buttons through real mappers")
    void registerResourceManifestUpsertsMenuTreeThroughRealMappers() {
        int registered = service.registerResourceManifest(createManifest());

        assertThat(registered).isEqualTo(3);
        assertThat(menuCodes()).containsExactly("contract", "contract:archive:list", "contract:archive:create");
        Menu directory = selectMenu("contract");
        Menu page = selectMenu("contract:archive:list");
        Menu button = selectMenu("contract:archive:create");
        assertThat(directory.getMenuType()).isEqualTo(1);
        assertThat(page.getParentId()).isEqualTo(directory.getMenuId());
        assertThat(page.getMenuType()).isEqualTo(2);
        assertThat(page.getPermissions()).isEqualTo("contract:archive:create,contract:archive:delete");
        assertThat(button.getParentId()).isEqualTo(page.getMenuId());
        assertThat(button.getMenuType()).isEqualTo(3);
        assertThat(button.getPermissions()).isEqualTo("contract:archive:create");
        assertThat(runtimeConfigTypes()).containsExactlyInAnyOrder("LOCAL_ROUTE", "LOCAL_ROUTE", "BUTTON");
    }

    @Test
    @DisplayName("registerResourceManifest should assign menus to packages and default roles through real mappers")
    void registerResourceManifestAssignsPackagesAndRolesThroughRealMappers() {
        seedPackage(1L, "internal-admin-default");
        seedRole(10L, 1L, "ROLE_ADMIN");
        AppModuleResourceManifestCommand manifest = createManifest();
        manifest.setPackageCodes(List.of("internal-admin-default"));
        manifest.setRoleCodes(List.of("ROLE_ADMIN"));

        int registered = service.registerResourceManifest(manifest);

        assertThat(registered).isEqualTo(3);
        assertThat(menuPackageItemMenuIds()).containsExactlyElementsOf(menuIds());
        assertThat(roleMenuIds()).containsExactlyElementsOf(menuIds());
        assertThat(bindingHandler.calls()).containsExactlyInAnyOrder("1:1", "2:1");
    }

    @Test
    @DisplayName("registerResourceManifest should attach root menu to declared parent code")
    void registerResourceManifestAttachesRootMenuToExistingParentCode() {
        seedParentMenu(2700L, "data");
        AppModuleResourceManifestCommand manifest = createManifest();
        manifest.getMenus().get(0).setParentCode("data");

        int registered = service.registerResourceManifest(manifest);

        assertThat(registered).isEqualTo(3);
        Menu directory = selectMenu("contract");
        Menu page = selectMenu("contract:archive:list");
        Menu button = selectMenu("contract:archive:create");
        assertThat(directory.getParentId()).isEqualTo(2700L);
        assertThat(page.getParentId()).isEqualTo(directory.getMenuId());
        assertThat(button.getParentId()).isEqualTo(page.getMenuId());
    }

    @Test
    @DisplayName("disable should disable module menus and clear derived bindings through real mappers")
    void disableDisablesMenusAndClearsDerivedBindingsThroughRealMappers() {
        seedPackage(1L, "internal-admin-default");
        seedRole(10L, 1L, "ROLE_ADMIN");
        AppModuleResourceManifestCommand manifest = createManifest();
        manifest.setPackageCodes(List.of("internal-admin-default"));
        manifest.setRoleCodes(List.of("ROLE_ADMIN"));
        service.registerResourceManifest(manifest);

        Boolean disabled = service.disable("internal-admin", "contract");

        assertThat(disabled).isTrue();
        assertThat(enabledMenuCount()).isZero();
        assertThat(activeModuleCount()).isZero();
        assertThat(countRows("frontend_menu_runtime_config")).isZero();
        assertThat(countRows("authorization_menu_package_item")).isZero();
        assertThat(countRows("authorization_role_menu")).isZero();
    }

    private void resetSchema() {
        jdbcTemplate.execute("drop table if exists frontend_menu_runtime_config");
        jdbcTemplate.execute("drop table if exists authorization_menu_package_item");
        jdbcTemplate.execute("drop table if exists authorization_menu_package");
        jdbcTemplate.execute("drop table if exists authorization_role_menu");
        jdbcTemplate.execute("drop table if exists authorization_role");
        jdbcTemplate.execute("drop table if exists authorization_menu");
        jdbcTemplate.execute("drop table if exists authorization_app_module");
        jdbcTemplate.execute("""
                create table authorization_app_module (
                    id bigint primary key,
                    app_code varchar(64) not null default 'internal-admin',
                    module_code varchar(64) not null,
                    module_name varchar(100) not null,
                    sort int not null default 0,
                    status tinyint not null default 1,
                    create_time timestamp default current_timestamp,
                    update_time timestamp default current_timestamp
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
                create table frontend_menu_runtime_config (
                    id bigint primary key,
                    app_code varchar(64) not null default 'internal-admin',
                    menu_id bigint not null,
                    page_type varchar(32) not null default 'LOCAL_ROUTE',
                    external_url varchar(512),
                    create_time timestamp default current_timestamp,
                    update_time timestamp default current_timestamp
                )
                """);
        jdbcTemplate.execute("""
                create table authorization_menu_package (
                    id bigint primary key,
                    package_name varchar(100) not null,
                    package_code varchar(100) not null,
                    app_code varchar(64) not null default 'internal-admin',
                    status tinyint not null default 1,
                    sort int not null default 0,
                    remark varchar(500),
                    create_by varchar(64),
                    update_by varchar(64),
                    create_time timestamp default current_timestamp,
                    update_time timestamp default current_timestamp,
                    del_flag tinyint not null default 0
                )
                """);
        jdbcTemplate.execute("""
                create table authorization_menu_package_item (
                    id bigint primary key,
                    package_id bigint not null,
                    menu_id bigint not null,
                    sort int not null default 0
                )
                """);
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
                create table authorization_role_menu (
                    id bigint primary key,
                    tenant_id bigint,
                    role_id bigint not null,
                    menu_id bigint not null
                )
                """);
    }

    private AppModuleResourceManifestCommand createManifest() {
        AppModuleResourceManifestCommand manifest = new AppModuleResourceManifestCommand();
        manifest.setAppCode("internal-admin");
        manifest.setModuleCode("contract");
        manifest.setModuleName("合同模块");

        AppModuleResourceManifestCommand.Menu directory = new AppModuleResourceManifestCommand.Menu();
        directory.setMenuType(1);
        directory.setMenuName("合同管理");
        directory.setMenuCode("contract");
        directory.setPath("/contract");
        directory.setSort(10);

        AppModuleResourceManifestCommand.Menu page = new AppModuleResourceManifestCommand.Menu();
        page.setMenuType(2);
        page.setMenuName("合同列表");
        page.setMenuCode("contract:archive:list");
        page.setPath("/contract/archives");
        page.setComponent("contract/archive/index");
        page.setPermissions(List.of("contract:archive:create", "contract:archive:delete"));

        AppModuleResourceManifestCommand.Permission create = new AppModuleResourceManifestCommand.Permission();
        create.setPermissionCode("contract:archive:create");
        create.setPermissionName("新增合同");
        page.setPermissionItems(List.of(create));
        directory.setChildren(List.of(page));
        manifest.setMenus(List.of(directory));
        return manifest;
    }

    private void seedPackage(Long packageId, String packageCode) {
        jdbcTemplate.update("""
                        insert into authorization_menu_package
                        (id, package_name, package_code, app_code, status)
                        values (?, ?, ?, 'internal-admin', 1)
                        """,
                packageId, packageCode, packageCode);
    }

    private void seedRole(Long roleId, Long tenantId, String roleCode) {
        jdbcTemplate.update("""
                        insert into authorization_role
                        (id, tenant_id, app_code, realm, actor_type, role_code, role_name)
                        values (?, ?, 'internal-admin', 'INTERNAL', 'INTERNAL_USER', ?, ?)
                        """,
                roleId, tenantId, roleCode, roleCode);
    }

    private void seedParentMenu(Long menuId, String menuCode) {
        jdbcTemplate.update("""
                        insert into authorization_menu
                        (id, tenant_id, app_code, module_code, parent_id, menu_type, menu_name, menu_code, status, visible, del_flag)
                        values (?, 1, 'internal-admin', 'mango-system', 0, 1, ?, ?, 1, 1, 0)
                        """,
                menuId, menuCode, menuCode);
    }

    private Menu selectMenu(String menuCode) {
        return menuMapper.selectList(null)
                .stream()
                .filter(menu -> menuCode.equals(menu.getMenuCode()))
                .findFirst()
                .orElseThrow();
    }

    private List<String> menuCodes() {
        return jdbcTemplate.queryForList(
                "select menu_code from authorization_menu where module_code = 'contract' order by menu_type, id",
                String.class);
    }

    private List<Long> menuIds() {
        return jdbcTemplate.queryForList(
                "select id from authorization_menu where module_code = 'contract' order by menu_type, id",
                Long.class);
    }

    private List<String> runtimeConfigTypes() {
        return jdbcTemplate.queryForList(
                "select page_type from frontend_menu_runtime_config order by menu_id",
                String.class);
    }

    private List<Long> menuPackageItemMenuIds() {
        return jdbcTemplate.queryForList(
                "select menu_id from authorization_menu_package_item order by menu_id",
                Long.class);
    }

    private List<Long> roleMenuIds() {
        return jdbcTemplate.queryForList(
                "select menu_id from authorization_role_menu order by menu_id",
                Long.class);
    }

    private Long enabledMenuCount() {
        return jdbcTemplate.queryForObject(
                "select count(*) from authorization_menu where status = 1",
                Long.class);
    }

    private Long activeModuleCount() {
        return jdbcTemplate.queryForObject(
                "select count(*) from authorization_app_module where status = 1",
                Long.class);
    }

    private Long countRows(String tableName) {
        return jdbcTemplate.queryForObject("select count(*) from " + tableName, Long.class);
    }

    @Configuration
    @MapperScan(basePackageClasses = AuthorizationAppModuleMapper.class)
    @Import(AppModuleServiceImpl.class)
    static class TestConfig {

        @Bean
        TenantPackageBindingProvider tenantPackageBindingProvider() {
            return packageId -> packageId == null ? List.of() : List.of(1L, 2L);
        }

        @Bean
        TestTenantMenuPackageBindingHandler tenantMenuPackageBindingHandler() {
            return new TestTenantMenuPackageBindingHandler();
        }
    }

    static class TestTenantMenuPackageBindingHandler extends TenantMenuPackageBindingHandler {

        private final List<String> calls = new ArrayList<>();

        TestTenantMenuPackageBindingHandler() {
            super(null, null, null, null);
        }

        @Override
        public void bindPackage(Long tenantId, Long packageId) {
            calls.add(tenantId + ":" + packageId);
        }

        List<String> calls() {
            return calls;
        }

        void clear() {
            calls.clear();
        }
    }
}
