package io.mango.authorization.starter.resource;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.IAuthorizationProvider;
import io.mango.authorization.core.mapper.SubjectRoleBindingMapper;
import io.mango.authorization.core.service.impl.SubjectAuthorityService;
import io.mango.authorization.starter.DefaultAuthorizationProvider;
import io.mango.authorization.starter.RolePermissionAuthorityContributor;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
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

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        ModuleDiagnosticPermissionAuthorizationIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:module_diagnostic_permission;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false"
})
class ModuleDiagnosticPermissionAuthorizationIntegrationTest {

    private static final String RESOURCE =
            "META-INF/mango/resources/authorization-common-menu.json";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private IAuthorizationProvider authorizationProvider;

    @BeforeEach
    void setUp() {
        resetSchema();
    }

    @Test
    void actualResourcePermissionReachesAuthorizationSnapshotThroughRealMybatisMappers() throws Exception {
        JsonNode item = diagnosticPermissionResource();
        String permission = item.path("apiCodes").get(0).asText();

        jdbcTemplate.update("""
                insert into authorization_role
                (id, tenant_id, app_code, realm, actor_type, role_code, role_name)
                values (10, 1, 'internal-admin', 'INTERNAL', 'INTERNAL_USER', 'ROLE_ADMIN', 'Admin')
                """);
        jdbcTemplate.update("""
                insert into authorization_subject_role
                (id, tenant_id, subject_id, subject_type, app_code, realm, actor_type, party_type, party_id, role_id)
                values (20, 1, 1001, 'TENANT_MEMBER', 'internal-admin', 'INTERNAL', 'INTERNAL_USER',
                        'INTERNAL_ORG', 1, 10)
                """);
        jdbcTemplate.update("""
                insert into authorization_menu
                (id, tenant_id, app_code, menu_name, menu_code, api_codes, menu_type, status, visible)
                values (30, 1, 'internal-admin', ?, ?, ?, ?, 1, 0)
                """,
                item.path("menuName").asText(),
                item.path("menuCode").asText(),
                permission,
                item.path("menuType").asInt());
        jdbcTemplate.update("""
                insert into authorization_role_menu (id, tenant_id, role_id, menu_id)
                values (40, 1, 10, 30)
                """);

        var snapshot = authorizationProvider.load(query("internal-admin"));
        var wrongAppSnapshot = authorizationProvider.load(query("customer-portal"));

        assertThat(snapshot.permissionCodes()).contains(permission);
        assertThat(snapshot.authorities()).contains(permission);
        assertThat(wrongAppSnapshot.permissionCodes()).doesNotContain(permission);
    }

    private JsonNode diagnosticPermissionResource() throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(RESOURCE)) {
            assertThat(input).isNotNull();
            JsonNode item = findMenu(new ObjectMapper().readTree(input), "system:module-diagnostic:read");
            assertThat(item).isNotNull();
            assertThat(item.path("menuType").asInt()).isEqualTo(3);
            assertThat(item.hasNonNull("path")).isFalse();
            assertThat(item.hasNonNull("component")).isFalse();
            return item;
        }
    }

    private JsonNode findMenu(JsonNode node, String menuCode) {
        if (node.isObject() && menuCode.equals(node.path("menuCode").asText())) {
            return node;
        }
        for (JsonNode child : node) {
            JsonNode found = findMenu(child, menuCode);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private AuthorizationQuery query(String appCode) {
        return AuthorizationQuery.member(1001L)
                .withTenantId("1")
                .withSystemCode(appCode)
                .withRealm("INTERNAL")
                .withActorType("INTERNAL_USER")
                .withParty("INTERNAL_ORG", 1L);
    }

    private void resetSchema() {
        jdbcTemplate.execute("drop table if exists authorization_subject_role");
        jdbcTemplate.execute("drop table if exists authorization_role_menu");
        jdbcTemplate.execute("drop table if exists authorization_menu");
        jdbcTemplate.execute("drop table if exists authorization_role");
        jdbcTemplate.execute("""
                create table authorization_subject_role (
                    id bigint primary key,
                    tenant_id bigint not null,
                    subject_id bigint not null,
                    subject_type varchar(32) not null,
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
                    tenant_id bigint not null,
                    role_id bigint not null,
                    menu_id bigint not null
                )
                """);
        jdbcTemplate.execute("""
                create table authorization_menu (
                    id bigint primary key,
                    tenant_id bigint not null,
                    app_code varchar(64) not null,
                    module_code varchar(64),
                    parent_id bigint not null default 0,
                    menu_type tinyint not null,
                    menu_name varchar(100) not null,
                    menu_code varchar(128),
                    path varchar(255),
                    icon varchar(64),
                    sort int not null default 0,
                    status tinyint not null,
                    visible tinyint not null,
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
                    remark varchar(500)
                )
                """);
        AuthorizationStarterTestSchema.ensureCanonicalColumns(jdbcTemplate);
    }

    @Configuration
    @MapperScan(basePackageClasses = SubjectRoleBindingMapper.class)
    @Import({
            SubjectAuthorityService.class,
            RolePermissionAuthorityContributor.class,
            DefaultAuthorizationProvider.class
    })
    static class TestConfig {
    }
}
