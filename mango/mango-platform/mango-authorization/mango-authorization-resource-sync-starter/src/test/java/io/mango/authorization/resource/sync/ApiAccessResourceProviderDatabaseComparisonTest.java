package io.mango.authorization.resource.sync;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.mango.authorization.api.ApiResourceApi;
import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.annotation.InternalApi;
import io.mango.authorization.api.annotation.LoginApi;
import io.mango.authorization.api.annotation.PermissionAccess;
import io.mango.authorization.api.annotation.PublicApi;
import io.mango.authorization.api.command.ApiResourceRegisterCommand;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.authorization.api.query.ApiResourceAccessDecisionQuery;
import io.mango.authorization.api.vo.ApiResourceAccessDecisionVO;
import io.mango.authorization.api.vo.ApiResourceRegisterResultVO;
import io.mango.authorization.core.mapper.ApiResourceMapper;
import io.mango.authorization.core.service.IApiResourceService;
import io.mango.authorization.core.service.impl.ApiResourceServiceImpl;
import io.mango.authorization.starter.resource.ApiResourceHandler;
import io.mango.common.result.R;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.resource.api.ResourceProvider;
import io.mango.resource.api.enums.ResourceSyncMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = ApiAccessResourceProviderDatabaseComparisonTest.TestApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(properties = {
        "spring.application.name=resource-sync-db-test",
        "spring.datasource.url=jdbc:h2:mem:api_resource_compare;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false",
        "mango.authorization.resource-sync.enabled=false",
        "mango.authorization.resource-sync.resource-provider.enabled=false",
        "mango.authorization.resource-sync.module-name=mango-resource-sync-test",
        "mango.authorization.resource-sync.provider-module-code=authorization",
        "mango.authorization.resource-sync.include-packages=io.mango.authorization.resource.sync",
        "mango.authorization.resource-sync.exclude-paths=/error",
        "mango.authorization.resource-sync.default-access-mode=LOGIN",
        "mango.authorization.resource-sync.resources[0].module-name=mango-doc",
        "mango.authorization.resource-sync.resources[0].http-method=GET",
        "mango.authorization.resource-sync.resources[0].path-pattern=/swagger-ui/**",
        "mango.authorization.resource-sync.resources[0].access-mode=PUBLIC",
        "mango.authorization.resource-sync.resources[0].description=Swagger UI",
        "spring.autoconfigure.exclude="
                + "org.springframework.cloud.gateway.config.GatewayAutoConfiguration,"
                + "org.springframework.cloud.gateway.config.GatewayClassPathWarningAutoConfiguration,"
                + "io.mango.authorization.starter.AuthorizationAutoConfiguration,"
                + "io.mango.authorization.support.autoconfigure.TokenAutoConfiguration,"
                + "io.mango.authorization.support.autoconfigure.SecurityAutoConfiguration"
})
class ApiAccessResourceProviderDatabaseComparisonTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Autowired
    private ApiResourceApi apiResourceApi;

    @Autowired
    private ApiResourceSyncProperties apiResourceSyncProperties;

    @Autowired
    private ResourceProvider resourceProvider;

    @Autowired
    private ApiResourceHandler apiResourceHandler;

    @BeforeEach
    void setUp() {
        rebuildTables();
    }

    @Test
    void resourceProviderSyncWritesSameAuthorizationApiResourceContentAsLegacyRunner() {
        new ApiResourceSyncRunner(
                handlerMapping,
                apiResourceApi,
                new EmptyModuleInfoRegistryProvider(),
                apiResourceSyncProperties).run(null);
        List<ApiResourceSnapshot> legacyRows = apiResourceRows();

        clearRuntimeTables();

        apiResourceHandler.upsertBatch(resourceProvider.provide());
        List<ApiResourceSnapshot> resourceRows = apiResourceRows();

        assertThat(resourceRows).containsExactlyElementsOf(legacyRows);
    }

    @Test
    void resourceProviderResyncKeepsAllApiResourcesActiveWhenDeclarationsAreUnchanged() {
        apiResourceHandler.upsertBatch(resourceProvider.provide());
        List<ApiResourceSnapshot> firstRows = apiResourceRows();

        apiResourceHandler.upsertBatch(resourceProvider.provide());
        List<ApiResourceSnapshot> secondRows = apiResourceRows();

        assertThat(secondRows).containsExactlyElementsOf(firstRows);
        assertThat(secondRows)
                .hasSize(9)
                .allSatisfy(row -> assertThat(row.status()).isEqualTo(1));
    }

    @Test
    void resourceProviderBatchSyncDoesNotOverrideManualApiResource() {
        apiResourceHandler.upsertBatch(resourceProvider.provide());
        Long manualTargetId = jdbcTemplate.queryForObject("""
                select id
                from authorization_api_resource
                where module_name = 'mango-resource-sync-test'
                  and http_method = 'GET'
                  and path_pattern = '/resource-sync/login'
                """, Long.class);
        jdbcTemplate.update("""
                update authorization_api_resource
                set description = 'Manual login resource', status = 1
                where id = ?
                """, manualTargetId);

        apiResourceHandler.upsertBatch(resourceProvider.provide().stream()
                .peek(resource -> {
                    String pathPattern = String.valueOf(resource.getFields().get("pathPattern").getValue());
                    if ("/resource-sync/login".equals(pathPattern)) {
                        resource.setSyncMode(ResourceSyncMode.MANUAL);
                    }
                })
                .toList());

        ApiResourceSnapshot manualRow = apiResourceRows().stream()
                .filter(row -> "/resource-sync/login".equals(row.pathPattern()))
                .findFirst()
                .orElseThrow();
        assertThat(manualRow.description()).isEqualTo("Manual login resource");
        assertThat(manualRow.status()).isEqualTo(1);
        assertThat(apiResourceRows()).hasSize(9);
    }

    private List<ApiResourceSnapshot> apiResourceRows() {
        return jdbcTemplate.query("""
                        select module_name, http_method, path_pattern, resource_code, permission_code,
                               access_mode, handler_class, handler_method, description, status, deleted
                        from authorization_api_resource
                        order by path_pattern, http_method
                        """,
                (rs, rowNum) -> new ApiResourceSnapshot(
                        rs.getString("module_name"),
                        rs.getString("http_method"),
                        rs.getString("path_pattern"),
                        rs.getString("resource_code"),
                        rs.getString("permission_code"),
                        rs.getString("access_mode"),
                        rs.getString("handler_class"),
                        rs.getString("handler_method"),
                        rs.getString("description"),
                        rs.getInt("status"),
                        rs.getInt("deleted")))
                .stream()
                .sorted(Comparator.comparing(ApiResourceSnapshot::pathPattern)
                        .thenComparing(ApiResourceSnapshot::httpMethod))
                .toList();
    }

    private void clearRuntimeTables() {
        jdbcTemplate.update("delete from authorization_api_resource");
    }

    private void rebuildTables() {
        jdbcTemplate.execute("drop table if exists authorization_api_resource");
        jdbcTemplate.execute("""
                create table authorization_api_resource (
                    id bigint primary key,
                    module_name varchar(128) not null,
                    http_method varchar(16) not null,
                    path_pattern varchar(512) not null,
                    resource_code varchar(640) not null,
                    permission_code varchar(255),
                    access_mode varchar(32) not null default 'LOGIN',
                    handler_class varchar(512),
                    handler_method varchar(128),
                    description varchar(255),
                    status tinyint not null default 1,
                    create_time timestamp,
                    update_time timestamp,
                    deleted tinyint not null default 0,
                    unique key uk_authorization_api_resource_method_path (module_name, http_method, path_pattern)
                )
                """);
    }

    private record ApiResourceSnapshot(
            String moduleName,
            String httpMethod,
            String pathPattern,
            String resourceCode,
            String permissionCode,
            String accessMode,
            String handlerClass,
            String handlerMethod,
            String description,
            Integer status,
            Integer deleted) {
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            ApiResourceMvcSyncAutoConfiguration.class,
            ApiResourceProviderAutoConfiguration.class
    }, excludeName = {
            "io.mango.authorization.starter.AuthorizationAutoConfiguration",
            "io.mango.authorization.support.autoconfigure.TokenAutoConfiguration",
            "io.mango.authorization.support.autoconfigure.SecurityAutoConfiguration"
    })
    @MapperScan(basePackageClasses = {
            ApiResourceMapper.class
    })
    @EnableConfigurationProperties(ApiResourceSyncProperties.class)
    @Import({
            DataSourceAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class,
            TransactionAutoConfiguration.class,
            MybatisPlusAutoConfiguration.class,
            PersistenceMybatisPlusAutoConfiguration.class,
            ApiResourceServiceImpl.class,
            ApiResourceHandler.class
    })
    static class TestApp {

        @Bean
        ApiResourceApi apiResourceApi(IApiResourceService apiResourceService) {
            return new ApiResourceApi() {
                @Override
                public R<ApiResourceRegisterResultVO> registerApiResources(List<ApiResourceRegisterCommand> resources) {
                    return R.ok(apiResourceService.registerApiResources(resources));
                }

                @Override
                public R<ApiResourceAccessDecisionVO> resolveAccessDecision(ApiResourceAccessDecisionQuery query) {
                    return R.ok(apiResourceService.resolveAccessDecision(query.getHttpMethod(), query.getPath()));
                }

                @Override
                public R<Void> refreshApiResourceCache() {
                    apiResourceService.refreshRuntimeCache();
                    return R.ok();
                }
            };
        }

        @Bean
        ApiAccessResourceDiscoverer apiAccessResourceDiscoverer(
                @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping,
                ApiResourceSyncProperties properties) {
            return new ApiAccessResourceDiscoverer(handlerMapping, new EmptyModuleInfoRegistryProvider(), properties);
        }

        @Bean
        ApiAccessResourceProvider apiAccessResourceProvider(ApiAccessResourceDiscoverer discoverer,
                                                           ApiResourceSyncProperties properties) {
            return new ApiAccessResourceProvider(discoverer, properties);
        }

        @Bean
        TestController testController() {
            return new TestController();
        }

        @Bean
        TypeLevelPublicController typeLevelPublicController() {
            return new TypeLevelPublicController();
        }
    }

    @RestController
    static class TestController {

        @GetMapping("/resource-sync/query")
        String query() {
            return "ok";
        }

        @LoginApi(desc = "Login resource")
        @GetMapping("/resource-sync/login")
        String login() {
            return "ok";
        }

        @ApiAccess(
                mode = ApiResourceAccessMode.PERMISSION,
                permission = "resource-sync:create",
                desc = "Create resource")
        @PostMapping("/resource-sync/create")
        String create() {
            return "ok";
        }

        @PermissionAccess(value = "resource-sync:update", desc = "Update resource")
        @PostMapping("/resource-sync/update")
        String update() {
            return "ok";
        }

        @PublicApi
        @GetMapping("/resource-sync/public")
        String publicResource() {
            return "ok";
        }

        @GetMapping("/resource-sync/inner")
        String inner() {
            return "ok";
        }

        @InternalApi
        @GetMapping("/resource-sync/internal")
        String internal() {
            return "ok";
        }
    }

    @PublicApi
    @RestController
    static class TypeLevelPublicController {

        @GetMapping("/resource-sync/type-public")
        String publicByType() {
            return "ok";
        }
    }

    private static class EmptyModuleInfoRegistryProvider
            implements ObjectProvider<io.mango.infra.module.api.ModuleInfoRegistry> {

        @Override
        public io.mango.infra.module.api.ModuleInfoRegistry getObject(Object... args) {
            return null;
        }

        @Override
        public io.mango.infra.module.api.ModuleInfoRegistry getIfAvailable() {
            return null;
        }

        @Override
        public io.mango.infra.module.api.ModuleInfoRegistry getIfUnique() {
            return null;
        }

        @Override
        public io.mango.infra.module.api.ModuleInfoRegistry getObject() {
            return null;
        }
    }
}
