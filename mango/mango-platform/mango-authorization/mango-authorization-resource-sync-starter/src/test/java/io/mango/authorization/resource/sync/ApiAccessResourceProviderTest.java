package io.mango.authorization.resource.sync;

import io.mango.authorization.api.command.ApiResourceRegisterCommand;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.authorization.api.enums.ApiResourceSource;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.support.ResourceTypes;
import io.mango.resource.support.declaration.ResourceDeclarationLoader;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceField;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiAccessResourceProviderTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class, RequestMappingHandlerMapping::new)
            .withBean(FixedPathController.class);

    @Test
    void provideShouldSkipControllerPathsAlreadyDeclaredByFixedApiResource() {
        ResourceDeclarationLoader loader = mock(ResourceDeclarationLoader.class);
        when(loader.load()).thenReturn(List.of(fixedDeclaration("/fixed/shared")));

        java.util.concurrent.atomic.AtomicReference<List<ResourceDeclaration>> result =
                new java.util.concurrent.atomic.AtomicReference<>();
        contextRunner.run(context -> {
            RequestMappingHandlerMapping handlerMapping = context.getBean(
                    "requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
            ObjectProvider<io.mango.infra.module.api.ModuleInfoRegistry> registryProvider =
                    context.getBeanProvider(io.mango.infra.module.api.ModuleInfoRegistry.class);
            ApiResourceSyncProperties properties = new ApiResourceSyncProperties();
            properties.setIncludePackages("io.mango.authorization.resource.sync");
            properties.setExcludePaths("/error");
            ApiAccessResourceDiscoverer discoverer =
                    new ApiAccessResourceDiscoverer(handlerMapping, registryProvider, properties);
            ApiAccessResourceProvider provider = new ApiAccessResourceProvider(
                    discoverer, properties, new ApiResourceDeclarationConverter(), loader);
            result.set(provider.provide());
        });
        List<ResourceDeclaration> declarations = result.get();

        assertThat(declarations)
                .noneMatch(declaration -> "/fixed/shared".equals(field(declaration, "pathPattern"))
                        && ApiResourceSource.CONTROLLER_SCAN.name()
                        .equals(field(declaration, "source")));
        assertThat(declarations)
                .anyMatch(declaration -> "/fixed/scanned".equals(field(declaration, "pathPattern"))
                        && ApiResourceSource.CONTROLLER_SCAN.name()
                        .equals(field(declaration, "source")));
    }

    private ResourceDeclaration fixedDeclaration(String path) {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId("123");
        declaration.setVersion(1);
        declaration.setResourceType(ResourceTypes.API_RESOURCE);
        declaration.setModuleCode("fixed");
        declaration.setBizKey("api.fixed.GET." + path.replace('/', '.'));
        declaration.setTargetModule("authorization");
        declaration.putField("moduleName", field("unknown-module"));
        declaration.putField("httpMethod", field("GET"));
        declaration.putField("pathPattern", field(path));
        return declaration;
    }

    private ResourceField field(String value) {
        ResourceField field = new ResourceField();
        field.setType(ResourceFieldType.STRING);
        field.setValue(value);
        return field;
    }

    private String field(ResourceDeclaration declaration, String name) {
        ResourceField field = declaration.getFields().get(name);
        return field == null || field.getValue() == null ? null : String.valueOf(field.getValue());
    }

    @RestController
    static class FixedPathController {

        @GetMapping("/fixed/shared")
        String shared() {
            return "ok";
        }

        @GetMapping("/fixed/scanned")
        String scanned() {
            return "ok";
        }
    }
}
