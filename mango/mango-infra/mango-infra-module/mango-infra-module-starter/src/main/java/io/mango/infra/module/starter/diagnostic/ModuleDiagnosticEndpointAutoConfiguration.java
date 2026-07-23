package io.mango.infra.module.starter.diagnostic;

import io.mango.infra.module.api.diagnostic.ModuleInstallationRegistry;
import io.mango.infra.module.core.diagnostic.ModuleDiagnosticAggregator;
import io.mango.infra.module.core.diagnostic.ModuleDiagnosticCoordinator;
import io.mango.infra.module.starter.ModuleAutoConfiguration;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/** Default-off assembly for the module diagnostic Actuator endpoint. */
@AutoConfiguration
@AutoConfigureAfter(
        value = ModuleAutoConfiguration.class,
        name = "io.mango.auth.starter.config.ModuleDiagnosticSecurityAutoConfiguration")
@ConditionalOnClass(Endpoint.class)
@ConditionalOnProperty(name = "mango.module.diagnostics.endpoint.enabled", havingValue = "true")
@ConditionalOnProperty(name = "mango.access.auth-enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(
        name = "management.endpoints.web.base-path",
        havingValue = "/actuator",
        matchIfMissing = true)
@ConditionalOnExpression(
        "'${management.server.port:}' == ''"
                + " && ('${server.servlet.context-path:}' == ''"
                + " || '${server.servlet.context-path:}' == '/')")
@ConditionalOnBean(name = {
        "mangoModuleDiagnosticAuthorizationManager",
        "mangoModuleDiagnosticSecurityFilterChain"
})
public class ModuleDiagnosticEndpointAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ModuleDiagnosticCoordinator moduleDiagnosticCoordinator(ModuleDiagnosticAggregator aggregator) {
        return new ModuleDiagnosticCoordinator(aggregator);
    }

    @Bean
    @ConditionalOnMissingBean
    public MangoModulesEndpoint mangoModulesEndpoint(
            ModuleDiagnosticCoordinator coordinator,
            ModuleInstallationRegistry installations,
            Environment environment) {
        return new MangoModulesEndpoint(
                coordinator,
                installations,
                environment.getProperty("spring.application.name", "application"),
                firstText(
                        environment.getProperty("spring.application.instance-id"),
                        environment.getProperty("HOSTNAME"),
                        "unknown"));
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "unknown";
    }
}
