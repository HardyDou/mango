package io.mango.infra.persistence.starter;

import io.mango.infra.persistence.starter.datasource.PersistenceDataSourceAutoConfiguration;
import io.mango.infra.persistence.starter.datasource.PersistenceDataSourceRegistry;
import io.mango.infra.persistence.api.datasource.PersistenceModuleDataSourceResolver;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Mango Flyway 自动配置。
 * <p>
 * 负责按模块加载数据库迁移脚本，支持通过配置开启或关闭指定模块的迁移。
 * 迁移脚本目录约定为 {@code classpath:db/migration/{module}/V*.sql}。
 * <p>
 * 配置示例：
 * <pre>
 * mango:
 *   persistence:
 *     flyway:
 *       enabled: true                     # 全局开关
 *       modules:
 *         user:
 *           enabled: true                 # 模块开关，默认开启
 * </pre>
 * <p>
 * 使用本配置时，应由 Mango 管理 Flyway 迁移。
 *
 * @see PersistenceFlywayProperties
 */
@AutoConfiguration(after = PersistenceDataSourceAutoConfiguration.class, before = FlywayAutoConfiguration.class)
@ConditionalOnClass(Flyway.class)
@ConditionalOnBean(DataSource.class)
@EnableConfigurationProperties(PersistenceFlywayProperties.class)
public class PersistenceFlywayAutoConfiguration {

    private static final String MIGRATION_LOCATION_PREFIX = "classpath:db/migration/";
    private static final String MIGRATION_SCAN_PATTERN = "classpath*:db/migration/*/V*.sql";
    private static final String NOOP_LOCATION = "classpath:db/migration/_noop";
    private static final String HISTORY_TABLE_PREFIX = "flyway_schema_history_";

    @Bean
    @DependsOn("dataSource")
    @ConditionalOnMissingBean(Flyway.class)
    public Flyway flyway(@Autowired DataSource dataSource,
                         @Autowired PersistenceFlywayProperties properties) {
        // Mango runs module migrations explicitly in the ApplicationRunner below.
        // This bean prevents Spring Boot's default Flyway flow from merging all
        // module locations into one history table, where duplicate V1 scripts clash.
        return Flyway.configure()
                .dataSource(dataSource)
                .locations(NOOP_LOCATION)
                .validateOnMigrate(false)
                .load();
    }

    @Bean
    @ConditionalOnMissingBean(name = "persistenceFlywayMigrationInitializer")
    public FlywayMigrationInitializer persistenceFlywayMigrationInitializer(@Autowired Flyway flyway,
                                                                           @Autowired DataSource dataSource,
                                                                           @Autowired PersistenceFlywayProperties properties,
                                                                           ObjectProvider<PersistenceDataSourceRegistry> registryProvider,
                                                                           ObjectProvider<PersistenceModuleDataSourceResolver> resolverProvider) {
        return new FlywayMigrationInitializer(flyway, ignored -> {
            if (!properties.isEnabled()) {
                return;
            }
            try {
                PersistenceDataSourceRegistry registry = registryProvider.getIfAvailable();
                PersistenceModuleDataSourceResolver resolver = resolverProvider.getIfAvailable();
                for (ModuleMigration module : resolveModuleMigrations(properties)) {
                    ResolvedDataSource resolvedDataSource = resolveDataSource(dataSource, module, registry, resolver);
                    DataSource moduleDataSource = resolvedDataSource.dataSource();
                    try {
                        FluentConfiguration configuration = Flyway.configure()
                                .dataSource(moduleDataSource)
                                .locations(module.location())
                                .table(resolveHistoryTable(module))
                                .baselineOnMigrate(module.config().isBaselineOnMigrate())
                                .baselineVersion("0")
                                .validateOnMigrate(module.config().isValidateOnMigrate())
                                .outOfOrder(false);
                        if (module.config().isIgnoreMissingMigrations()) {
                            configuration.ignoreMigrationPatterns("*:missing");
                        }
                        configuration.load().migrate();
                    } finally {
                        closeModuleDataSource(resolvedDataSource);
                    }
                }
            } catch (Exception e) {
                throw new IllegalStateException("Mango Flyway module migration failed", e);
            }
        });
    }

