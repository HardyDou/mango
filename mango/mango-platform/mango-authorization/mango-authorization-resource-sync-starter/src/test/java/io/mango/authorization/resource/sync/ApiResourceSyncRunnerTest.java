package io.mango.authorization.resource.sync;

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
import io.mango.common.result.R;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest(
        classes = ApiResourceSyncRunnerTest.TestApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(properties = {
        "spring.application.name=resource-sync-test",
        "mango.persistence.flyway.enabled=false",
        "mango.authorization.resource-sync.module-name=mango-resource-sync-test",
        "mango.authorization.resource-sync.legacy-writer-enabled=true",
        "mango.authorization.resource-sync.mode=write",
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
                + "io.mango.authorization.starter.autoconfigure.TokenAutoConfiguration,"
                + "io.mango.authorization.starter.autoconfigure.SecurityAutoConfiguration"
})
@DisplayName("API resource sync runner tests")
class ApiResourceSyncRunnerTest {

    @Test
    @DisplayName("should register all controller endpoints from ApiAccess declarations")
    void shouldRegisterControllerEndpoints() {
        List<ApiResourceRegisterCommand> resources = TestRegistryApi.RESOURCES;

        ApiResourceRegisterCommand query = find(resources, "GET", "/resource-sync/query");
        assertEquals("mango-resource-sync-test", query.getModuleName());
        assertEquals("GET:/resource-sync/query", query.getResourceCode());
        assertEquals(ApiResourceAccessMode.LOGIN, query.getAccessMode());
        assertNull(query.getPermissionCode());

        ApiResourceRegisterCommand explicitLogin = find(resources, "GET", "/resource-sync/login");
        assertEquals("GET:/resource-sync/login", explicitLogin.getResourceCode());
        assertEquals(ApiResourceAccessMode.LOGIN, explicitLogin.getAccessMode());
        assertEquals("Login resource", explicitLogin.getDescription());

        ApiResourceRegisterCommand create = find(resources, "POST", "/resource-sync/create");
        assertEquals("resource-sync:create", create.getResourceCode());
        assertEquals("resource-sync:create", create.getPermissionCode());
        assertEquals(ApiResourceAccessMode.PERMISSION, create.getAccessMode());
        assertEquals("Create resource", create.getDescription());

        ApiResourceRegisterCommand update = find(resources, "POST", "/resource-sync/update");
        assertEquals("resource-sync:update", update.getResourceCode());
        assertEquals("resource-sync:update", update.getPermissionCode());
        assertEquals(ApiResourceAccessMode.PERMISSION, update.getAccessMode());
        assertEquals("Update resource", update.getDescription());

        ApiResourceRegisterCommand publicResource = find(resources, "GET", "/resource-sync/public");
        assertEquals("GET:/resource-sync/public", publicResource.getResourceCode());
        assertEquals(ApiResourceAccessMode.PUBLIC, publicResource.getAccessMode());
        assertNull(publicResource.getPermissionCode());

        ApiResourceRegisterCommand typeLevelPublicResource = find(resources, "GET", "/resource-sync/type-public");
        assertEquals("GET:/resource-sync/type-public", typeLevelPublicResource.getResourceCode());
        assertEquals(ApiResourceAccessMode.PUBLIC, typeLevelPublicResource.getAccessMode());
        assertNull(typeLevelPublicResource.getPermissionCode());

        ApiResourceRegisterCommand configuredPublicResource = find(resources, "GET", "/swagger-ui/**");
        assertEquals("mango-doc", configuredPublicResource.getModuleName());
        assertEquals("GET:/swagger-ui/**", configuredPublicResource.getResourceCode());
        assertEquals(ApiResourceAccessMode.PUBLIC, configuredPublicResource.getAccessMode());
        assertEquals("Swagger UI", configuredPublicResource.getDescription());
        assertNull(configuredPublicResource.getPermissionCode());

        ApiResourceRegisterCommand inner = find(resources, "GET", "/resource-sync/inner");
        assertEquals("GET:/resource-sync/inner", inner.getResourceCode());
        assertEquals(ApiResourceAccessMode.LOGIN, inner.getAccessMode());
        assertNull(inner.getPermissionCode());

        ApiResourceRegisterCommand internal = find(resources, "GET", "/resource-sync/internal");
        assertEquals("GET:/resource-sync/internal", internal.getResourceCode());
        assertEquals(ApiResourceAccessMode.INTERNAL, internal.getAccessMode());
        assertNull(internal.getPermissionCode());
    }

    private ApiResourceRegisterCommand find(List<ApiResourceRegisterCommand> resources, String method, String path) {
        return resources.stream()
                .filter(resource -> method.equals(resource.getHttpMethod()))
                .filter(resource -> path.equals(resource.getPathPattern()))
                .findFirst()
                .orElseThrow();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApp {

        @Bean
        TestRegistryApi testRegistryApi() {
            TestRegistryApi.RESOURCES.clear();
            return new TestRegistryApi();
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

    static class TestRegistryApi implements ApiResourceApi {

        static final List<ApiResourceRegisterCommand> RESOURCES = new ArrayList<>();

        @Override
        public R<ApiResourceRegisterResultVO> registerApiResources(List<ApiResourceRegisterCommand> resources) {
            RESOURCES.addAll(resources);
            return R.ok(new ApiResourceRegisterResultVO(resources.size(), resources.size(), 0));
        }

        @Override
        public R<ApiResourceAccessDecisionVO> resolveAccessDecision(ApiResourceAccessDecisionQuery query) {
            return R.ok(ApiResourceAccessDecisionVO.unmatched(ApiResourceAccessMode.LOGIN));
        }

        @Override
        public R<Void> refreshApiResourceCache() {
            return R.ok();
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
}
