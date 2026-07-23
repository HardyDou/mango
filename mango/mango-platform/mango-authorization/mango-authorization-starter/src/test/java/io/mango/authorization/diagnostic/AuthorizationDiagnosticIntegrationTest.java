package io.mango.authorization.diagnostic;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.mango.authorization.core.mapper.ApiResourceMapper;
import io.mango.authorization.core.mapper.MenuMapper;
import io.mango.authorization.starter.diagnostic.AuthorizationModuleDiagnosticContributor;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.module.api.diagnostic.ModuleConditionStatus;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticProfile;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticRequest;
import io.mango.infra.module.api.diagnostic.ModuleInstallation;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.resource.api.ResourceAuthorizationRequirementsProvider;
import io.mango.resource.api.vo.ApiRequirementVO;
import io.mango.resource.api.vo.AuthorizationRequirementsVO;
import io.mango.resource.api.vo.MenuRequirementVO;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        AuthorizationDiagnosticIntegrationTest.TestConfig.class
})
@ImportAutoConfiguration(AuthorizationDiagnosticAutoConfiguration.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:authorization_diagnostic;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=true"
})
class AuthorizationDiagnosticIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AuthorizationModuleDiagnosticContributor contributor;

    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("drop table if exists authorization_menu");
        jdbcTemplate.execute("drop table if exists authorization_api_resource");
        jdbcTemplate.execute("""
                create table authorization_menu (
                    id bigint primary key,
                    tenant_id varchar(64) not null,
                    app_code varchar(64) not null,
                    module_code varchar(64) not null,
                    menu_code varchar(100) not null,
                    component varchar(255),
                    api_codes varchar(1000),
                    status int not null,
                    del_flag int not null
                )
                """);
        jdbcTemplate.execute("""
                create table authorization_api_resource (
                    id bigint primary key,
                    tenant_id varchar(64) not null,
                    module_name varchar(100) not null,
                    http_method varchar(16) not null,
                    path_pattern varchar(255) not null,
                    resource_code varchar(255),
                    permission_code varchar(255),
                    access_mode varchar(32),
                    status int not null,
                    deleted int not null
                )
                """);
    }

    @AfterEach
    void clearContext() {
        MangoContextHolder.clear();
    }

    @Test
    void explicitPlatformScopeAndRealMybatisQueriesProducePass() {
        insertMenu(1, "1", "internal-admin", "link:item:view");
        insertMenu(2, "2", "internal-admin", "wrong:tenant");
        insertMenu(3, "1", "customer-portal", "wrong:app");
        jdbcTemplate.update("""
                insert into authorization_api_resource (
                    id, tenant_id, module_name, http_method, path_pattern,
                    resource_code, permission_code, access_mode, status, deleted
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, 10L, "default", "mango-link", "GET", "/link/items",
                "GET:/link/items", "link:item:view", "PERMISSION", 1, 0);
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("999"));

        var condition = contributor.contribute(request()).iterator().next();

        assertThat(condition.status()).isEqualTo(ModuleConditionStatus.PASS);
        assertThat(condition.reasonCode()).isEqualTo("AUTHORIZATION_MATERIALIZED");
        assertThat(MangoContextHolder.tenantId()).isEqualTo("999");
    }

    @Test
    void platformTenantApiRowsCannotSubstituteGlobalApiCatalog() {
        insertMenu(1, "1", "internal-admin", "link:item:view");
        jdbcTemplate.update("""
                insert into authorization_api_resource (
                    id, tenant_id, module_name, http_method, path_pattern,
                    resource_code, permission_code, access_mode, status, deleted
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, 10L, "1", "mango-link", "GET", "/link/items",
                "GET:/link/items", "link:item:view", "PERMISSION", 1, 0);

        var condition = contributor.contribute(request()).iterator().next();

        assertThat(condition.status()).isEqualTo(ModuleConditionStatus.FAIL);
        assertThat(condition.reasonCode()).isEqualTo("AUTHORIZATION_MENU_API_MISMATCH");
        assertThat(condition.evidence()).containsEntry("missingApiCount", 1);
    }

    @Test
    void diagnosticMappedStatementsHaveBoundedTimeout() {
        var configuration = sqlSessionFactory.getConfiguration();

        assertThat(configuration.getMappedStatement(
                MenuMapper.class.getName() + ".selectDiagnosticMenus").getTimeout())
                .isEqualTo(MenuMapper.DIAGNOSTIC_QUERY_TIMEOUT_SECONDS);
        assertThat(configuration.getMappedStatement(
                ApiResourceMapper.class.getName() + ".selectDiagnosticApis").getTimeout())
                .isEqualTo(ApiResourceMapper.DIAGNOSTIC_QUERY_TIMEOUT_SECONDS);
    }

    private void insertMenu(long id, String tenantId, String appCode, String apiCodes) {
        jdbcTemplate.update("""
                insert into authorization_menu (
                    id, tenant_id, app_code, module_code, menu_code,
                    component, api_codes, status, del_flag
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, tenantId, appCode, "mango-link", "data:link:item",
                "link/items/index", apiCodes, 1, 0);
    }

    private ModuleDiagnosticRequest request() {
        return new ModuleDiagnosticRequest(
                "mango-link",
                "internal-admin",
                ModuleDiagnosticProfile.ADMIN_MODULE_RUNTIME_V1,
                Map.of(ModuleInstallation.RESOURCE_MODULE_ATTRIBUTE, "link"));
    }

    @Configuration
    @MapperScan(basePackageClasses = {MenuMapper.class, ApiResourceMapper.class})
    static class TestConfig {

        @Bean
        ResourceAuthorizationRequirementsProvider requirementsProvider() {
            return (resourceModule, runtimeModule, appCode) -> {
                if (!"link".equals(resourceModule)
                        || !"mango-link".equals(runtimeModule)
                        || !"internal-admin".equals(appCode)) {
                    return AuthorizationRequirementsVO.empty();
                }
                return new AuthorizationRequirementsVO(
                        List.of(new MenuRequirementVO(
                                "internal-admin",
                                "mango-link",
                                "data:link:item",
                                "link/items/index",
                                List.of("link:item:view"),
                                1)),
                        List.of(new ApiRequirementVO(
                                "mango-link",
                                "GET",
                                "/link/items",
                                "GET:/link/items",
                                "link:item:view",
                                "PERMISSION",
                                1)),
                        true);
            };
        }
    }
}
