package io.mango.authorization.resource.sync;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.annotation.ApiResourceIgnore;
import io.mango.authorization.api.command.ApiResourceRegisterCommand;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.authorization.api.enums.ApiResourceSource;
import io.swagger.v3.oas.annotations.Operation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("API resource discoverer de-duplication tests")
class ApiAccessResourceDiscovererTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class, RequestMappingHandlerMapping::new)
            .withBean(ScanController.class)
            .withBean(IgnoredTypeController.class)
            .withBean(PartiallyIgnoredController.class);

    @Test
    @DisplayName("controller scan should mark its own declaration source")
    void controllerScanShouldMarkControllerScanSource() {
        List<ApiResourceRegisterCommand> resources = discover();

        ApiResourceRegisterCommand scanned = find(resources, "GET", "/discover/plain");
        assertThat(scanned.getSource()).isEqualTo(ApiResourceSource.CONTROLLER_SCAN);
    }

    @Test
    @DisplayName("fixed declaration should override controller scan on the same method and path")
    void fixedDeclarationShouldOverrideControllerScan() {
        ApiResourceSyncProperties properties = new ApiResourceSyncProperties();
        properties.setIncludePackages("io.mango.authorization.resource.sync");
        properties.setExcludePaths("/error");
        properties.setResources(List.of(resource("GET", "/discover/fixed")));

        List<ApiResourceRegisterCommand> resources = discover(properties);

        assertThat(resources)
                .filteredOn(resource -> "GET".equals(resource.getHttpMethod()))
                .filteredOn(resource -> "/discover/fixed".equals(resource.getPathPattern()))
                .singleElement()
                .satisfies(resource -> {
                    assertThat(resource.getSource()).isEqualTo(ApiResourceSource.FIXED_DECLARATION);
                    assertThat(resource.getModuleName()).isEqualTo("mango-doc");
                    assertThat(resource.getHandlerClass()).isEqualTo("configuration");
                    assertThat(resource.getAccessMode()).isEqualTo(ApiResourceAccessMode.PUBLIC);
                });
        assertThat(find(resources, "POST", "/discover/fixed").getSource())
                .isEqualTo(ApiResourceSource.CONTROLLER_SCAN);
    }

    @Test
    @DisplayName("class level ApiResourceIgnore should skip every method of the controller")
    void classLevelIgnoreShouldSkipAllMethods() {
        List<ApiResourceRegisterCommand> resources = discover();

        assertThat(resources)
                .noneMatch(resource -> resource.getPathPattern().startsWith("/discover/ignored-type"));
    }

    @Test
    @DisplayName("method level ApiResourceIgnore should skip only the annotated method")
    void methodLevelIgnoreShouldSkipAnnotatedMethod() {
        List<ApiResourceRegisterCommand> resources = discover();

        assertThat(resources)
                .noneMatch(resource -> "/discover/ignored-method".equals(resource.getPathPattern()));
        assertThat(find(resources, "GET", "/discover/kept-method").getSource())
                .isEqualTo(ApiResourceSource.CONTROLLER_SCAN);
    }

    @Test
    @DisplayName("controller scan should use OpenAPI operation summary as description")
    void controllerScanShouldUseOperationSummaryAsDescription() {
        List<ApiResourceRegisterCommand> resources = discover();

        assertThat(find(resources, "GET", "/discover/described").getDescription())
                .isEqualTo("查询接口说明");
    }

    private List<ApiResourceRegisterCommand> discover() {
        return discover(new ApiResourceSyncProperties());
    }

    private List<ApiResourceRegisterCommand> discover(ApiResourceSyncProperties properties) {
        properties.setIncludePackages("io.mango.authorization.resource.sync");
        if (properties.getExcludePaths() == null) {
            properties.setExcludePaths("/error");
        }
        java.util.concurrent.atomic.AtomicReference<List<ApiResourceRegisterCommand>> discovered =
                new java.util.concurrent.atomic.AtomicReference<>();
        contextRunner.run(context -> {
            RequestMappingHandlerMapping handlerMapping = context.getBean(
                    "requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
            ObjectProvider<io.mango.infra.module.api.ModuleInfoRegistry> registryProvider =
                    context.getBeanProvider(io.mango.infra.module.api.ModuleInfoRegistry.class);
            discovered.set(new ApiAccessResourceDiscoverer(handlerMapping, registryProvider, properties).discover());
        });
        return discovered.get();
    }

    private ApiResourceRegisterCommand find(List<ApiResourceRegisterCommand> resources,
                                            String httpMethod,
                                            String pathPattern) {
        return resources.stream()
                .filter(resource -> httpMethod.equals(resource.getHttpMethod()))
                .filter(resource -> pathPattern.equals(resource.getPathPattern()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing API resource " + httpMethod + " " + pathPattern));
    }

    private ApiResourceSyncProperties.Resource resource(String httpMethod, String pathPattern) {
        ApiResourceSyncProperties.Resource resource = new ApiResourceSyncProperties.Resource();
        resource.setModuleName("mango-doc");
        resource.setHttpMethod(httpMethod);
        resource.setPathPattern(pathPattern);
        resource.setAccessMode(ApiResourceAccessMode.PUBLIC);
        resource.setDescription("Document asset");
        return resource;
    }

    @RestController
    @RequestMapping("/discover")
    static class ScanController {

        @GetMapping("/plain")
        String plain() {
            return "ok";
        }

        @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "Fixed candidate")
        @GetMapping("/fixed")
        String fixedGet() {
            return "ok";
        }

        @Operation(summary = "查询接口说明")
        @GetMapping("/described")
        String described() {
            return "ok";
        }

        @PostMapping("/fixed")
        String fixedPost() {
            return "ok";
        }
    }

    @ApiResourceIgnore
    @RestController
    @RequestMapping("/discover/ignored-type")
    static class IgnoredTypeController {

        @GetMapping("/value")
        String value() {
            return "ok";
        }
    }

    @RestController
    @RequestMapping("/discover")
    static class PartiallyIgnoredController {

        @ApiResourceIgnore
        @GetMapping("/ignored-method")
        String ignoredMethod() {
            return "ok";
        }

        @GetMapping("/kept-method")
        String keptMethod() {
            return "ok";
        }
    }
}
