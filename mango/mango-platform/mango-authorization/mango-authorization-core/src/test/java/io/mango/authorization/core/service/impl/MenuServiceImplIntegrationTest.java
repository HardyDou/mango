package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.vo.ButtonDisplayRuleVO;
import io.mango.authorization.api.vo.MenuVO;
import io.mango.authorization.core.entity.Menu;
import io.mango.authorization.core.mapper.MenuMapper;
import io.mango.authorization.core.service.ISubjectAuthorityService;
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
        MenuServiceImplIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:menu_service;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false"
})
@DisplayName("MenuServiceImpl 集成测试")
class MenuServiceImplIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MenuMapper menuMapper;

    @Autowired
    private MenuServiceImpl service;

    @BeforeEach
    void setUp() {
        resetSchema();
    }

    @Test
    @DisplayName("addMenu and getById should persist and read runtime config through real mappers")
    void addMenuAndGetByIdPersistsRuntimeConfigThroughRealMappers() {
        Menu menu = menu(1L, 0L, "Frame Menu", "frame:menu", 2, 1);
        menu.setPageType("IFRAME");
        menu.setExternalUrl("https://example.com/frame");

        boolean saved = service.addMenu(menu);
        Menu persisted = service.getById(1L);

        assertThat(saved).isTrue();
        assertThat(persisted).isNotNull();
        assertThat(persisted.getMenuName()).isEqualTo("Frame Menu");
        assertThat(persisted.getPageType()).isEqualTo("IFRAME");
        assertThat(persisted.getExternalUrl()).isEqualTo("https://example.com/frame");
        assertThat(countRuntimeConfigs()).isEqualTo(1L);
    }

    @Test
    @DisplayName("listUserMenus should keep hidden enabled menus for route registration")
    void listUserMenusKeepsHiddenEnabledMenusForRouteRegistration() {
        seedEnabledModule("mango-notice");
        seedMenu(menu(10L, 0L, "Root", "root", 1, 1, "mango-notice"));
        seedMenu(menu(11L, 10L, "Visible", "visible", 2, 1, "mango-notice"));
        Menu hiddenMenu = menu(12L, 10L, "Hidden", "hidden", 2, 1, "mango-notice");
        hiddenMenu.setVisible(0);
        seedMenu(hiddenMenu);

        List<MenuVO> result = service.listUserMenus(
                "internal-admin",
                null,
                null,
                AuthorizationQuery.user(1L),
                true);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getChildren()).hasSize(2);
        assertThat(result.get(0).getChildren())
                .anyMatch(menu -> Long.valueOf(12L).equals(menu.getMenuId())
                        && Integer.valueOf(0).equals(menu.getVisible()));
    }

    @Test
    @DisplayName("buildMenuTree should keep workflow manage children under workflow root")
    void buildMenuTreeKeepsWorkflowManageChildrenNested() {
        seedMenu(menu(26L, 0L, "审批中心", "workflow", 1, 2, "mango-workflow"));
        seedMenu(menu(2601L, 26L, "流程办理", "workflow:task", 2, 1, "mango-workflow"));
        seedMenu(menu(2604L, 26L, "流程管理", "workflow:manage", 1, 3, "mango-workflow"));
        seedMenu(menu(260401L, 2604L, "流程模板", "workflow:template", 2, 1, "mango-workflow"));
        seedMenu(menu(260402L, 2604L, "流程定义", "workflow:definition", 2, 2, "mango-workflow"));

        List<MenuVO> result = service.listMenus("internal-admin", "mango-workflow", null, null, null, 1, true);

        assertThat(result).hasSize(1);
        MenuVO workflowNode = result.get(0);
        assertThat(workflowNode.getMenuId()).isEqualTo(26L);
        assertThat(workflowNode.getChildren()).hasSize(2);
        MenuVO manageNode = workflowNode.getChildren().stream()
                .filter(menu -> Long.valueOf(2604L).equals(menu.getMenuId()))
                .findFirst()
                .orElseThrow();
        assertThat(manageNode.getChildren())
                .extracting(MenuVO::getMenuId)
                .containsExactly(260401L, 260402L);
    }

    @Test
    @DisplayName("deleteMenu should reject parent menu and delete leaf runtime config through real mappers")
    void deleteMenuRejectsParentAndDeletesLeafRuntimeConfigThroughRealMappers() {
        seedMenu(menu(1L, 0L, "Parent", "parent", 1, 1));
        seedMenu(menu(2L, 1L, "Child", "child", 2, 1));
        seedRuntimeConfig(2L, "IFRAME", "https://example.com/child");

        boolean parentDeleted = service.deleteMenu(1L);
        boolean childDeleted = service.deleteMenu(2L);

        assertThat(parentDeleted).isFalse();
        assertThat(childDeleted).isTrue();
        assertThat(menuMapper.selectById(1L)).isNotNull();
        assertThat(menuMapper.selectById(2L)).isNull();
        assertThat(countRuntimeConfigs()).isZero();
    }

    private void resetSchema() {
        jdbcTemplate.execute("drop table if exists frontend_menu_runtime_config");
        jdbcTemplate.execute("drop table if exists authorization_app_module");
        jdbcTemplate.execute("drop table if exists authorization_subject_role");
        jdbcTemplate.execute("drop table if exists authorization_role_menu");
        jdbcTemplate.execute("drop table if exists authorization_menu");
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
                create table authorization_app_module (
                    id bigint primary key,
                    app_code varchar(64) not null default 'internal-admin',
                    module_code varchar(64) not null,
                    module_name varchar(100) not null,
                    sort int not null default 0,
                    status tinyint not null default 1,
                    create_time timestamp default current_timestamp,
                    update_time timestamp default current_timestamp,
                    remark varchar(500)
                )
                """);
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
    }

    private Menu menu(Long menuId, Long parentId, String menuName, String menuCode, Integer menuType, Integer sort) {
        return menu(menuId, parentId, menuName, menuCode, menuType, sort, "mango-system");
    }

    private Menu menu(Long menuId,
                      Long parentId,
                      String menuName,
                      String menuCode,
                      Integer menuType,
                      Integer sort,
                      String moduleCode) {
        Menu menu = new Menu();
        menu.setMenuId(menuId);
        menu.setTenantId(1L);
        menu.setAppCode("internal-admin");
        menu.setModuleCode(moduleCode);
        menu.setParentId(parentId);
        menu.setMenuName(menuName);
        menu.setMenuCode(menuCode);
        menu.setMenuType(menuType);
        menu.setSort(sort);
        menu.setStatus(1);
        menu.setVisible(1);
        menu.setKeepAlive(0);
        menu.setEmbedded(0);
        menu.setDelFlag(0);
        return menu;
    }

    private void seedMenu(Menu menu) {
        menuMapper.insert(menu);
    }

    private void seedEnabledModule(String moduleCode) {
        jdbcTemplate.update("""
                        insert into authorization_app_module
                        (id, app_code, module_code, module_name, status)
                        values (?, 'internal-admin', ?, ?, 1)
                        """,
                Math.abs(moduleCode.hashCode()), moduleCode, moduleCode);
    }

    private void seedRuntimeConfig(Long menuId, String pageType, String externalUrl) {
        jdbcTemplate.update("""
                        insert into frontend_menu_runtime_config
                        (id, app_code, menu_id, page_type, external_url)
                        values (?, 'internal-admin', ?, ?, ?)
                        """,
                menuId, menuId, pageType, externalUrl);
    }

    private Long countRuntimeConfigs() {
        return jdbcTemplate.queryForObject("select count(*) from frontend_menu_runtime_config", Long.class);
    }

    @Configuration
    @MapperScan(basePackageClasses = MenuMapper.class)
    @Import(MenuServiceImpl.class)
    static class TestConfig {

        @Bean
        ISubjectAuthorityService subjectAuthorityService() {
            return new AllPermissionSubjectAuthorityService();
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
