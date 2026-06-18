package io.mango.infra.persistence.starter;

import io.mango.infra.persistence.api.scope.DataScopeApplier;
import io.mango.infra.persistence.api.scope.DataScopeProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PersistenceAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PersistenceAutoConfiguration.class));

    private final ApplicationContextRunner authorizationOrderedContextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    PersistenceAutoConfiguration.class,
                    io.mango.authorization.starter.AuthorizationAutoConfiguration.class));

    @Test
    void dataScopeApplier_shouldBeCreatedWhenDataScopeProviderExists() {
        contextRunner
                .withUserConfiguration(DataScopeProviderConfig.class)
                .run(ctx -> assertThat(ctx).hasSingleBean(DataScopeApplier.class));
    }

    @Test
    void dataScopeApplier_shouldNotBeCreatedWhenDataScopeProviderMissing() {
        contextRunner.run(ctx -> assertThat(ctx).doesNotHaveBean(DataScopeApplier.class));
    }

    @Test
    void dataScopeApplier_shouldBeCreatedWhenAuthorizationAutoConfigurationIsImportedAfterPersistence() {
        authorizationOrderedContextRunner.run(ctx -> {
            assertThat(ctx).hasSingleBean(DataScopeProvider.class);
            assertThat(ctx).hasSingleBean(DataScopeApplier.class);
        });
    }

    @Test
    void dataScopeApplier_shouldNotDependOnAuthorizationStarterAtRuntime() {
        contextRunner
                .withClassLoader(new FilteredClassLoader("io.mango.authorization"))
                .withUserConfiguration(DataScopeProviderConfig.class)
                .run(ctx -> assertThat(ctx).hasSingleBean(DataScopeApplier.class));
    }

    @Configuration
    static class DataScopeProviderConfig {
        @Bean
        DataScopeProvider dataScopeProvider() {
            return resourceCode -> Optional.empty();
        }
    }
}
