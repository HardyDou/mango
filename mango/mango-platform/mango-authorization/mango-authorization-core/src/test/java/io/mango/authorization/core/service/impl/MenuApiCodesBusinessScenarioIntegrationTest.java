package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.query.MenuTreeQuery;
import io.mango.authorization.api.command.AppModuleMenuRequest;
import io.mango.authorization.api.command.AppModuleResourceManifestCommand;
import io.mango.authorization.api.vo.MenuVO;
import io.mango.authorization.core.mapper.AuthorizationAppModuleMapper;
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
        MenuApiCodesBusinessScenarioIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:menu_api_codes_business_scenario;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false"
})
@DisplayName("menuCode/apiCodes 真实业务接入场景集成测试")
class MenuApiCodesBusinessScenarioIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AppModuleService appModuleService;

    @Autowired
    private MenuService menuService;

    @Autowired
    private SubjectAuthorityService subjectAuthorityService;

    @BeforeEach
    void setUp() {
        resetSchema();
        seedRole(10L, "ROLE_SALE");
        seedRole(11L, "ROLE_RISK");
        seedRole(20L, SubjectAuthorityService.ROLE_LOGIN);
        seedRole(21L, SubjectAuthorityService.ROLE_ANONYMOUS);
    }

    @Test
    @DisplayName("业务角色可通过业务菜单获得 workflow 接口权限，但不会暴露审批中心或风控菜单")
    void businessRoleGetsWorkflowApiCodesWithoutWorkflowMenus() {
        appModuleService.registerResourceManifest(riskWorkflowManifest());
        appModuleService.registerResourceManifest(discountApprovalManifest());
        seedSubjectRole(1L, 1001L, 10L);

        AuthorizationQuery query = memberQuery(1001L);
        List<String> permissions = subjectAuthorityService.listSubjectPermissions(query);
        List<String> menuCodes = flattenMenuCodes(menuService.listUserMenus(menuTreeQuery(), query));

        assertThat(permissions)
                .contains("discount:approval:view",
                        "workflow:definition:list",
                        "workflow:process:start",
                        "notice:site:view",
                        "notice:receive-setting:view")
                .doesNotContain("workflow:risk:view", "workflow:risk:approve");
        assertThat(menuCodes)
                .contains("discount", "discount:approval")
                .doesNotContain("workflow", "workflow:risk-approval", "discount:basic-login");
    }

    @Test
    @DisplayName("匿名角色可通过隐藏菜单获得文件基础接口权限，但不会暴露文件菜单")
    void anonymousRoleGetsHiddenFileApiCodesWithoutMenus() {
        appModuleService.registerResourceManifest(fileBasicManifest());

        AuthorizationQuery query = AuthorizationQuery.anonymous()
                .withTenantId("1")
                .withSystemCode("internal-admin");
        List<String> permissions = subjectAuthorityService.listSubjectPermissions(query);
        List<String> menuCodes = flattenMenuCodes(menuService.listUserMenus(menuTreeQuery(), query));

        assertThat(permissions)
                .containsExactly("file:files:query", "file:files:upload", "file:files:download", "file:settings:query");
        assertThat(menuCodes).isEmpty();
    }

    private AppModuleResourceManifestCommand riskWorkflowManifest() {
        AppModuleResourceManifestCommand manifest = manifest("workflow", "审批中心", List.of("ROLE_RISK"));
        AppModuleMenuRequest root = menu(1, "审批中心", "workflow", "/workflow", null);
        AppModuleMenuRequest riskApproval = menu(2, "风控审批", "workflow:risk-approval",
                "/workflow/risk-approval", "workflow/risk-approval/index");
        riskApproval.setApiCodes(List.of("workflow:risk:view", "workflow:risk:approve"));
        root.setChildren(List.of(riskApproval));
        manifest.setMenus(List.of(root));
        return manifest;
    }

    private MenuTreeQuery menuTreeQuery() {
        MenuTreeQuery query = new MenuTreeQuery();
        query.setAppCode("internal-admin");
        query.setStatus(1);
        query.setFmt("tree");
        return query;
    }

    private AppModuleResourceManifestCommand discountApprovalManifest() {
        AppModuleResourceManifestCommand manifest = manifest("discount", "优惠审批", List.of("ROLE_SALE"));
        AppModuleMenuRequest root = menu(1, "优惠管理", "discount", "/discount", null);
        AppModuleMenuRequest approval = menu(2, "优惠审批", "discount:approval",
                "/discount/approval", "discount/approval/index");
        approval.setApiCodes(List.of("discount:approval:view", "workflow:definition:list", "workflow:process:start"));

        AppModuleMenuRequest loginBasic = menu(2, "优惠审批登录基础权限",
                "discount:basic-login", "/discount/basic-login", null);
        loginBasic.setVisible(0);
        loginBasic.setRoleCodes(List.of(SubjectAuthorityService.ROLE_LOGIN));
        loginBasic.setPackageCodes(List.of());
        loginBasic.setApiCodes(List.of("notice:site:view", "notice:receive-setting:view"));

        root.setChildren(List.of(approval, loginBasic));
        manifest.setMenus(List.of(root));
        return manifest;
    }

    private AppModuleResourceManifestCommand fileBasicManifest() {
        AppModuleResourceManifestCommand manifest = manifest("file", "文件中心", List.of());
        AppModuleMenuRequest root = menu(1, "文件中心", "file", "/file", null);
        AppModuleMenuRequest anonymousBasic = menu(2, "文件匿名基础权限",
                "file:basic-anonymous", "/file/basic-anonymous", null);
        anonymousBasic.setVisible(0);
        anonymousBasic.setRoleCodes(List.of(SubjectAuthorityService.ROLE_ANONYMOUS));
        anonymousBasic.setPackageCodes(List.of());
        anonymousBasic.setApiCodes(List.of("file:files:query",
                "file:files:upload",
                "file:files:download",
                "file:settings:query"));
        root.setChildren(List.of(anonymousBasic));
        manifest.setMenus(List.of(root));
        return manifest;
    }

    private AppModuleResourceManifestCommand manifest(String moduleCode, String moduleName, List<String> roleCodes) {
        AppModuleResourceManifestCommand manifest = new AppModuleResourceManifestCommand();
        manifest.setAppCode("internal-admin");
        manifest.setModuleCode(moduleCode);
        manifest.setModuleName(moduleName);
        manifest.setRoleCodes(roleCodes);
        manifest.setPackageCodes(List.of());
        return manifest;
    }

    private AppModuleMenuRequest menu(Integer menuType,
                                                       String menuName,
                                                       String menuCode,
                                                       String path,
                                                       String component) {
        AppModuleMenuRequest menu = new AppModuleMenuRequest();
        menu.setMenuType(menuType);
        menu.setMenuName(menuName);
        menu.setMenuCode(menuCode);
        menu.setPath(path);
        menu.setComponent(component);
        menu.setSort(10);
        return menu;
    }

    private AuthorizationQuery memberQuery(Long subjectId) {
        return AuthorizationQuery.member(subjectId)
                .withTenantId("1")
                .withSystemCode("internal-admin")
                .withRealm("INTERNAL")
                .withActorType("INTERNAL_USER")
                .withParty("INTERNAL_ORG", 1L);
    }

    private List<String> flattenMenuCodes(List<MenuVO> menus) {
        List<String> codes = new ArrayList<>();
        collectMenuCodes(menus, codes);
        return codes;
    }

    private void collectMenuCodes(List<MenuVO> menus, List<String> collector) {
        if (menus == null || menus.isEmpty()) {
            return;
        }
        for (MenuVO menu : menus) {
            collector.add(menu.getMenuCode());
            collectMenuCodes(menu.getChildren(), collector);
        }
    }

    private void seedRole(Long roleId, String roleCode) {
        jdbcTemplate.update("""
                        insert into authorization_role
                        (id, tenant_id, app_code, realm, actor_type, role_code, role_name, status)
                        values (?, 1, 'internal-admin', 'INTERNAL', 'INTERNAL_USER', ?, ?, 1)
                        """,
                roleId, roleCode, roleCode);
    }

    private void seedSubjectRole(Long id, Long subjectId, Long roleId) {
        jdbcTemplate.update("""
                        insert into authorization_subject_role
                        (id, tenant_id, subject_id, subject_type, app_code, realm, actor_type, party_type, party_id, role_id)
                        values (?, 1, ?, 'TENANT_MEMBER', 'internal-admin', 'INTERNAL', 'INTERNAL_USER', 'INTERNAL_ORG', 1, ?)
                        """,
                id, subjectId, roleId);
    }

    private void resetSchema() {
        jdbcTemplate.execute("drop table if exists frontend_menu_runtime_config");
        jdbcTemplate.execute("drop table if exists authorization_menu_package_item");
        jdbcTemplate.execute("drop table if exists authorization_menu_package");
        jdbcTemplate.execute("drop table if exists authorization_subject_role");
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
                    api_codes varchar(2000),
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
        AuthorizationTestSchema.ensureCanonicalColumns(jdbcTemplate);
    }

    @Configuration
    @MapperScan(basePackageClasses = AuthorizationAppModuleMapper.class)
    @Import({AppModuleService.class, MenuService.class, SubjectAuthorityService.class})
    static class TestConfig {

        @Bean
        TenantPackageBindingProvider tenantPackageBindingProvider() {
            return packageId -> List.of();
        }

        @Bean
        TenantMenuPackageBindingHandler tenantMenuPackageBindingHandler() {
            return new TenantMenuPackageBindingHandler(null, null, null, null);
        }
    }
}
