package io.mango.infra.persistence.starter;

import io.mango.infra.bootstrap.api.BootstrapPhase;
import io.mango.infra.persistence.starter.datasource.PersistenceDataSourceAutoConfiguration;
import io.mango.infra.persistence.starter.datasource.PersistenceDataSourceRegistry;
import io.mango.infra.persistence.api.datasource.PersistenceModuleDataSourceResolver;
import io.mango.infra.persistence.starter.diagnostic.PersistenceModuleDiagnosticContributor;
import io.mango.infra.persistence.starter.diagnostic.PersistenceModuleMigrationStatusRegistry;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.output.MigrateResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.NoneNestedConditions;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.ConfigurationCondition.ConfigurationPhase;
import org.springframework.core.io.Resource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

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
    private static final int MAX_LOGICAL_DATA_SOURCE_KEY_LENGTH = 128;
    private static final int CHECKSUM_SUMMARY_LENGTH = 16;
    private static final int MAX_EXISTING_TABLES_IN_ERROR = 20;
    private static final String MIGRATION_LOCATION_PREFIX = "classpath:db/migration/";
    private static final String MIGRATION_SCAN_PATTERN = "classpath*:db/migration/*/*.sql";
    private static final String CONTRACT_MIGRATION_LOCATION_PREFIX = "classpath:db/migration-contract/";
    private static final String CONTRACT_MIGRATION_SCAN_PATTERN = "classpath*:db/migration-contract/*/*.sql";
    private static final String NOOP_LOCATION = "classpath:db/migration/_noop";
    private static final String HISTORY_TABLE_PREFIX = "flyway_schema_history_";
    private static final String DEFAULT_MANGO_HOME = "/opt/mango";
    private static final String DEFAULT_UPGRADE_DIRECTORY_NAME = "upgrade";
    private static final Set<String> PERSISTENCE_EXCLUDED_CLASSPATH_MODULES = Set.of("bootstrap");
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
        // Mango runs module migrations through its lifecycle executor or the
        // compatibility initializer below.
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
    @ConditionalOnMissingBean
    public PersistenceFlywayBootstrapExecutor persistenceFlywayBootstrapExecutor(
            @Autowired DataSource dataSource,
            @Autowired PersistenceFlywayProperties properties,
            PersistenceModuleMigrationStatusRegistry statusRegistry,
            ObjectProvider<PersistenceDataSourceRegistry> registryProvider,
            ObjectProvider<PersistenceModuleDataSourceResolver> resolverProvider) {
        return new PersistenceFlywayBootstrapExecutor(phase -> {
            if (!properties.isEnabled()) {
                return new PersistenceFlywayBootstrapExecutor.MigrationSummary(0, 0, phase.name());
            }
            int moduleCount = 0;
            int migrationCount = 0;
            try {
                PersistenceDataSourceRegistry registry = registryProvider.getIfAvailable();
                PersistenceModuleDataSourceResolver resolver = resolverProvider.getIfAvailable();
                for (ModuleMigration module : resolveModuleMigrations(properties, phase)) {
                    moduleCount++;
                    ResolvedDataSource resolvedDataSource = null;
                    ResolvedLocations resolvedLocations = null;
                    String historyTable = "<unresolved>";
                    boolean outOfOrder = resolveOutOfOrder(module);
                    boolean ignoreMissingMigrations = resolveIgnoreMissingMigrations(module);
                    boolean validateOnMigrate = module.config().isValidateOnMigrate();
                    String registeredDataSourceName = resolveRegisteredDataSourceName(module, resolver);
                    String datasource = resolveDataSourceDescription(module, registry, registeredDataSourceName);
                    try {
                        FluentConfiguration configuration = Flyway.configure();
                        resolvedDataSource = resolveDataSource(
                                dataSource, module, registry, registeredDataSourceName);
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
                        migrationCount += migrateResult.migrationsExecuted;
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
            return new PersistenceFlywayBootstrapExecutor.MigrationSummary(
                    moduleCount, migrationCount, phase.name());
        }, () -> applyColdBaseline(dataSource, properties, registryProvider.getIfAvailable(),
                resolverProvider.getIfAvailable()));
    }

    @Bean
    @ConditionalOnMissingBean(FlywayMigrationInitializer.class)
    @Conditional(LegacyDirectStartupCondition.class)
    public FlywayMigrationInitializer persistenceFlywayMigrationInitializer(
            Flyway flyway,
            PersistenceFlywayBootstrapExecutor executor) {
        return new FlywayMigrationInitializer(flyway,
                ignored -> executor.migrate(BootstrapPhase.EXPAND));
    }

    static final class LegacyDirectStartupCondition extends NoneNestedConditions {

        LegacyDirectStartupCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnProperty(prefix = "mango.bootstrap", name = "mode")
        static final class BootstrapLifecycleModeConfigured {
        }
    }

    private PersistenceFlywayBootstrapExecutor.MigrationSummary applyColdBaseline(
            DataSource dataSource,
            PersistenceFlywayProperties properties,
            PersistenceDataSourceRegistry registry,
            PersistenceModuleDataSourceResolver resolver) {
        PersistenceFlywayProperties.ColdBaseline baseline = properties.getColdBaseline();
        if (baseline == null || !baseline.isEnabled()) {
            return new PersistenceFlywayBootstrapExecutor.MigrationSummary(0, 0, "COLD_BASELINE_DISABLED");
        }
        try {
            List<ModuleMigration> modules = resolveModuleMigrations(properties, BootstrapPhase.EXPAND);
            Map<String, List<ColdBaselineModule>> groups = new TreeMap<>();
            for (ModuleMigration module : modules) {
                ColdBaselineModule resolved = resolveColdBaselineModule(module, registry, resolver);
                groups.computeIfAbsent(resolved.logicalDataSourceKey(), ignored -> new ArrayList<>())
                        .add(resolved);
            }
            validateColdBaselineRouting(groups);
            int applied = 0;
            for (Map.Entry<String, List<ColdBaselineModule>> entry : groups.entrySet()) {
                List<ColdBaselineModule> group = entry.getValue().stream()
                        .sorted(Comparator.comparing(item -> item.migration().name()))
                        .toList();
                validateColdBaselineDataSourceGroup(entry.getKey(), group);
                ColdBaselineModule first = group.getFirst();
                ResolvedDataSource resolvedDataSource = resolveDataSource(
                        dataSource, first.migration(), registry, first.registeredDataSourceName());
                try {
                    applied += applyColdBaselineGroup(entry.getKey(), group, resolvedDataSource.dataSource());
                } finally {
                    closeModuleDataSource(resolvedDataSource);
                }
            }
            return new PersistenceFlywayBootstrapExecutor.MigrationSummary(
                    modules.size(), applied, "COLD_BASELINE");
        } catch (Exception exception) {
            if (exception instanceof IllegalStateException illegalStateException) {
                throw illegalStateException;
            }
            throw new IllegalStateException("Apply Mango cold baseline failed", exception);
        }
    }

    private ColdBaselineModule resolveColdBaselineModule(
            ModuleMigration module,
            PersistenceDataSourceRegistry registry,
            PersistenceModuleDataSourceResolver resolver) throws IOException {
        PersistenceFlywayProperties.BaselineConfig config = module.config().getBaseline();
        Resource resource;
        String location;
        if (config != null && StringUtils.hasText(config.getLocation())) {
            location = config.getLocation().trim();
            resource = new DefaultResourceLoader().getResource(location);
            if (!resource.exists() || !resource.isReadable()) {
                throw new IllegalStateException("Mango cold baseline SQL is not readable: module="
                        + module.name() + ", location=" + location);
            }
        } else {
            String pattern = "classpath*:db/baseline/" + module.name() + "/B*__baseline.sql";
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources(pattern);
            if (resources.length != 1) {
                throw new IllegalStateException("Mango cold baseline requires exactly one current SQL per module: "
                        + "module=" + module.name() + ", pattern=" + pattern + ", count=" + resources.length);
            }
            resource = resources[0];
            location = resource.getURL().toString();
        }
        assertIdempotentBaseline(resource, module.name());
        String version = config != null && StringUtils.hasText(config.getVersion())
                ? config.getVersion().trim() : baselineVersion(resource, module.name());
        String checksum = sha256(resource);
        String registeredName = resolveRegisteredDataSourceName(module, resolver);
        String logicalKey = resolveLogicalDataSourceKey(module, registry, registeredName);
        if (logicalKey.length() > MAX_LOGICAL_DATA_SOURCE_KEY_LENGTH) {
            throw new IllegalStateException("Mango logical datasource key is too long: module=" + module.name());
        }
        return new ColdBaselineModule(module, resource, location, version, checksum,
                registeredName, logicalKey);
    }

    private int applyColdBaselineGroup(String logicalKey,
                                       List<ColdBaselineModule> modules,
                                       DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        if (!coldBaselineStateTablesExist(jdbcTemplate)) {
            List<String> existingTables = coldBaselineUserTables(jdbcTemplate);
            if (!existingTables.isEmpty()) {
                log.info("Mango cold baseline skipped for existing datasource: datasource={}, existingTableCount={}",
                        logicalKey, existingTables.size());
                return 0;
            }
        }
        ensureColdBaselineStateTables(jdbcTemplate);
        String fingerprint = coldBaselineGroupFingerprint(logicalKey, modules);
        ColdBaselineControl control = findColdBaselineControl(jdbcTemplate, logicalKey);
        if (control == null) {
            assertColdBaselineDatabaseEmpty(dataSource);
            jdbcTemplate.update("""
                    INSERT INTO mango_cold_baseline_control
                        (logical_key, fingerprint, status, updated_at)
                    VALUES (?, ?, 'IN_PROGRESS', ?)
                    """, logicalKey, fingerprint, LocalDateTime.now());
        } else {
            if ("COMPLETED".equals(control.status())) {
                return 0;
            }
            if (!"IN_PROGRESS".equals(control.status())) {
                throw new IllegalStateException("Mango cold baseline has invalid control status: datasource="
                        + logicalKey + ", status=" + control.status());
            }
            if (!fingerprint.equals(control.fingerprint())) {
                throw new IllegalStateException("Mango cold baseline fingerprint mismatch: datasource="
                        + logicalKey);
            }
        }

        int applied = 0;
        for (ColdBaselineModule module : modules) {
            if (coldBaselineModuleCompleted(jdbcTemplate, logicalKey, module)) {
                continue;
            }
            Flyway flyway = coldBaselineFlyway(dataSource, module);
            MigrationInfo current = flyway.info().current();
            String baselineDescription = coldBaselineDescription(module);
            if (current != null) {
                String currentVersion = current.getVersion() == null ? null : current.getVersion().getVersion();
                if (!module.version().equals(currentVersion)
                        || !baselineDescription.equals(current.getDescription())) {
                    throw new IllegalStateException("Mango cold baseline found unexpected Flyway history: module="
                            + module.migration().name() + ", currentVersion=" + currentVersion);
                }
            } else {
                executeColdBaselineSql(dataSource, module);
                coldBaselineFlyway(dataSource, module).baseline();
                applied++;
            }
            markColdBaselineModuleCompleted(jdbcTemplate, logicalKey, module);
        }
        jdbcTemplate.update("""
                UPDATE mango_cold_baseline_control
                   SET status = 'COMPLETED', updated_at = ?
                 WHERE logical_key = ? AND fingerprint = ?
                """, LocalDateTime.now(), logicalKey, fingerprint);
        return applied;
    }

    private void executeColdBaselineSql(DataSource dataSource, ColdBaselineModule module) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.setContinueOnError(false);
        populator.setIgnoreFailedDrops(false);
        populator.addScript(module.resource());
        DatabasePopulatorUtils.execute(populator, dataSource);
    }

    private Flyway coldBaselineFlyway(DataSource dataSource, ColdBaselineModule module) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations(NOOP_LOCATION)
                .table(resolveHistoryTable(module.migration()))
                .baselineVersion(module.version())
                .baselineDescription(coldBaselineDescription(module))
                .load();
    }

    private String coldBaselineDescription(ColdBaselineModule module) {
        return "Mango cold baseline " + module.migration().name()
                + " sha256=" + module.checksum().substring(0, CHECKSUM_SUMMARY_LENGTH);
    }

    private boolean coldBaselineStateTablesExist(JdbcTemplate jdbcTemplate) {
        Set<String> tables = databaseTables(Objects.requireNonNull(jdbcTemplate.getDataSource()));
        return tables.contains("mango_cold_baseline_control")
                && tables.contains("mango_cold_baseline_module");
    }

    private void ensureColdBaselineStateTables(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS mango_cold_baseline_control (
                    logical_key varchar(128) NOT NULL,
                    fingerprint char(64) NOT NULL,
                    status varchar(32) NOT NULL,
                    updated_at timestamp(6) NOT NULL,
                    PRIMARY KEY (logical_key)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS mango_cold_baseline_module (
                    logical_key varchar(128) NOT NULL,
                    module_code varchar(128) NOT NULL,
                    baseline_version varchar(64) NOT NULL,
                    checksum char(64) NOT NULL,
                    status varchar(32) NOT NULL,
                    updated_at timestamp(6) NOT NULL,
                    PRIMARY KEY (logical_key, module_code)
                )
                """);
    }

    private ColdBaselineControl findColdBaselineControl(JdbcTemplate jdbcTemplate, String logicalKey) {
        List<ColdBaselineControl> controls = jdbcTemplate.query("""
                SELECT fingerprint, status
                  FROM mango_cold_baseline_control
                 WHERE logical_key = ?
                """, (resultSet, rowNumber) -> new ColdBaselineControl(
                resultSet.getString("fingerprint"), resultSet.getString("status")), logicalKey);
        return controls.isEmpty() ? null : controls.getFirst();
    }

    private boolean coldBaselineModuleCompleted(JdbcTemplate jdbcTemplate,
                                                String logicalKey,
                                                ColdBaselineModule module) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT baseline_version, checksum, status
                  FROM mango_cold_baseline_module
                 WHERE logical_key = ? AND module_code = ?
                """, logicalKey, module.migration().name());
        if (rows.isEmpty()) {
            return false;
        }
        Map<String, Object> row = rows.getFirst();
        if (!module.version().equals(String.valueOf(row.get("baseline_version")))
                || !module.checksum().equals(String.valueOf(row.get("checksum")))) {
            throw new IllegalStateException("Mango cold baseline module fingerprint mismatch: datasource="
                    + logicalKey + ", module=" + module.migration().name());
        }
        return "COMPLETED".equals(String.valueOf(row.get("status")));
    }

    private void markColdBaselineModuleCompleted(JdbcTemplate jdbcTemplate,
                                                 String logicalKey,
                                                 ColdBaselineModule module) {
        int updated = jdbcTemplate.update("""
                UPDATE mango_cold_baseline_module
                   SET baseline_version = ?, checksum = ?, status = 'COMPLETED', updated_at = ?
                 WHERE logical_key = ? AND module_code = ?
                """, module.version(), module.checksum(), LocalDateTime.now(),
                logicalKey, module.migration().name());
        if (updated == 0) {
            jdbcTemplate.update("""
                    INSERT INTO mango_cold_baseline_module
                        (logical_key, module_code, baseline_version, checksum, status, updated_at)
                    VALUES (?, ?, ?, ?, 'COMPLETED', ?)
                    """, logicalKey, module.migration().name(), module.version(), module.checksum(),
                    LocalDateTime.now());
        }
    }

    private String coldBaselineGroupFingerprint(String logicalKey, List<ColdBaselineModule> modules) {
        StringBuilder material = new StringBuilder(logicalKey);
        modules.forEach(module -> material.append('|')
                .append(module.migration().name()).append(':')
                .append(module.version()).append(':').append(module.checksum()));
        return sha256(material.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private void assertColdBaselineDatabaseEmpty(DataSource dataSource) {
        List<String> userTables = coldBaselineUserTables(new JdbcTemplate(dataSource));
        if (!userTables.isEmpty()) {
            throw new IllegalStateException("Mango cold baseline requires an empty database: existingTables="
                    + userTables.stream().sorted().limit(MAX_EXISTING_TABLES_IN_ERROR).toList());
        }
    }

    private List<String> coldBaselineUserTables(JdbcTemplate jdbcTemplate) {
        Set<String> excluded = Set.of(
                "mango_cold_baseline_control",
                "mango_cold_baseline_module",
                "flyway_schema_history_bootstrap",
                "mango_bootstrap_control",
                "mango_bootstrap_execution",
                "mango_bootstrap_step_execution",
                "mango_runtime_instance");
        return databaseTables(Objects.requireNonNull(jdbcTemplate.getDataSource())).stream()
                .filter(table -> !excluded.contains(table))
                .sorted()
                .toList();
    }

    private Set<String> databaseTables(DataSource dataSource) {
        Set<String> tables = new LinkedHashSet<>();
        try (Connection connection = dataSource.getConnection();
             ResultSet resultSet = connection.getMetaData().getTables(
                     connection.getCatalog(), connection.getSchema(), "%", new String[]{"TABLE"})) {
            while (resultSet.next()) {
                tables.add(resultSet.getString("TABLE_NAME").toLowerCase(java.util.Locale.ROOT));
            }
            return tables;
        } catch (SQLException exception) {
            throw new IllegalStateException("Inspect Mango cold baseline datasource tables failed", exception);
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public PersistenceBootstrapStepContributor persistenceBootstrapStepContributor(
            PersistenceFlywayProperties properties,
            PersistenceModuleMigrationStatusRegistry statusRegistry,
            PersistenceFlywayBootstrapExecutor executor,
            ObjectProvider<PersistenceDataSourceRegistry> registryProvider,
            ObjectProvider<PersistenceModuleDataSourceResolver> resolverProvider) {
        return new PersistenceBootstrapStepContributor(properties, statusRegistry, executor,
                registryProvider.getIfAvailable(), resolverProvider.getIfAvailable());
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

    private List<ModuleMigration> resolveModuleMigrations(PersistenceFlywayProperties properties,
                                                           BootstrapPhase phase) throws Exception {
        boolean contract = phase == BootstrapPhase.FINALIZE;
        String scanPattern = contract ? CONTRACT_MIGRATION_SCAN_PATTERN : MIGRATION_SCAN_PATTERN;
        String locationPrefix = contract ? CONTRACT_MIGRATION_LOCATION_PREFIX : MIGRATION_LOCATION_PREFIX;
        if (!properties.getModules().isEmpty()) {
            Set<String> discoveredModules = discoverMigrationModules(scanPattern);
            validateConfiguredClasspathModules(properties.getModules(), discoveredModules, locationPrefix);
            List<ModuleMigration> migrations = new ArrayList<>();
            properties.getModules().forEach((module, config) -> {
                if ((config == null || config.isEnabled())
                        && (!contract || hasContractMigrations(module, config, discoveredModules))) {
                    migrations.add(new ModuleMigration(
                            module,
                            contract
                                    ? resolveContractLocations(module, config)
                                    : resolveConfiguredLocations(module, config, properties),
                            config == null ? new PersistenceFlywayProperties.ModuleConfig() : config));
                }
            });
            return migrations;
        }

        Set<String> modules = discoverMigrationModules(scanPattern);
        List<ModuleMigration> migrations = new ArrayList<>();
        for (String module : modules) {
            migrations.add(new ModuleMigration(
                    module,
                    contract ? List.of(CONTRACT_MIGRATION_LOCATION_PREFIX + module)
                            : resolveDefaultLocations(module, properties),
                    new PersistenceFlywayProperties.ModuleConfig()));
        }
        return migrations;
    }

    private void validateConfiguredClasspathModules(
            Map<String, PersistenceFlywayProperties.ModuleConfig> configuredModules,
            Set<String> discoveredModules,
            String locationPrefix) {
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
                .map(module -> locationPrefix + module)
                .toList();
        throw new IllegalStateException(
                "Mango Flyway classpath migration modules are not fully declared: missingModules="
                        + missingModules
                        + ", disabledWithoutSkipReason=" + disabledWithoutSkipReason
                        + ", migrationPaths=" + migrationPaths
                        + ". Declare mango.persistence.flyway.modules.<module>.enabled=true, "
                        + "or set enabled=false with skip-reason for intentional skips.");
    }

    private boolean hasContractMigrations(String module,
                                          PersistenceFlywayProperties.ModuleConfig config,
                                          Set<String> discoveredModules) {
        return config != null && config.getContractLocations() != null
                && config.getContractLocations().stream().anyMatch(StringUtils::hasText)
                || discoveredModules.contains(module);
    }

    private List<String> resolveContractLocations(String module,
                                                  PersistenceFlywayProperties.ModuleConfig config) {
        if (config != null && config.getContractLocations() != null) {
            List<String> configured = config.getContractLocations().stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .toList();
            if (!configured.isEmpty()) {
                return configured;
            }
        }
        return List.of(CONTRACT_MIGRATION_LOCATION_PREFIX + module);
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

    private Set<String> discoverMigrationModules(String scanPattern) throws Exception {
        Resource[] resources = new PathMatchingResourcePatternResolver().getResources(scanPattern);
        Set<String> modules = new LinkedHashSet<>();
        for (Resource resource : resources) {
            String url = resource.getURL().toString();
            String pathPrefix = scanPattern.equals(CONTRACT_MIGRATION_SCAN_PATTERN)
                    ? "/db/migration-contract/" : "/db/migration/";
            int prefixIndex = url.indexOf(pathPrefix);
            if (prefixIndex < 0) {
                continue;
            }
            String tail = url.substring(prefixIndex + pathPrefix.length());
            int slashIndex = tail.indexOf('/');
            if (slashIndex > 0) {
                modules.add(tail.substring(0, slashIndex));
            }
        }
        return modules.stream()
                .filter(StringUtils::hasText)
                .filter(module -> !PERSISTENCE_EXCLUDED_CLASSPATH_MODULES.contains(module))
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

    private String resolveRegisteredDataSourceName(ModuleMigration module,
                                                   PersistenceModuleDataSourceResolver resolver) {
        if (resolver == null) {
            return null;
        }
        return resolver.resolveDataSource(module.name())
                .filter(StringUtils::hasText)
                .map(String::trim)
                .orElse(null);
    }

    private String resolveLogicalDataSourceKey(ModuleMigration module,
                                               PersistenceDataSourceRegistry registry,
                                               String registeredDataSourceName) {
        if (StringUtils.hasText(registeredDataSourceName)) {
            if (registry == null) {
                throw new IllegalStateException("Mango module datasource resolver requires a datasource registry: "
                        + "module=" + module.name() + ", datasource=" + registeredDataSourceName);
            }
            return registeredDataSourceName;
        }

        PersistenceFlywayProperties.DataSourceConfig datasource = module.config().getDatasource();
        if (datasource != null && StringUtils.hasText(datasource.getUrl())) {
            if (!StringUtils.hasText(datasource.getLogicalName())) {
                throw new IllegalStateException("Mango cold baseline explicit datasource requires logical-name: module="
                        + module.name());
            }
            return datasource.getLogicalName().trim();
        }
        if (registry != null && StringUtils.hasText(registry.primaryName())) {
            return registry.primaryName().trim();
        }
        return "default";
    }

    private void validateColdBaselineDataSourceGroup(String logicalKey,
                                                     List<ColdBaselineModule> modules) {
        if (modules.isEmpty()) {
            throw new IllegalStateException("Mango cold baseline datasource group is empty: datasource=" + logicalKey);
        }
        List<ColdBaselineModule> explicitModules = modules.stream()
                .filter(module -> hasExplicitDataSource(module.migration()))
                .toList();
        if (explicitModules.isEmpty()) {
            return;
        }
        if (explicitModules.size() != modules.size()) {
            throw new IllegalStateException("Mango cold baseline datasource group mixes explicit and managed "
                    + "datasources: datasource=" + logicalKey);
        }
        PersistenceFlywayProperties.DataSourceConfig expected = explicitModules.getFirst()
                .migration().config().getDatasource();
        boolean consistent = explicitModules.stream()
                .map(module -> module.migration().config().getDatasource())
                .allMatch(candidate -> sameDataSourceConfiguration(expected, candidate));
        if (!consistent) {
            throw new IllegalStateException("Mango cold baseline datasource group has inconsistent connection "
                    + "configuration: datasource=" + logicalKey);
        }
    }

    private void validateColdBaselineRouting(Map<String, List<ColdBaselineModule>> groups) {
        List<ColdBaselineModule> explicitModules = groups.values().stream()
                .flatMap(List::stream)
                .filter(module -> hasExplicitDataSource(module.migration()))
                .toList();
        for (int leftIndex = 0; leftIndex < explicitModules.size(); leftIndex++) {
            ColdBaselineModule left = explicitModules.get(leftIndex);
            for (int rightIndex = leftIndex + 1; rightIndex < explicitModules.size(); rightIndex++) {
                ColdBaselineModule right = explicitModules.get(rightIndex);
                boolean sameConnection = sameDataSourceConfiguration(
                        left.migration().config().getDatasource(),
                        right.migration().config().getDatasource());
                if (sameConnection && !left.logicalDataSourceKey().equals(right.logicalDataSourceKey())) {
                    throw new IllegalStateException("Mango cold baseline assigns one connection to multiple logical "
                            + "datasources: modules=" + List.of(
                            left.migration().name(), right.migration().name()));
                }
            }
        }
    }

    private boolean hasExplicitDataSource(ModuleMigration module) {
        PersistenceFlywayProperties.DataSourceConfig datasource = module.config().getDatasource();
        return datasource != null && StringUtils.hasText(datasource.getUrl());
    }

    private boolean sameDataSourceConfiguration(PersistenceFlywayProperties.DataSourceConfig expected,
                                                PersistenceFlywayProperties.DataSourceConfig candidate) {
        return Objects.equals(trimToNull(expected.getUrl()), trimToNull(candidate.getUrl()))
                && Objects.equals(trimToNull(expected.getDriverClassName()),
                trimToNull(candidate.getDriverClassName()))
                && Objects.equals(trimToNull(expected.getUsername()), trimToNull(candidate.getUsername()))
                && Objects.equals(expected.getPassword(), candidate.getPassword());
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void assertIdempotentBaseline(Resource resource, String module) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                resource.getInputStream(), StandardCharsets.UTF_8))) {
            boolean markerFound = reader.lines()
                    .map(String::trim)
                    .anyMatch("-- mango:baseline-idempotent"::equals);
            if (!markerFound) {
                throw new IllegalStateException("Mango cold baseline must declare idempotent retry support: module="
                        + module + ", marker=-- mango:baseline-idempotent");
            }
        }
    }

    private String baselineVersion(Resource resource, String module) {
        String filename = resource.getFilename();
        if (!StringUtils.hasText(filename)
                || !filename.startsWith("B")
                || !filename.endsWith("__baseline.sql")) {
            throw new IllegalStateException("Mango cold baseline filename must match B{version}__baseline.sql: module="
                    + module + ", filename=" + filename);
        }
        String version = filename.substring(1, filename.length() - "__baseline.sql".length());
        if (!StringUtils.hasText(version)) {
            throw new IllegalStateException("Mango cold baseline version is empty: module=" + module);
        }
        try {
            MigrationVersion.fromVersion(version);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Mango cold baseline version is invalid: module=" + module
                    + ", version=" + version, exception);
        }
        return version;
    }

    private String sha256(Resource resource) throws IOException {
        try (InputStream input = resource.getInputStream();
             DigestInputStream digestInput = new DigestInputStream(input, sha256Digest())) {
            digestInput.transferTo(OutputStream.nullOutputStream());
            return HexFormat.of().formatHex(digestInput.getMessageDigest().digest());
        }
    }

    private String sha256(byte[] content) {
        return HexFormat.of().formatHex(sha256Digest().digest(content));
    }

    private MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private ResolvedDataSource resolveDataSource(DataSource defaultDataSource,
                                                 ModuleMigration module,
                                                 PersistenceDataSourceRegistry registry,
                                                 String dataSourceName) {
        if (StringUtils.hasText(dataSourceName)) {
            if (registry == null) {
                throw new IllegalStateException("Mango module datasource registry is unavailable: module="
                        + module.name() + ", datasource=" + dataSourceName);
            }
            return new ResolvedDataSource(registry.get(dataSourceName), false,
                    "registry:" + dataSourceName);
        }

        PersistenceFlywayProperties.DataSourceConfig datasource = module.config().getDatasource();
        if (datasource == null || !StringUtils.hasText(datasource.getUrl())) {
            String description = registry != null && StringUtils.hasText(registry.primaryName())
                    ? "registry-primary:" + registry.primaryName() : "default";
            return new ResolvedDataSource(defaultDataSource, false, description);
        }

        DataSourceBuilder<?> builder = DataSourceBuilder.create()
                .url(datasource.getUrl())
                .username(datasource.getUsername())
                .password(datasource.getPassword());
        if (StringUtils.hasText(datasource.getDriverClassName())) {
            builder.driverClassName(datasource.getDriverClassName());
        }
        return new ResolvedDataSource(builder.build(), true,
                "module-config:" + trimToNull(datasource.getLogicalName()));
    }

    private String resolveDataSourceDescription(ModuleMigration module,
                                                PersistenceDataSourceRegistry registry,
                                                String registeredDataSourceName) {
        if (StringUtils.hasText(registeredDataSourceName)) {
            return "registry:" + registeredDataSourceName;
        }
        PersistenceFlywayProperties.DataSourceConfig datasource = module.config().getDatasource();
        if (datasource != null && StringUtils.hasText(datasource.getUrl())) {
            String logicalName = trimToNull(datasource.getLogicalName());
            return logicalName == null ? "module-config" : "module-config:" + logicalName;
        }
        return registry != null && StringUtils.hasText(registry.primaryName())
                ? "registry-primary:" + registry.primaryName() : "default";
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

    private record ColdBaselineModule(ModuleMigration migration,
                                      Resource resource,
                                      String location,
                                      String version,
                                      String checksum,
                                      String registeredDataSourceName,
                                      String logicalDataSourceKey) {
    }

    private record ColdBaselineControl(String fingerprint, String status) {
    }

    private record ResolvedDataSource(DataSource dataSource, boolean closeAfterUse, String description) {
    }

    private record ResolvedLocations(List<String> locations, List<Path> tempDirectories) {
    }
}
