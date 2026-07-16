package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.mango.authorization.api.command.TenantAppBindingCommand;
import io.mango.authorization.api.query.TenantAppBindingQuery;
import io.mango.authorization.api.vo.TenantAppBindingVO;
import io.mango.authorization.core.mapper.MenuMapper;
import io.mango.authorization.core.mapper.RoleMapper;
import io.mango.authorization.core.mapper.RoleMenuMapper;
import io.mango.authorization.core.service.ITenantAppBindingService;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.system.api.tenant.TenantProvisionCommand;
import org.junit.jupiter.api.AfterEach;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        AuthorizationTenantProvisionerIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:authorization_tenant_provisioner;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=true",
        "mango.persistence.mybatis-plus.tenant.default-tenant-id=1"
})
class AuthorizationTenantProvisionerIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AuthorizationTenantProvisioner provisioner;

    @BeforeEach
    void setUp() {
        resetSchema();
        seedTemplateRole(10L, AuthorizationTenantProvisioner.LOGIN_ROLE);
        seedTemplateRole(11L, AuthorizationTenantProvisioner.ANONYMOUS_ROLE);
        seedMenu(100L, "notice:site-message");
        seedMenu(101L, "public:home");
        seedRoleMenu(1000L, 10L, 100L);
        seedRoleMenu(1001L, 11L, 101L);
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("2"));
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void provisionCreatesBuiltInRolesAndCopiesPlatformRoleMenusIdempotently() {
        TenantProvisionCommand command = new TenantProvisionCommand(2L, "tenant-2", "租户二");

        provisioner.provision(command);
        provisioner.provision(command);

        assertThat(MangoContextHolder.tenantId()).isEqualTo("2");
        assertThat(jdbcTemplate.queryForList("""
                        select role_code from authorization_role
                        where tenant_id = 2 order by sort
                        """, String.class))
                .containsExactly(
                        AuthorizationTenantProvisioner.TENANT_ADMIN_ROLE,
                        AuthorizationTenantProvisioner.LOGIN_ROLE,
                        AuthorizationTenantProvisioner.ANONYMOUS_ROLE);
        assertThat(jdbcTemplate.queryForList("""
                        select m.menu_code
                        from authorization_role_menu rm
                        join authorization_role r on r.id = rm.role_id
                        join authorization_menu m on m.id = rm.menu_id
                        where rm.tenant_id = 2 and r.role_code = 'ROLE_LOGIN'
                        """, String.class))
                .containsExactly("notice:site-message");
        assertThat(jdbcTemplate.queryForList("""
                        select m.menu_code
                        from authorization_role_menu rm
                        join authorization_role r on r.id = rm.role_id
                        join authorization_menu m on m.id = rm.menu_id
                        where rm.tenant_id = 2 and r.role_code = 'ROLE_ANONYMOUS'
                        """, String.class))
                .containsExactly("public:home");
    }

    private void resetSchema() {
        jdbcTemplate.execute("drop table if exists authorization_role_menu");
        jdbcTemplate.execute("drop table if exists authorization_menu");
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
                    remark varchar(500),
                    del_flag tinyint not null default 0
                )
                """);
        jdbcTemplate.execute("""
                create unique index uk_authorization_role_tenant_app_role_code
                on authorization_role(tenant_id, app_code, role_code)
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
                    component varchar(255),
                    sort int not null default 0,
                    status tinyint not null default 1,
                    visible tinyint not null default 1,
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
                create table authorization_role_menu (
                    id bigint primary key,
                    tenant_id bigint not null,
                    role_id bigint not null,
                    menu_id bigint not null
                )
                """);
        AuthorizationTestSchema.ensureCanonicalColumns(jdbcTemplate);
    }

    private void seedTemplateRole(Long roleId, String roleCode) {
        jdbcTemplate.update("""
                        insert into authorization_role
                        (id, tenant_id, app_code, realm, actor_type, role_code, role_name, sort)
                        values (?, 1, 'internal-admin', 'INTERNAL', 'INTERNAL_USER', ?, ?, 1)
                        """,
                roleId, roleCode, roleCode);
    }

    private void seedMenu(Long menuId, String menuCode) {
        jdbcTemplate.update("""
                        insert into authorization_menu
                        (id, tenant_id, app_code, menu_name, menu_code)
                        values (?, 1, 'internal-admin', ?, ?)
                        """,
                menuId, menuCode, menuCode);
    }

    private void seedRoleMenu(Long id, Long roleId, Long menuId) {
        jdbcTemplate.update("""
                        insert into authorization_role_menu (id, tenant_id, role_id, menu_id)
                        values (?, 1, ?, ?)
                        """,
                id, roleId, menuId);
    }

    @Configuration
    @MapperScan(basePackageClasses = {RoleMapper.class, MenuMapper.class, RoleMenuMapper.class})
    @Import(AuthorizationTenantProvisioner.class)
    static class TestConfig {

        @Bean
        ITenantAppBindingService tenantAppBindingService() {
            return new NoopTenantAppBindingService();
        }
    }

    static class NoopTenantAppBindingService implements ITenantAppBindingService {

        @Override
        public List<TenantAppBindingVO> list(TenantAppBindingQuery query) {
            return List.of();
        }

        @Override
        public Long enable(TenantAppBindingCommand command) {
            return null;
        }

        @Override
        public Boolean disable(Long tenantId, String appCode) {
            return false;
        }

        @Override
        public Boolean disableRequired(Long tenantId, String appCode) {
            return false;
        }

        @Override
        public void ensureEnabled(Long tenantId, String appCode) {
        }

        @Override
        public boolean isEnabled(Long tenantId, String appCode) {
            return true;
        }
    }
}
