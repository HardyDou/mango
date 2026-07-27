package io.mango.infra.bootstrap.starter;

import io.mango.infra.bootstrap.api.BootstrapStepContributor;
import io.mango.infra.bootstrap.core.BootstrapDatabaseLock;
import io.mango.infra.bootstrap.core.BootstrapManifestHasher;
import io.mango.infra.bootstrap.core.BootstrapOrchestrator;
import io.mango.infra.bootstrap.core.BootstrapPlanBuilder;
import io.mango.infra.bootstrap.core.BootstrapSchemaMigrator;
import io.mango.infra.bootstrap.core.JdbcBootstrapRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@AutoConfiguration
@ConditionalOnBean(DataSource.class)
@EnableConfigurationProperties({BootstrapProperties.class, MangoReleaseProperties.class})
public class BootstrapAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    BootstrapManifestHasher bootstrapManifestHasher() {
        return new BootstrapManifestHasher();
    }

    @Bean
    @ConditionalOnMissingBean
    BootstrapPlanBuilder bootstrapPlanBuilder(BootstrapManifestHasher hasher) {
        return new BootstrapPlanBuilder(hasher);
    }

    @Bean
    @ConditionalOnMissingBean
    BootstrapSchemaMigrator bootstrapSchemaMigrator(DataSource dataSource) {
        return new BootstrapSchemaMigrator(dataSource);
    }

    @Bean
    @ConditionalOnMissingBean
    BootstrapDatabaseLock bootstrapDatabaseLock(DataSource dataSource) {
        return new BootstrapDatabaseLock(dataSource);
    }

    @Bean
    @ConditionalOnMissingBean
    JdbcBootstrapRepository jdbcBootstrapRepository(DataSource dataSource) {
        return new JdbcBootstrapRepository(new JdbcTemplate(dataSource));
    }

    @Bean
    @ConditionalOnMissingBean
    BootstrapOrchestrator bootstrapOrchestrator(
            BootstrapPlanBuilder planBuilder,
            BootstrapManifestHasher hasher,
            BootstrapSchemaMigrator schemaMigrator,
            BootstrapDatabaseLock databaseLock,
            JdbcBootstrapRepository repository,
            ObjectProvider<BootstrapStepContributor> contributors) {
        return new BootstrapOrchestrator(planBuilder, hasher, schemaMigrator, databaseLock,
                repository, contributors.orderedStream().toList());
    }

    @Bean
    BootstrapCommandRunner bootstrapCommandRunner(BootstrapProperties bootstrapProperties,
                                                   MangoReleaseProperties releaseProperties,
                                                   BootstrapOrchestrator orchestrator) {
        return new BootstrapCommandRunner(bootstrapProperties, releaseProperties, orchestrator);
    }

    @Bean
    RuntimeLeaseManager runtimeLeaseManager(BootstrapProperties bootstrapProperties,
                                             MangoReleaseProperties releaseProperties,
                                             BootstrapPlanBuilder planBuilder,
                                             ObjectProvider<BootstrapStepContributor> contributors,
                                             JdbcBootstrapRepository repository,
                                             ApplicationEventPublisher eventPublisher) {
        return new RuntimeLeaseManager(bootstrapProperties, releaseProperties, planBuilder,
                contributors.orderedStream().toList(), repository, eventPublisher);
    }
}
