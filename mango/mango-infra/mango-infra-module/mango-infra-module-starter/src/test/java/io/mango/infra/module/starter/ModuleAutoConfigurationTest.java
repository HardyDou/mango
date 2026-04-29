package io.mango.infra.module.starter;

import io.mango.infra.module.api.ModuleInfoRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ModuleAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ModuleAutoConfiguration.class));

    @Test
    void moduleInfoRegistry_withConfigMapping_registersModuleInfo() {
        contextRunner
                .withPropertyValues(
                        "spring.application.name=mango-admin-app",
                        "server.servlet.context-path=/admin",
                        "mango.module.module-service.modules.mango-rbac.service-name=mango-admin-app",
                        "mango.module.module-service.modules.mango-rbac.context-path=/admin",
                        "mango.module.module-service.modules.mango-rbac.module-path=/rbac")
                .run(context -> {
                    ModuleInfoRegistry registry = context.getBean(ModuleInfoRegistry.class);
                    assertThat(registry.resolve("mango-rbac"))
                            .isPresent()
                            .get()
                            .extracting("serviceName", "contextPath", "modulePath")
                            .containsExactly("mango-admin-app", "/admin", "/rbac");
                });
    }

    @Test
    void moduleInfoRegistry_withClasspathMetadataWithoutModulePath_derivesModulePath() {
        contextRunner
                .withPropertyValues(
                        "spring.application.name=mango-platform-app",
                        "server.servlet.context-path=/platform")
                .withBean(ModuleMetadataLoader.class, () -> new TestModuleMetadataLoader(
                        new ModuleMetadataLoader.ModuleMetadata(
                                "mango-system",
                                "",
                                "test")))
                .run(context -> {
                    ModuleInfoRegistry registry = context.getBean(ModuleInfoRegistry.class);
                    assertThat(registry.resolve("mango-system"))
                            .isPresent()
                            .get()
                            .extracting("serviceName", "contextPath", "modulePath")
                            .containsExactly("mango-platform-app", "/platform", "/system");
                });
    }

    private static class TestModuleMetadataLoader extends ModuleMetadataLoader {

        private final ModuleMetadata moduleMetadata;

        TestModuleMetadataLoader(ModuleMetadata moduleMetadata) {
            this.moduleMetadata = moduleMetadata;
        }

        @Override
        public java.util.List<ModuleMetadata> load() {
            return java.util.List.of(moduleMetadata);
        }
    }
}
