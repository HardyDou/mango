package io.mango.authorization.resource.sync;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResourceProviderAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ApiResourceProviderAutoConfiguration.class))
            .withBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class, RequestMappingHandlerMapping::new)
            .withBean(TestController.class);

    @Test
    void apiResourceProvider_shouldBeEnabledByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ApiAccessResourceProvider.class);
            assertThat(context).hasSingleBean(ApiAccessResourceDiscoverer.class);
        });
    }

    @Test
    void apiResourceProvider_shouldAllowExplicitDisable() {
        contextRunner
                .withPropertyValues("mango.authorization.resource-sync.resource-provider.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(ApiAccessResourceProvider.class));
    }

    @RestController
    static class TestController {

        @GetMapping("/test")
        String test() {
            return "ok";
        }
    }
}
