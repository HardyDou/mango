package io.mango.auth.starter.config;

import io.mango.authorization.api.IAuthorizationProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

/** Registers the fixed-name authorization manager used only by module diagnostics. */
@AutoConfiguration
@AutoConfigureAfter(name = "io.mango.authorization.starter.AuthorizationAutoConfiguration")
public class ModuleDiagnosticAuthorizationAutoConfiguration {

    @Bean(name = ModuleDiagnosticAuthorizationManager.BEAN_NAME)
    @ConditionalOnBean(IAuthorizationProvider.class)
    @ConditionalOnMissingBean(name = ModuleDiagnosticAuthorizationManager.BEAN_NAME)
    public AuthorizationManager<RequestAuthorizationContext> mangoModuleDiagnosticAuthorizationManager(
            IAuthorizationProvider authorizationProvider) {
        return new ModuleDiagnosticAuthorizationManager(authorizationProvider);
    }
}
