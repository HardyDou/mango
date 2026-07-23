package io.mango.authorization.diagnostic;

import io.mango.authorization.starter.AuthorizationAutoConfiguration;
import io.mango.authorization.starter.diagnostic.AuthorizationModuleDiagnosticContributor;
import io.mango.resource.api.ResourceAuthorizationRequirementsProvider;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Orders the cross-module Authorization diagnostic after both owning modules are available.
 */
@AutoConfiguration(
        after = AuthorizationAutoConfiguration.class,
        afterName = "io.mango.resource.starter.ResourceRegistryAutoConfiguration")
@ConditionalOnClass(AuthorizationDiagnosticMapper.class)
@ConditionalOnBean(ResourceAuthorizationRequirementsProvider.class)
@MapperScan(basePackageClasses = AuthorizationDiagnosticMapper.class)
public class AuthorizationDiagnosticAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuthorizationModuleDiagnosticContributor authorizationModuleDiagnosticContributor(
            ResourceAuthorizationRequirementsProvider requirementsProvider,
            AuthorizationDiagnosticMapper diagnosticMapper) {
        return new AuthorizationModuleDiagnosticContributor(requirementsProvider, diagnosticMapper);
    }
}
