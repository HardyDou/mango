package io.mango.authorization.starter;

import io.mango.infra.persistence.api.scope.DataScopeProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.Optional;

/**
 * Test auto-configuration with the same class name as the authorization starter.
 */
@AutoConfiguration
public class AuthorizationAutoConfiguration {

    @Bean
    DataScopeProvider dataScopeProvider() {
        return resourceCode -> Optional.empty();
    }
}
