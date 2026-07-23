package io.mango.infra.module.starter.diagnostic;

import io.mango.infra.module.starter.ModuleAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModuleDiagnosticEndpointAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ModuleAutoConfiguration.class,
                    ModuleDiagnosticEndpointAutoConfiguration.class));

    @Test
    void endpointIsAbsentByDefault() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(MangoModulesEndpoint.class));
    }

    @Test
    void endpointIsAbsentWithoutDedicatedAuthorizationBean() {
        contextRunner
                .withPropertyValues("mango.module.diagnostics.endpoint.enabled=true")
                .run(context -> assertThat(context).doesNotHaveBean(MangoModulesEndpoint.class));
    }

    @Test
    void endpointRequiresExplicitEnablementAndDedicatedAuthorizationBean() {
        contextRunner
                .withPropertyValues(
                        "mango.module.diagnostics.endpoint.enabled=true",
                        "spring.application.name=internal-admin")
                .withBean("mangoModuleDiagnosticAuthorizationManager", Object.class, Object::new)
                .withBean("mangoModuleDiagnosticSecurityFilterChain", Object.class, Object::new)
                .run(context -> {
                    assertThat(context).hasSingleBean(MangoModulesEndpoint.class);
                    var snapshot = context.getBean(MangoModulesEndpoint.class).diagnose(
                            "mango-link",
                            "internal-admin",
                            "ADMIN_MODULE_RUNTIME_V1");
                    assertThat(snapshot.service()).isEqualTo("internal-admin");
                    assertThat(snapshot.modules()).singleElement().satisfies(report -> {
                        assertThat(report.moduleCode()).isEqualTo("mango-link");
                        assertThat(report.conditions()).hasSize(5);
                    });
                    assertThatThrownBy(() -> context.getBean(MangoModulesEndpoint.class).diagnose(
                            "MANGO-LINK", "internal-admin", "ADMIN_MODULE_RUNTIME_V1"))
                            .isInstanceOf(IllegalArgumentException.class);
                });
    }

    @Test
    void endpointTreatsSlashContextPathAsRootContext() {
        contextRunner
                .withPropertyValues(
                        "mango.module.diagnostics.endpoint.enabled=true",
                        "server.servlet.context-path=/")
                .withBean("mangoModuleDiagnosticAuthorizationManager", Object.class, Object::new)
                .withBean("mangoModuleDiagnosticSecurityFilterChain", Object.class, Object::new)
                .run(context -> assertThat(context).hasSingleBean(MangoModulesEndpoint.class));
    }

    @Test
    void endpointIsAbsentWhenSecurityOrManagementShapeIsUnsupported() {
        contextRunner
                .withPropertyValues(
                        "mango.module.diagnostics.endpoint.enabled=true",
                        "mango.access.auth-enabled=false")
                .withBean("mangoModuleDiagnosticAuthorizationManager", Object.class, Object::new)
                .withBean("mangoModuleDiagnosticSecurityFilterChain", Object.class, Object::new)
                .run(context -> assertThat(context).doesNotHaveBean(MangoModulesEndpoint.class));
        contextRunner
                .withPropertyValues(
                        "mango.module.diagnostics.endpoint.enabled=true",
                        "management.endpoints.web.base-path=/manage")
                .withBean("mangoModuleDiagnosticAuthorizationManager", Object.class, Object::new)
                .withBean("mangoModuleDiagnosticSecurityFilterChain", Object.class, Object::new)
                .run(context -> assertThat(context).doesNotHaveBean(MangoModulesEndpoint.class));
        contextRunner
                .withPropertyValues(
                        "mango.module.diagnostics.endpoint.enabled=true",
                        "management.server.port=19091")
                .withBean("mangoModuleDiagnosticAuthorizationManager", Object.class, Object::new)
                .withBean("mangoModuleDiagnosticSecurityFilterChain", Object.class, Object::new)
                .run(context -> assertThat(context).doesNotHaveBean(MangoModulesEndpoint.class));
        contextRunner
                .withPropertyValues(
                        "mango.module.diagnostics.endpoint.enabled=true",
                        "server.servlet.context-path=/mango")
                .withBean("mangoModuleDiagnosticAuthorizationManager", Object.class, Object::new)
                .withBean("mangoModuleDiagnosticSecurityFilterChain", Object.class, Object::new)
                .run(context -> assertThat(context).doesNotHaveBean(MangoModulesEndpoint.class));
    }
}
