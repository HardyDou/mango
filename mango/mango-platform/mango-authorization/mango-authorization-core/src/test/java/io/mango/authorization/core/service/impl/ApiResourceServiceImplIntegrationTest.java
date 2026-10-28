package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.mango.authorization.api.command.ApiResourceManualCommand;
import io.mango.authorization.api.command.ApiResourceRegisterCommand;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.authorization.api.enums.ApiResourceSource;
import io.mango.authorization.api.query.ApiResourceManualQuery;
import io.mango.authorization.api.query.ApiResourcePageQuery;
import io.mango.authorization.api.vo.ApiResourceAccessDecisionVO;
import io.mango.authorization.api.vo.ApiResourceFilterOptionsVO;
import io.mango.authorization.api.vo.ApiResourceRegisterResultVO;
import io.mango.authorization.api.vo.ApiResourceVO;
import io.mango.common.vo.PageResult;
import io.mango.authorization.core.entity.ApiResourceEntity;
import io.mango.authorization.core.mapper.ApiResourceMapper;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
        ApiResourceServiceImplIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:api_resource_service;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false",
        "mybatis-plus.mapper-locations=classpath*:/mapper/**/*.xml"
})
@DisplayName("ApiResourceService 集成测试")
class ApiResourceServiceImplIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ApiResourceMapper apiResourceMapper;

    @Autowired
    private ApiResourceService service;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("drop table if exists authorization_api_resource");
        jdbcTemplate.execute("""
                create table authorization_api_resource (
                    id bigint primary key,
                    module_name varchar(128) not null,
                    source varchar(32) not null default 'FIXED_DECLARATION',
                    manual_override tinyint not null default 0,
                    origin_source varchar(32),
                    origin_access_mode varchar(32),
                    origin_permission_code varchar(255),
                    origin_description varchar(255),
                    domain varchar(128) not null default 'root',
                    http_method varchar(16) not null,
                    path_pattern varchar(512) not null,
                    resource_code varchar(640) not null,
                    permission_code varchar(255),
                    access_mode varchar(32) not null default 'LOGIN',
                    handler_class varchar(512),
                    handler_method varchar(128),
                    description varchar(255),
                    status tinyint not null default 1,
                    tenant_id varchar(64) not null default 'default',
                    create_time timestamp default current_timestamp,
                    update_time timestamp default current_timestamp,
                    deleted tinyint not null default 0
                )
                """);
        AuthorizationTestSchema.ensureCanonicalColumns(jdbcTemplate);
        service.refreshRuntimeCache();
    }

    @Test
    @DisplayName("registerApiResources should disable stale controller paths through real mapper")
    void registerApiResourcesDisablesStaleControllerPathsThroughRealMapper() {
        seedResource(1L, "mango-system", "GET", "/system/area/tree",
                "io.mango.area.core.controller.SysAreaController", 1);
        seedResource(2L, "mango-system", "GET", "/area/tree",
                "io.mango.area.core.controller.SysAreaController", 1);

        ApiResourceRegisterResultVO result = service.registerApiResources(List.of(command("mango-system", "GET",
                "/system/area/tree", "io.mango.area.core.controller.SysAreaController")));

        assertThat(result.scanned()).isEqualTo(1);
        assertThat(result.created()).isZero();
        assertThat(result.updated()).isEqualTo(2);
        assertThat(apiResourceMapper.selectById(1L).getStatus()).isEqualTo(1);
        assertThat(apiResourceMapper.selectById(2L).getStatus()).isZero();
    }

    @Test
    @DisplayName("registerApiResources should revive a soft-deleted resource instead of failing on unique key")
    void registerApiResourcesRevivesSoftDeletedResource() {
        seedResource(11L, "mango-file-preview", "GET", "/file-preview/files/preview-entry",
                "io.mango.file.preview.controller.FilePreviewController", 1);
        jdbcTemplate.update("update authorization_api_resource set deleted = 1 where id = ?", 11L);

        ApiResourceRegisterResultVO result = service.registerApiResources(List.of(command("mango-file-preview", "GET",
                "/file-preview/files/preview-entry", "io.mango.file.preview.controller.FilePreviewController")));
        assertThat(result.created()).isZero();
        assertThat(result.updated()).isZero();
        Integer rowCount = jdbcTemplate.queryForObject(
                "select count(*) from authorization_api_resource where module_name = ? and http_method = ? and path_pattern = ?",
                Integer.class, "mango-file-preview", "GET", "/file-preview/files/preview-entry");
        assertThat(rowCount).isEqualTo(1);
        Integer deleted = jdbcTemplate.queryForObject(
                "select deleted from authorization_api_resource where id = ?", Integer.class, 11L);
        assertThat(deleted).isZero();
        assertThat(apiResourceMapper.selectById(11L).getStatus()).isEqualTo(1);
    }

    @Test
    @DisplayName("registerApiResources should keep working when both active and soft-deleted rows share a route")
    void registerApiResourcesPrefersActiveRowWhenRouteIsDuplicated() {
        seedResource(21L, "mango-file-preview", "GET", "/file-preview/files/preview-content",
                "io.mango.file.preview.controller.FilePreviewController", 1);
        jdbcTemplate.update("""
                insert into authorization_api_resource
                (id, module_name, source, domain, http_method, path_pattern, resource_code, permission_code,
                 access_mode, handler_class, handler_method, status, deleted)
                values (22, 'mango-file-preview', 'FIXED_DECLARATION', 'file-preview', 'GET',
                        '/file-preview/files/preview-content', 'GET:/file-preview/files/preview-content', null,
                        'LOGIN', 'io.mango.file.preview.controller.FilePreviewController', 'content', 0, 1)
                """);

        service.registerApiResources(List.of(command("mango-file-preview", "GET",
                "/file-preview/files/preview-content", "io.mango.file.preview.controller.FilePreviewController")));

        Integer activeCount = jdbcTemplate.queryForObject("""
                select count(*) from authorization_api_resource
                where module_name = 'mango-file-preview'
                  and path_pattern = '/file-preview/files/preview-content'
                  and deleted = 0
                """, Integer.class);
        assertThat(activeCount).isEqualTo(1);
        ApiResourceAccessDecisionVO decision = service.resolveAccessDecision("GET", "/file-preview/files/preview-content");
        assertThat(decision.matched()).isTrue();
    }

    @Test
    @DisplayName("registerApiResources should reactivate a disabled public resource")
    void registerApiResourcesReactivatesDisabledPublicResource() {
        seedResource(5L, "mango-auth", "POST", "/auth/login-institutions",
                "io.mango.auth.starter.controller.AuthController", 0,
                null, ApiResourceAccessMode.PUBLIC);

        ApiResourceRegisterCommand command = command("mango-auth", "POST", "/auth/login-institutions",
                "io.mango.auth.starter.controller.AuthController");
        command.setAccessMode(ApiResourceAccessMode.PUBLIC);
        command.setHandlerMethod("loginInstitutions");

        service.registerApiResources(List.of(command));

        ApiResourceEntity resource = apiResourceMapper.selectById(5L);
        assertThat(resource.getStatus()).isEqualTo(1);
        assertThat(resource.getAccessMode()).isEqualTo(ApiResourceAccessMode.PUBLIC.name());
        ApiResourceAccessDecisionVO decision = service.resolveAccessDecision("POST", "/auth/login-institutions");
        assertThat(decision.matched()).isTrue();
        assertThat(decision.accessMode()).isEqualTo(ApiResourceAccessMode.PUBLIC);
    }

    @Test
    @DisplayName("resolveAccessDecision should match persisted active resources")
    void resolveAccessDecisionMatchesPersistedActiveResources() {
        seedResource(10L, "mango-workflow", "GET", "/workflow/definitions/**",
                "io.mango.workflow.DefinitionController", 1, "workflow:definition:list",
                ApiResourceAccessMode.PERMISSION);

        ApiResourceAccessDecisionVO decision = service.resolveAccessDecision("GET", "/workflow/definitions/100");

        assertThat(decision.matched()).isTrue();
        assertThat(decision.accessMode()).isEqualTo(ApiResourceAccessMode.PERMISSION);
        assertThat(decision.permissionCode()).isEqualTo("workflow:definition:list");
    }

    @Test
    @DisplayName("registerApiResources should disable duplicate route from stale module")
    void registerApiResourcesDisablesDuplicateRouteFromStaleModule() {
        seedResource(20L, "mango-admin-app", "POST", "/notice/site/my/messages/read-all",
                "io.mango.notice.starter.controller.NoticeController", 1, "notice:site:edit",
                ApiResourceAccessMode.PERMISSION);

        ApiResourceRegisterCommand command = command("mango-notice", "POST",
                "/notice/site/my/messages/read-all",
                "io.mango.notice.starter.controller.NoticeController");
        command.setHandlerMethod("markAllSiteMessagesRead");
        ApiResourceRegisterResultVO result = service.registerApiResources(List.of(command));

        ApiResourceAccessDecisionVO decision = service.resolveAccessDecision("POST",
                "/notice/site/my/messages/read-all");

        assertThat(result.created()).isEqualTo(1);
        assertThat(result.updated()).isEqualTo(1);
        assertThat(apiResourceMapper.selectById(20L).getStatus()).isZero();
        assertThat(apiResourceMapper.selectList(null))
                .filteredOn(resource -> "mango-notice".equals(resource.getModuleName()))
                .singleElement()
                .extracting(ApiResourceEntity::getTenantId)
                .isEqualTo("default");
        assertThat(decision.matched()).isTrue();
        assertThat(decision.accessMode()).isEqualTo(ApiResourceAccessMode.LOGIN);
        assertThat(decision.permissionCode()).isNull();
    }

    @ParameterizedTest
    @CsvSource({
            "POST,/notice/site/my/messages/read-all,notice:site:edit,LOGIN",
            "GET,/notice/site/my/messages,notice:site:view,LOGIN",
            "GET,/notice/site/my/unread-count,notice:site:view,LOGIN",
            "GET,/file/files/detail,file:files:query,LOGIN",
            "GET,/file/files/preview,file:files:query,LOGIN",
            "GET,/file/files/download,file:files:download,LOGIN",
            "GET,/file/settings,file:settings:query,LOGIN",
            "GET,/file-preview/files/preview-link,file:files:query,LOGIN",
            "GET,/file-preview/files/preview,file:files:query,LOGIN",
            "GET,/file-preview/files/preview-entry,file:files:query,PUBLIC",
            "GET,/file-preview/sources/{token},file:files:download,PUBLIC"
    })
    @DisplayName("registerApiResources should keep basic login and token endpoints from stale permission routes")
    void registerApiResourcesKeepsBasicEndpointsFromStalePermissionRoutes(
            String httpMethod,
            String path,
            String stalePermission,
            ApiResourceAccessMode expectedMode) {
        seedResource(40L, "mango-admin-app", httpMethod, path,
                "io.mango.legacy.LegacyController", 1, stalePermission, ApiResourceAccessMode.PERMISSION);

        ApiResourceRegisterCommand command = command("mango-platform", httpMethod, path,
                "io.mango.platform.CurrentController");
        command.setHandlerMethod("currentEndpoint");
        command.setAccessMode(expectedMode);

        service.registerApiResources(List.of(command));
        ApiResourceAccessDecisionVO decision = service.resolveAccessDecision(httpMethod, path);

        assertThat(apiResourceMapper.selectById(40L).getStatus()).isZero();
        assertThat(decision.matched()).isTrue();
        assertThat(decision.accessMode()).isEqualTo(expectedMode);
        assertThat(decision.permissionCode()).isNull();
    }

    @Test
    @DisplayName("resolveAccessDecision should prefer exact login route over wildcard permission route")
    void resolveAccessDecisionPrefersExactLoginRouteOverWildcardPermissionRoute() {
        seedResource(30L, "mango-notice", "POST", "/notice/site/my/messages/**",
                "configuration", 1, "notice:site:edit", ApiResourceAccessMode.PERMISSION);
        seedResource(31L, "mango-notice", "POST", "/notice/site/my/messages/read-all",
                "io.mango.notice.starter.controller.NoticeController", 1, null, ApiResourceAccessMode.LOGIN);

        ApiResourceAccessDecisionVO decision = service.resolveAccessDecision("POST",
                "/notice/site/my/messages/read-all");

        assertThat(decision.matched()).isTrue();
        assertThat(decision.accessMode()).isEqualTo(ApiResourceAccessMode.LOGIN);
        assertThat(decision.permissionCode()).isNull();
    }

    @Test
    @DisplayName("fixed declaration should keep active and disable controller scan on the same route")
    void fixedDeclarationShouldKeepActiveAndDisableControllerScan() {
        ApiResourceRegisterCommand scanned = command("mango-file-preview", "GET", "/favicon.ico",
                "cn.keking.web.controller.OnlinePreviewController");
        scanned.setSource(ApiResourceSource.CONTROLLER_SCAN);
        service.registerApiResources(List.of(scanned));

        ApiResourceRegisterCommand fixed = command("mango-infra-doc", "GET", "/favicon.ico", "configuration");
        fixed.setSource(ApiResourceSource.FIXED_DECLARATION);
        ApiResourceRegisterResultVO result = service.registerApiResources(List.of(fixed));

        assertThat(apiResourceRows("mango-file-preview", "GET", "/favicon.ico")).singleElement()
                .extracting(ApiResourceEntity::getStatus).isEqualTo(0);
        assertThat(apiResourceRows("mango-infra-doc", "GET", "/favicon.ico")).singleElement()
                .satisfies(resource -> {
                    assertThat(resource.getStatus()).isEqualTo(1);
                    assertThat(resource.getSource()).isEqualTo(ApiResourceSource.FIXED_DECLARATION.name());
                });
        assertThat(result.created()).isEqualTo(1);

        ApiResourceAccessDecisionVO decision = service.resolveAccessDecision("GET", "/favicon.ico");
        assertThat(decision.matched()).isTrue();
        assertThat(decision.accessMode()).isEqualTo(ApiResourceAccessMode.LOGIN);
    }

    @Test
    @DisplayName("controller scan should not create a duplicate when a fixed declaration already exists")
    void controllerScanShouldNotDuplicateExistingFixedDeclaration() {
        ApiResourceRegisterCommand fixed = command("mango-infra-doc", "GET", "/favicon.ico", "configuration");
        fixed.setSource(ApiResourceSource.FIXED_DECLARATION);
        service.registerApiResources(List.of(fixed));

        ApiResourceRegisterCommand scanned = command("mango-file-preview", "GET", "/favicon.ico",
                "cn.keking.web.controller.OnlinePreviewController");
        scanned.setSource(ApiResourceSource.CONTROLLER_SCAN);
        service.registerApiResources(List.of(scanned));

        assertThat(apiResourceMapper.selectList(null))
                .filteredOn(resource -> "/favicon.ico".equals(resource.getPathPattern()))
                .singleElement()
                .satisfies(resource -> {
                    assertThat(resource.getModuleName()).isEqualTo("mango-infra-doc");
                    assertThat(resource.getStatus()).isEqualTo(1);
                });
    }

    @Test
    @DisplayName("manual takeover marks the same row without touching module or permission code and release restores it")
    void manualTakeoverMarksSameRowAndReleaseRestoresIt() {
        ApiResourceRegisterCommand scanned = command("mango-guarantee", "POST", "/guarantee/open/apply",
                "io.mango.guarantee.OpenController");
        scanned.setSource(ApiResourceSource.CONTROLLER_SCAN);
        scanned.setAccessMode(ApiResourceAccessMode.PERMISSION);
        scanned.setPermissionCode("guarantee:apply:create");
        service.registerApiResources(List.of(scanned));
        assertThat(service.resolveAccessDecision("POST", "/guarantee/open/apply").source())
                .isEqualTo(ApiResourceSource.CONTROLLER_SCAN);

        ApiResourceManualCommand manual = new ApiResourceManualCommand();
        manual.setModuleName("mango-guarantee");
        manual.setHttpMethod("post");
        manual.setPathPattern("/guarantee/open/apply");
        manual.setAccessMode(ApiResourceAccessMode.PUBLIC);
        manual.setDescription("人工接管为匿名开放");
        service.saveManualApiResource(manual);

        assertThat(apiResourceMapper.selectList(null))
                .filteredOn(resource -> "/guarantee/open/apply".equals(resource.getPathPattern()))
                .as("接管只允许一条记录，不再复制 manual-control 行")
                .singleElement()
                .satisfies(resource -> {
                    assertThat(resource.getModuleName()).as("模块归属保持不变").isEqualTo("mango-guarantee");
                    assertThat(resource.getManualOverride()).isEqualTo(1);
                    assertThat(resource.getAccessMode()).isEqualTo(ApiResourceAccessMode.PUBLIC.name());
                    assertThat(resource.getPermissionCode()).as("权限标识不被清空").isEqualTo("guarantee:apply:create");
                    assertThat(resource.getOriginAccessMode()).isEqualTo(ApiResourceAccessMode.PERMISSION.name());
                    assertThat(resource.getOriginPermissionCode()).isEqualTo("guarantee:apply:create");
                    assertThat(resource.getStatus()).isEqualTo(1);
                });
        assertThat(service.resolveAccessDecision("POST", "/guarantee/open/apply"))
                .satisfies(decision -> {
                    assertThat(decision.accessMode()).isEqualTo(ApiResourceAccessMode.PUBLIC);
                    assertThat(decision.manualOverride()).isTrue();
                });

        ApiResourceRegisterCommand rescan = command("mango-guarantee", "POST", "/guarantee/open/apply",
                "io.mango.guarantee.OpenController");
        rescan.setSource(ApiResourceSource.CONTROLLER_SCAN);
        rescan.setAccessMode(ApiResourceAccessMode.PERMISSION);
        rescan.setPermissionCode("guarantee:apply:create");
        service.registerApiResources(List.of(rescan));

        assertThat(service.resolveAccessDecision("POST", "/guarantee/open/apply").accessMode())
                .as("同步不得覆盖人工接管")
                .isEqualTo(ApiResourceAccessMode.PUBLIC);

        ApiResourceManualQuery release = new ApiResourceManualQuery();
        release.setModuleName("mango-guarantee");
        release.setHttpMethod("POST");
        release.setPathPattern("/guarantee/open/apply");
        service.releaseManualApiResource(release);

        assertThat(apiResourceMapper.selectList(null))
                .filteredOn(resource -> "/guarantee/open/apply".equals(resource.getPathPattern()))
                .singleElement()
                .satisfies(resource -> {
                    assertThat(resource.getManualOverride()).isZero();
                    assertThat(resource.getSource()).isEqualTo(ApiResourceSource.CONTROLLER_SCAN.name());
                    assertThat(resource.getAccessMode()).isEqualTo(ApiResourceAccessMode.PERMISSION.name());
                    assertThat(resource.getPermissionCode()).isEqualTo("guarantee:apply:create");
                    assertThat(resource.getOriginSource()).isNull();
                });
        assertThat(service.resolveAccessDecision("POST", "/guarantee/open/apply"))
                .satisfies(decision -> {
                    assertThat(decision.accessMode()).isEqualTo(ApiResourceAccessMode.PERMISSION);
                    assertThat(decision.permissionCode()).isEqualTo("guarantee:apply:create");
                    assertThat(decision.manualOverride()).isFalse();
                });
    }

    @Test
    @DisplayName("release deletes a legacy manual row that has no declared origin")
    void releaseDeletesLegacyManualRowWithoutOrigin() {
        jdbcTemplate.update("""
                        insert into authorization_api_resource
                        (id, module_name, source, manual_override, domain, http_method, path_pattern, resource_code,
                         permission_code, access_mode, handler_class, handler_method, description, status, deleted)
                        values (990113000000000021, 'manual-control', 'MANUAL', 1, 'openapi', 'POST', '/openapi/callback',
                                'POST:/openapi/callback', null, 'PUBLIC', 'manual', 'migrated', '历史接管', 1, 0)
                        """);

        service.releaseManualApiResource(manualQuery("manual-control", "POST", "/openapi/callback"));

        assertThat(apiResourceMapper.selectList(null))
                .as("没有声明来源的接管记录解除后直接删除，不留禁用垃圾行")
                .noneMatch(resource -> "/openapi/callback".equals(resource.getPathPattern()));
    }

    @Test
    @DisplayName("page query should filter by module, domain, source and keyword")
    void pageQueryShouldFilterAndExposeDomainAndSource() {
        seedResource(60L, "mango-auth", "POST", "/auth/login",
                "io.mango.auth.starter.controller.AuthController", 1, null, ApiResourceAccessMode.PUBLIC);
        seedResource(61L, "mango-system", "GET", "/system/users",
                "io.mango.system.starter.controller.SysUserController", 1, "system:user:list",
                ApiResourceAccessMode.PERMISSION);

        ApiResourcePageQuery query = new ApiResourcePageQuery();
        query.setModuleName("mango-auth");
        query.setDomain("auth");
        query.setSource(ApiResourceSource.FIXED_DECLARATION);
        query.setKeyword("login");

        PageResult<ApiResourceVO> page = service.pageApiResources(query);

        assertThat(page.getTotal()).isEqualTo(1);
        assertThat(page.getList()).singleElement().satisfies(resource -> {
            assertThat(resource.getModuleName()).isEqualTo("mango-auth");
            assertThat(resource.getDomain()).isEqualTo("auth");
            assertThat(resource.getSource()).isEqualTo(ApiResourceSource.FIXED_DECLARATION);
            assertThat(resource.getPathPattern()).isEqualTo("/auth/login");
        });

        ApiResourceFilterOptionsVO options = service.listApiResourceFilterOptions();
        assertThat(options.getModules()).contains("mango-auth", "mango-system");
        assertThat(options.getDomains()).contains("auth", "system");
    }

    private List<ApiResourceEntity> apiResourceRows(String moduleName, String httpMethod, String pathPattern) {
        return apiResourceMapper.selectList(null).stream()
                .filter(resource -> moduleName.equals(resource.getModuleName()))
                .filter(resource -> httpMethod.equals(resource.getHttpMethod()))
                .filter(resource -> pathPattern.equals(resource.getPathPattern()))
                .toList();
    }

    private ApiResourceRegisterCommand command(String moduleName, String httpMethod, String path, String handlerClass) {
        ApiResourceRegisterCommand command = new ApiResourceRegisterCommand();
        command.setModuleName(moduleName);
        command.setHttpMethod(httpMethod);
        command.setPathPattern(path);
        command.setHandlerClass(handlerClass);
        command.setHandlerMethod("tree");
        command.setAccessMode(ApiResourceAccessMode.LOGIN);
        command.setResourceCode(httpMethod + ":" + path);
        return command;
    }

    private void seedResource(Long id,
                              String moduleName,
                              String httpMethod,
                              String path,
                              String handlerClass,
                              int status) {
        seedResource(id, moduleName, httpMethod, path, handlerClass, status,
                httpMethod + ":" + path, ApiResourceAccessMode.LOGIN);
    }

    private void seedResource(Long id,
                              String moduleName,
                              String httpMethod,
                              String path,
                              String handlerClass,
                              int status,
                              String permissionCode,
                              ApiResourceAccessMode accessMode) {
        jdbcTemplate.update("""
                        insert into authorization_api_resource
                        (id, module_name, source, domain, http_method, path_pattern, resource_code, permission_code,
                         access_mode, handler_class, handler_method, status, deleted)
                        values (?, ?, 'FIXED_DECLARATION', ?, ?, ?, ?, ?, ?, ?, 'tree', ?, 0)
                        """,
                id, moduleName, domain(path), httpMethod, path, httpMethod + ":" + path, permissionCode,
                accessMode.name(), handlerClass, status);
    }

    private ApiResourceEntity manualEntity(String moduleName, String httpMethod, String path) {
        ApiResourceEntity entity = new ApiResourceEntity();
        entity.setTenantId("default");
        entity.setModuleName(moduleName);
        entity.setDomain(domain(path));
        entity.setSource(ApiResourceSource.CONTROLLER_SCAN.name());
        entity.setHttpMethod(httpMethod);
        entity.setPathPattern(path);
        entity.setResourceCode(httpMethod + ":" + path);
        entity.setAccessMode(ApiResourceAccessMode.LOGIN.name());
        entity.setHandlerClass("io.mango.openapi.CallbackController");
        entity.setHandlerMethod("callback");
        entity.setDescription("开放回调");
        entity.setStatus(1);
        entity.setDeleted(0);
        return entity;
    }

    private ApiResourceManualQuery manualQuery(String moduleName, String httpMethod, String path) {
        ApiResourceManualQuery query = new ApiResourceManualQuery();
        query.setModuleName(moduleName);
        query.setHttpMethod(httpMethod);
        query.setPathPattern(path);
        return query;
    }

    private void seedManualResource(Long id,
                                    String moduleName,
                                    String httpMethod,
                                    String path,
                                    String handlerClass,
                                    int status,
                                    String permissionCode,
                                    ApiResourceAccessMode accessMode) {
        jdbcTemplate.update("""
                        insert into authorization_api_resource
                        (id, module_name, source, domain, http_method, path_pattern, resource_code, permission_code,
                         access_mode, handler_class, handler_method, status, deleted)
                        values (?, ?, 'MANUAL', ?, ?, ?, ?, ?, ?, ?, 'manual', ?, 0)
                        """,
                id, moduleName, domain(path), httpMethod, path, httpMethod + ":" + path, permissionCode,
                accessMode.name(), handlerClass, status);
    }

    private String domain(String path) {
        if (!path.startsWith("/")) {
            return "root";
        }
        String remaining = path.substring(1);
        int separator = remaining.indexOf('/');
        String segment = separator < 0 ? remaining : remaining.substring(0, separator);
        if (segment.isBlank() || segment.contains("*") || segment.contains("{") || segment.contains(".")) {
            return "root";
        }
        return segment;
    }

    @Configuration
    @MapperScan(basePackageClasses = ApiResourceMapper.class)
    @Import(ApiResourceService.class)
    static class TestConfig {
    }
}
