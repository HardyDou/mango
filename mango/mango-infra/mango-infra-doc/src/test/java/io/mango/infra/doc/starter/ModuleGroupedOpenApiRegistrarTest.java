package io.mango.infra.doc.starter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleGroupedOpenApiRegistrarTest {

    @Test
    void configuredModulesWithPreviouslyCollidingNamesShouldBothRegister() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                "mango.module.module-service.modules.sales-api.module-path", "/sales-api",
                "mango.module.module-service.modules.sales_api.module-path", "/sales-alias")));
        ModuleGroupedOpenApiRegistrar registrar = new ModuleGroupedOpenApiRegistrar();
        registrar.setEnvironment(environment);
        SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();

        registrar.registerBeanDefinitions(AnnotationMetadata.introspect(TestConfiguration.class), registry);

        List<Object> registeredModuleNames = Arrays.stream(registry.getBeanDefinitionNames())
                .map(registry::getBeanDefinition)
                .map(BeanDefinition::getConstructorArgumentValues)
                .map(arguments -> arguments.getIndexedArgumentValue(0, String.class))
                .filter(valueHolder -> valueHolder != null)
                .map(valueHolder -> valueHolder.getValue())
                .toList();
        assertTrue(registeredModuleNames.contains("sales-api"));
        assertTrue(registeredModuleNames.contains("sales_api"));
    }

    static class TestConfiguration {
    }
}
