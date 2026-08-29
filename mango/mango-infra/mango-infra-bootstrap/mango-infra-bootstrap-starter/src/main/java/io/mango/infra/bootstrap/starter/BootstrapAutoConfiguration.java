package io.mango.infra.bootstrap.starter;

import io.mango.infra.bootstrap.api.BootstrapAction;
import io.mango.infra.bootstrap.api.BootstrapMode;
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
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;

@AutoConfiguration
@ConditionalOnBean(DataSource.class)
@EnableConfigurationProperties({BootstrapProperties.class, MangoReleaseProperties.class})
public class BootstrapAutoConfiguration {

    private static final String RESOURCE_BASELINE_ENVIRONMENT = "mango-resource-baseline-build";

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
            BootstrapProperties bootstrapProperties,
            ObjectProvider<BootstrapStepContributor> contributors) {
        return new BootstrapOrchestrator(planBuilder, hasher, schemaMigrator, databaseLock,
                repository, selectContributors(bootstrapProperties, contributors.orderedStream().toList()));
    }

    @Bean
    BootstrapReceiptWriter bootstrapReceiptWriter(BootstrapProperties bootstrapProperties,
                                                   Environment environment) {
        return new BootstrapReceiptWriter(bootstrapProperties, environment);
    }

    @Bean
    BootstrapCommandRunner bootstrapCommandRunner(BootstrapProperties bootstrapProperties,
                                                   MangoReleaseProperties releaseProperties,
                                                   BootstrapOrchestrator orchestrator,
                                                   JdbcBootstrapRepository repository,
                                                   BootstrapReceiptWriter receiptWriter) {
        return new BootstrapCommandRunner(
                bootstrapProperties, releaseProperties, orchestrator, repository, receiptWriter);
    }

    @Bean
    RuntimeLeaseManager runtimeLeaseManager(BootstrapProperties bootstrapProperties,
                                             MangoReleaseProperties releaseProperties,
                                             BootstrapPlanBuilder planBuilder,
                                             ObjectProvider<BootstrapStepContributor> contributors,
                                             JdbcBootstrapRepository repository,
                                             ApplicationEventPublisher eventPublisher) {
        return new RuntimeLeaseManager(bootstrapProperties, releaseProperties, planBuilder,
                selectContributors(bootstrapProperties, contributors.orderedStream().toList()),
                repository, eventPublisher);
    }

    static List<BootstrapStepContributor> selectContributors(
            BootstrapProperties properties,
            List<BootstrapStepContributor> contributors) {
        if (!properties.isResourceBaselineBuildEnabled()) {
            return List.copyOf(contributors);
        }
        if (properties.getMode() != BootstrapMode.BOOTSTRAP
                || properties.getAction() != BootstrapAction.APPLY
                || !RESOURCE_BASELINE_ENVIRONMENT.equals(properties.getEnvironmentKey())) {
            throw new IllegalStateException(
                    "Resource baseline build mode requires bootstrap apply and the reserved environment key");
        }
        List<BootstrapStepContributor> selected = contributors.stream()
                .filter(BootstrapStepContributor::supportsResourceBaselineBuild)
                .toList();
        if (selected.isEmpty()) {
            throw new IllegalStateException(
                    "Resource baseline build mode requires an eligible Resource Bootstrap contributor");
        }
        return selected;
    }
}