    private List<ModuleMigration> resolveModuleMigrations(PersistenceFlywayProperties properties) throws Exception {
        if (!properties.getModules().isEmpty()) {
            List<ModuleMigration> migrations = new ArrayList<>();
            properties.getModules().forEach((module, config) -> {
                if (config == null || config.isEnabled()) {
                    migrations.add(new ModuleMigration(
                            module,
                            MIGRATION_LOCATION_PREFIX + module,
                            config == null ? new PersistenceFlywayProperties.ModuleConfig() : config));
                }
            });
            return migrations;
        }

        Set<String> modules = discoverMigrationModules();
        List<ModuleMigration> migrations = new ArrayList<>();
        for (String module : modules) {
            migrations.add(new ModuleMigration(
                    module,
                    MIGRATION_LOCATION_PREFIX + module,
                    new PersistenceFlywayProperties.ModuleConfig()));
        }
        return migrations;
    }

    private Set<String> discoverMigrationModules() throws Exception {
        Resource[] resources = new PathMatchingResourcePatternResolver().getResources(MIGRATION_SCAN_PATTERN);
        Set<String> modules = new LinkedHashSet<>();
        for (Resource resource : resources) {
            String url = resource.getURL().toString();
            int prefixIndex = url.indexOf("/db/migration/");
            if (prefixIndex < 0) {
                continue;
            }
            String tail = url.substring(prefixIndex + "/db/migration/".length());
            int slashIndex = tail.indexOf('/');
            if (slashIndex > 0) {
                modules.add(tail.substring(0, slashIndex));
            }
        }
        return modules.stream()
                .filter(StringUtils::hasText)
                .sorted(Comparator.naturalOrder())
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
    }

    private String sanitizeModuleName(String moduleName) {
        return moduleName.replaceAll("[^A-Za-z0-9_]", "_");
    }

    private String resolveHistoryTable(ModuleMigration module) {
        if (StringUtils.hasText(module.config().getHistoryTable())) {
            return module.config().getHistoryTable();
        }
        return HISTORY_TABLE_PREFIX + sanitizeModuleName(module.name());
    }

    private ResolvedDataSource resolveDataSource(DataSource defaultDataSource,
                                                 ModuleMigration module,
                                                 PersistenceDataSourceRegistry registry,
                                                 PersistenceModuleDataSourceResolver resolver) {
        if (registry != null && resolver != null) {
            String dataSourceName = resolver.resolveDataSource(module.name()).orElse("");
            if (StringUtils.hasText(dataSourceName)) {
                return new ResolvedDataSource(registry.get(dataSourceName), false);
            }
        }

        PersistenceFlywayProperties.DataSourceConfig datasource = module.config().getDatasource();
        if (datasource == null || !StringUtils.hasText(datasource.getUrl())) {
            return new ResolvedDataSource(defaultDataSource, false);
        }

        DataSourceBuilder<?> builder = DataSourceBuilder.create()
                .url(datasource.getUrl())
                .username(datasource.getUsername())
                .password(datasource.getPassword());
        if (StringUtils.hasText(datasource.getDriverClassName())) {
            builder.driverClassName(datasource.getDriverClassName());
        }
        return new ResolvedDataSource(builder.build(), true);
    }

    private void closeModuleDataSource(ResolvedDataSource resolvedDataSource) throws Exception {
        if (!resolvedDataSource.closeAfterUse() || !(resolvedDataSource.dataSource() instanceof AutoCloseable closeable)) {
            return;
        }
        closeable.close();
    }

    private record ModuleMigration(String name,
                                   String location,
                                   PersistenceFlywayProperties.ModuleConfig config) {
    }

    private record ResolvedDataSource(DataSource dataSource, boolean closeAfterUse) {
    }
}
