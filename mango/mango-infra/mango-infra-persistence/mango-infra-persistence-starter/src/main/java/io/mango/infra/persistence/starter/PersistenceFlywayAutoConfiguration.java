package io.mango.infra.persistence.starter;

import io.mango.infra.persistence.starter.datasource.PersistenceDataSourceAutoConfiguration;
import io.mango.infra.persistence.starter.datasource.PersistenceDataSourceRegistry;
import io.mango.infra.persistence.api.datasource.PersistenceModuleDataSourceResolver;
import io.mango.infra.persistence.starter.diagnostic.PersistenceModuleDiagnosticContributor;
import io.mango.infra.persistence.starter.diagnostic.PersistenceModuleMigrationStatusRegistry;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.output.MigrateResult;
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
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mango Flyway 自动配置。
 * <p>
 * 负责按模块加载数据库迁移脚本，支持通过配置开启或关闭指定模块的迁移。
 * 迁移脚本目录约定为 {@code classpath:db/migration/{module}/V*.sql}。
 * 未显式配置模块 locations 时，会在目录存在的情况下追加
 * {@code ${MANGO_HOME:-/opt/mango}/upgrade/{module}} 外部升级目录。
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
@Slf4j
public class PersistenceFlywayAutoConfiguration {

    private static final int URL_TIMEOUT_MILLIS = 30_000;
    private static final String MIGRATION_LOCATION_PREFIX = "classpath:db/migration/";
    private static final String MIGRATION_SCAN_PATTERN = "classpath*:db/migration/*/V*.sql";
    private static final String NOOP_LOCATION = "classpath:db/migration/_noop";
    private static final String HISTORY_TABLE_PREFIX = "flyway_schema_history_";
    private static final String DEFAULT_MANGO_HOME = "/opt/mango";
    private static final String DEFAULT_UPGRADE_DIRECTORY_NAME = "upgrade";
    private static final Set<String> MANGO_NON_LINEAR_PUBLISHED_MODULES = Set.of(
            "authorization",
            "domain",
            "link",
            "mango-job",
            "notice",
            "numgen",
            "payment",
            "system"
    );
    private static final Set<String> MANGO_MISSING_MIGRATION_COMPATIBILITY_MODULES = Set.of("link");

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
    @ConditionalOnMissingBean
    public PersistenceModuleMigrationStatusRegistry persistenceModuleMigrationStatusRegistry() {
        return new PersistenceModuleMigrationStatusRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public PersistenceModuleDiagnosticContributor persistenceModuleDiagnosticContributor(
            PersistenceModuleMigrationStatusRegistry registry,
            PersistenceFlywayProperties properties) {
        return new PersistenceModuleDiagnosticContributor(registry, properties);
    }

    @Bean
    @ConditionalOnMissingBean(name = "persistenceFlywayMigrationInitializer")
    public FlywayMigrationInitializer persistenceFlywayMigrationInitializer(@Autowired Flyway flyway,
                                                                           @Autowired DataSource dataSource,
                                                                           @Autowired PersistenceFlywayProperties properties,
                                                                           PersistenceModuleMigrationStatusRegistry statusRegistry,
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
                    ResolvedDataSource resolvedDataSource = null;
                    ResolvedLocations resolvedLocations = null;
                    String historyTable = "<unresolved>";
                    boolean outOfOrder = resolveOutOfOrder(module);
                    boolean ignoreMissingMigrations = resolveIgnoreMissingMigrations(module);
                    boolean validateOnMigrate = module.config().isValidateOnMigrate();
                    String datasource = resolveDataSourceDescription(module, resolver);
                    try {
                        FluentConfiguration configuration = Flyway.configure();
                        resolvedDataSource = resolveDataSource(dataSource, module, registry, datasource);
                        DataSource moduleDataSource = resolvedDataSource.dataSource();
                        datasource = resolvedDataSource.description();
                        historyTable = resolveHistoryTable(module);
                        recordMigrationRunning(statusRegistry, module.name(), historyTable);
                        resolvedLocations = resolveLocations(module);
                        configuration
                                .dataSource(moduleDataSource)
                                .locations(resolvedLocations.locations().toArray(String[]::new))
                                .table(historyTable)
                                .baselineOnMigrate(module.config().isBaselineOnMigrate())
                                .baselineVersion("0")
                                .validateOnMigrate(validateOnMigrate)
                                .outOfOrder(outOfOrder);
                        if (ignoreMissingMigrations) {
                            configuration.ignoreMigrationPatterns("*:missing");
                        }
                        Flyway moduleFlyway = configuration.load();
                        MigrateResult migrateResult = moduleFlyway.migrate();
                        observeMigrationResult(
                                statusRegistry, module.name(), historyTable, moduleFlyway, migrateResult);
                    } catch (Exception e) {
                        recordMigrationFailed(statusRegistry, module.name(), historyTable);
                        throw new IllegalStateException(
                                "Mango Flyway module migration failed: module=" + module.name()
                                        + ", historyTable=" + historyTable
                                        + ", locations=" + module.locations()
                                        + ", datasource=" + datasource
                                        + ", validateOnMigrate=" + validateOnMigrate
                                        + ", outOfOrder=" + outOfOrder
                                        + ", ignoreMissingMigrations=" + ignoreMissingMigrations,
                                e);
                    } finally {
                        if (resolvedDataSource != null) {
                            closeModuleDataSource(resolvedDataSource);
                        }
                        if (resolvedLocations != null) {
                            cleanResolvedLocations(resolvedLocations);
                        }
                    }
                }
            } catch (Exception e) {
                if (e instanceof IllegalStateException illegalStateException
                        && shouldExposeFlywayFailure(illegalStateException)) {
                    throw illegalStateException;
                }
                throw new IllegalStateException("Mango Flyway module migration failed", e);
            }
        });
    }

    private void observeMigrationResult(
            PersistenceModuleMigrationStatusRegistry statusRegistry,
            String module,
            String historyTable,
            Flyway moduleFlyway,
            MigrateResult migrateResult) {
        try {
            MigrationInfo current = moduleFlyway.info().current();
            int pendingCount = moduleFlyway.info().pending().length;
            String currentVersion = current == null || current.getVersion() == null
                    ? migrateResult.targetSchemaVersion
                    : current.getVersion().getVersion();
            statusRegistry.applied(module, historyTable, currentVersion, pendingCount);
        } catch (RuntimeException observationFailure) {
            recordMigrationUnknown(statusRegistry, module, historyTable, "MIGRATION_OBSERVATION_FAILED");
            log.warn("Mango Flyway migration succeeded but diagnostic observation failed: module={}, historyTable={}",
                    module, historyTable, observationFailure);
        }
    }

    private void recordMigrationRunning(
            PersistenceModuleMigrationStatusRegistry statusRegistry,
            String module,
            String historyTable) {
        try {
            statusRegistry.running(module, historyTable);
        } catch (RuntimeException observationFailure) {
            log.warn("Mango Flyway diagnostic state update failed before migration: module={}, historyTable={}",
                    module, historyTable, observationFailure);
        }
    }

    private void recordMigrationFailed(
            PersistenceModuleMigrationStatusRegistry statusRegistry,
            String module,
            String historyTable) {
        try {
            statusRegistry.failed(module, historyTable);
        } catch (RuntimeException observationFailure) {
            log.warn("Mango Flyway diagnostic state update failed after migration failure: module={}, historyTable={}",
                    module, historyTable, observationFailure);
        }
    }

    private void recordMigrationUnknown(
            PersistenceModuleMigrationStatusRegistry statusRegistry,
            String module,
            String historyTable,
            String reasonCode) {
        try {
            statusRegistry.unknown(module, historyTable, reasonCode);
        } catch (RuntimeException observationFailure) {
            log.warn("Mango Flyway diagnostic state update failed after observation failure: module={}, historyTable={}",
                    module, historyTable, observationFailure);
        }
    }

    private boolean shouldExposeFlywayFailure(IllegalStateException exception) {
        String message = exception.getMessage();
        return message != null && (message.startsWith("Mango Flyway module migration failed:")
                || message.startsWith("Mango Flyway classpath migration modules are not fully declared:"));
    }

    private List<ModuleMigration> resolveModuleMigrations(PersistenceFlywayProperties properties) throws Exception {
        if (!properties.getModules().isEmpty()) {
            Set<String> discoveredModules = discoverMigrationModules();
            validateConfiguredClasspathModules(properties.getModules(), discoveredModules);
            List<ModuleMigration> migrations = new ArrayList<>();
            properties.getModules().forEach((module, config) -> {
                if (config == null || config.isEnabled()) {
                    migrations.add(new ModuleMigration(
                            module,
                            resolveConfiguredLocations(module, config, properties),
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
                    resolveDefaultLocations(module, properties),
                    new PersistenceFlywayProperties.ModuleConfig()));
        }
        return migrations;
    }

    private void validateConfiguredClasspathModules(
            Map<String, PersistenceFlywayProperties.ModuleConfig> configuredModules,
            Set<String> discoveredModules) {
        List<String> missingModules = new ArrayList<>();
        List<String> disabledWithoutSkipReason = new ArrayList<>();
        for (String module : discoveredModules) {
            if (!configuredModules.containsKey(module)) {
                missingModules.add(module);
                continue;
            }
            PersistenceFlywayProperties.ModuleConfig config = configuredModules.get(module);
            if (config != null && !config.isEnabled() && !StringUtils.hasText(config.getSkipReason())) {
                disabledWithoutSkipReason.add(module);
            }
        }
        if (missingModules.isEmpty() && disabledWithoutSkipReason.isEmpty()) {
            return;
        }
        List<String> migrationPaths = discoveredModules.stream()
                .map(module -> MIGRATION_LOCATION_PREFIX + module)
                .toList();
        throw new IllegalStateException(
                "Mango Flyway classpath migration modules are not fully declared: missingModules="
                        + missingModules
                        + ", disabledWithoutSkipReason=" + disabledWithoutSkipReason
                        + ", migrationPaths=" + migrationPaths
                        + ". Declare mango.persistence.flyway.modules.<module>.enabled=true, "
                        + "or set enabled=false with skip-reason for intentional skips.");
    }

    private List<String> resolveConfiguredLocations(String module,
                                                    PersistenceFlywayProperties.ModuleConfig config,
                                                    PersistenceFlywayProperties properties) {
        if (config != null && config.getLocations() != null && !config.getLocations().isEmpty()) {
            List<String> locations = config.getLocations().stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .toList();
            if (!locations.isEmpty()) {
                return locations;
            }
        }
        return resolveDefaultLocations(module, properties);
    }

    private List<String> resolveDefaultLocations(String module, PersistenceFlywayProperties properties) {
        List<String> locations = new ArrayList<>();
        locations.add(MIGRATION_LOCATION_PREFIX + module);
        appendDefaultUpgradeLocation(locations, module, properties);
        return List.copyOf(locations);
    }

    private void appendDefaultUpgradeLocation(List<String> locations,
                                              String module,
                                              PersistenceFlywayProperties properties) {
        if (properties == null || !properties.isUpgradeLocationsEnabled()) {
            return;
        }
        Path upgradeDirectory = resolveUpgradeDirectory(module, properties);
        if (!Files.isDirectory(upgradeDirectory)) {
            return;
        }
        String location = "filesystem:" + upgradeDirectory;
        if (!locations.contains(location)) {
            locations.add(location);
        }
    }

    private Path resolveUpgradeDirectory(String module, PersistenceFlywayProperties properties) {
        Path root = Path.of(resolveUpgradeRoot(properties)).toAbsolutePath().normalize();
        Path moduleDirectory = root.resolve(module).normalize();
        if (!moduleDirectory.startsWith(root)) {
            throw new IllegalStateException("Mango Flyway upgrade module directory is invalid: module=" + module);
        }
        return moduleDirectory;
    }

    private String resolveUpgradeRoot(PersistenceFlywayProperties properties) {
        if (properties != null && StringUtils.hasText(properties.getUpgradeRoot())) {
            return properties.getUpgradeRoot().trim();
        }
        String systemUpgradeRoot = System.getProperty("mango.upgrade.root");
        if (StringUtils.hasText(systemUpgradeRoot)) {
            return systemUpgradeRoot.trim();
        }
        String envUpgradeRoot = System.getenv("MANGO_UPGRADE_DIR");
        if (StringUtils.hasText(envUpgradeRoot)) {
            return envUpgradeRoot.trim();
        }
        String mangoHome = System.getProperty("mango.home");
        if (!StringUtils.hasText(mangoHome)) {
            mangoHome = System.getenv("MANGO_HOME");
        }
        if (!StringUtils.hasText(mangoHome)) {
            mangoHome = DEFAULT_MANGO_HOME;
        }
        return Path.of(mangoHome.trim(), DEFAULT_UPGRADE_DIRECTORY_NAME).toString();
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

    private boolean resolveOutOfOrder(ModuleMigration module) {
        Boolean configured = module.config().getOutOfOrder();
        if (configured != null) {
            return configured;
        }
        return MANGO_NON_LINEAR_PUBLISHED_MODULES.contains(module.name());
    }

    private boolean resolveIgnoreMissingMigrations(ModuleMigration module) {
        return module.config().isIgnoreMissingMigrations()
                || MANGO_MISSING_MIGRATION_COMPATIBILITY_MODULES.contains(module.name());
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
                                                 String dataSourceName) {
        if (registry != null && StringUtils.hasText(dataSourceName)) {
            return new ResolvedDataSource(registry.get(dataSourceName), false, dataSourceName);
        }

        PersistenceFlywayProperties.DataSourceConfig datasource = module.config().getDatasource();
        if (datasource == null || !StringUtils.hasText(datasource.getUrl())) {
            return new ResolvedDataSource(defaultDataSource, false, "default");
        }

        DataSourceBuilder<?> builder = DataSourceBuilder.create()
                .url(datasource.getUrl())
                .username(datasource.getUsername())
                .password(datasource.getPassword());
        if (StringUtils.hasText(datasource.getDriverClassName())) {
            builder.driverClassName(datasource.getDriverClassName());
        }
        return new ResolvedDataSource(builder.build(), true, "module-config");
    }

    private String resolveDataSourceDescription(ModuleMigration module,
                                                PersistenceModuleDataSourceResolver resolver) {
        if (resolver == null) {
            return "default";
        }
        return resolver.resolveDataSource(module.name()).orElse("default");
    }

    private void closeModuleDataSource(ResolvedDataSource resolvedDataSource) throws Exception {
        if (!resolvedDataSource.closeAfterUse() || !(resolvedDataSource.dataSource() instanceof AutoCloseable closeable)) {
            return;
        }
        closeable.close();
    }

    private ResolvedLocations resolveLocations(ModuleMigration module) throws IOException {
        List<String> locations = new ArrayList<>();
        List<Path> tempDirectories = new ArrayList<>();
        Path urlDirectory = null;
        for (String location : module.locations()) {
            if (!isHttpLocation(location)) {
                locations.add(location);
                continue;
            }
            if (urlDirectory == null) {
                urlDirectory = Files.createTempDirectory("mango-flyway-" + sanitizeModuleName(module.name()) + "-");
                tempDirectories.add(urlDirectory);
            }
            downloadSql(location, urlDirectory);
        }
        if (urlDirectory != null) {
            locations.add("filesystem:" + urlDirectory.toAbsolutePath());
        }
        return new ResolvedLocations(locations, tempDirectories);
    }

    private boolean isHttpLocation(String location) {
        return location.startsWith("http://") || location.startsWith("https://");
    }

    private void downloadSql(String location, Path targetDirectory) throws IOException {
        URI uri = URI.create(location);
        String path = uri.getPath();
        int lastSlashIndex = path == null ? -1 : path.lastIndexOf('/');
        String filename = lastSlashIndex >= 0 ? path.substring(lastSlashIndex + 1) : path;
        if (!StringUtils.hasText(filename) || !filename.endsWith(".sql")) {
            throw new IllegalStateException("Mango Flyway URL migration must point to a .sql file: " + location);
        }
        Path target = targetDirectory.resolve(filename).normalize();
        if (!target.startsWith(targetDirectory)) {
            throw new IllegalStateException("Mango Flyway URL migration filename is invalid: " + location);
        }
        if (Files.exists(target)) {
            throw new IllegalStateException("Mango Flyway URL migration filename is duplicated: " + filename);
        }
        var connection = uri.toURL().openConnection();
        connection.setConnectTimeout(URL_TIMEOUT_MILLIS);
        connection.setReadTimeout(URL_TIMEOUT_MILLIS);
        try (InputStream inputStream = connection.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void cleanResolvedLocations(ResolvedLocations locations) throws IOException {
        for (Path tempDirectory : locations.tempDirectories()) {
            if (!Files.exists(tempDirectory)) {
                continue;
            }
            try (var paths = Files.walk(tempDirectory)) {
                paths.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException e) {
                                throw new IllegalStateException("Clean Mango Flyway temp location failed: " + path, e);
                            }
                        });
            }
        }
    }

    private record ModuleMigration(String name,
                                   List<String> locations,
                                   PersistenceFlywayProperties.ModuleConfig config) {
    }

    private record ResolvedDataSource(DataSource dataSource, boolean closeAfterUse, String description) {
    }

    private record ResolvedLocations(List<String> locations, List<Path> tempDirectories) {
    }
}
