package io.mango.infra.persistence.starter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.infra.bootstrap.api.BootstrapExecutionContext;
import io.mango.infra.bootstrap.api.BootstrapPhase;
import io.mango.infra.bootstrap.api.BootstrapStep;
import io.mango.infra.bootstrap.api.BootstrapStepContributor;
import io.mango.infra.bootstrap.api.BootstrapStepResult;
import io.mango.infra.persistence.api.datasource.PersistenceModuleDataSourceResolver;
import io.mango.infra.persistence.starter.datasource.PersistenceDataSourceRegistry;
import io.mango.infra.persistence.starter.diagnostic.PersistenceMigrationState;
import io.mango.infra.persistence.starter.diagnostic.PersistenceModuleMigrationStatus;
import io.mango.infra.persistence.starter.diagnostic.PersistenceModuleMigrationStatusRegistry;
import org.springframework.core.io.Resource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class PersistenceBootstrapStepContributor implements BootstrapStepContributor {

    private final PersistenceFlywayProperties properties;
    private final PersistenceModuleMigrationStatusRegistry statusRegistry;
    private final PersistenceFlywayBootstrapExecutor executor;
    private final PersistenceDataSourceRegistry dataSourceRegistry;
    private final PersistenceModuleDataSourceResolver dataSourceResolver;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "The contributor intentionally retains Spring-managed configuration collaborators")
    public PersistenceBootstrapStepContributor(PersistenceFlywayProperties properties,
                                               PersistenceModuleMigrationStatusRegistry statusRegistry,
                                               PersistenceFlywayBootstrapExecutor executor,
                                               PersistenceDataSourceRegistry dataSourceRegistry,
                                               PersistenceModuleDataSourceResolver dataSourceResolver) {
        this.properties = properties;
        this.statusRegistry = statusRegistry;
        this.executor = executor;
        this.dataSourceRegistry = dataSourceRegistry;
        this.dataSourceResolver = dataSourceResolver;
    }

    @Override
    public List<BootstrapStep> contributeSteps() {
        if (properties.getColdBaseline() != null && properties.getColdBaseline().isEnabled()) {
            return List.of(new FlywayColdBaselineStep(), new FlywayExpandStep(), new FlywayContractStep());
        }
        return List.of(new FlywayExpandStep(), new FlywayContractStep());
    }

    private String fingerprintMaterial(String phase) {
        Map<String, String> modules = new TreeMap<>();
        properties.getModules().forEach((name, config) -> modules.put(name,
                config == null ? "default" : String.join("|",
                        Boolean.toString(config.isEnabled()),
                        Boolean.toString(config.isBaselineOnMigrate()),
                        Boolean.toString(config.isValidateOnMigrate()),
                        Boolean.toString(config.isOutOfOrder()),
                        Boolean.toString(config.isIgnoreMissingMigrations()),
                        String.valueOf(config.getHistoryTable()),
                        String.valueOf(config.getLocations()),
                        String.valueOf(config.getContractLocations()))));
        return "persistence-flyway-v1|" + phase + "|enabled=" + properties.isEnabled()
                + "|upgradeRoot=" + properties.getUpgradeRoot() + "|modules=" + modules
                + "|inventory=" + migrationInventory();
    }

    private List<String> migrationInventory() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            List<String> inventory = new ArrayList<>();
            collectMigrationInventory(resolver, "classpath*:db/migration/*/*.sql", inventory);
            collectMigrationInventory(resolver, "classpath*:db/migration-contract/*/*.sql", inventory);
            if (properties.getColdBaseline() != null && properties.getColdBaseline().isEnabled()) {
                inventory.addAll(coldBaselineInventory(resolver));
            }
            return inventory.stream().sorted(Comparator.naturalOrder()).toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Read Mango Flyway migration inventory failed", exception);
        }
    }

    private List<String> coldBaselineInventory(PathMatchingResourcePatternResolver resolver) throws IOException {
        List<String> inventory = new ArrayList<>();
        for (String module : coldBaselineModules(resolver)) {
            PersistenceFlywayProperties.ModuleConfig moduleConfig = properties.getModules().get(module);
            if (moduleConfig == null) {
                moduleConfig = new PersistenceFlywayProperties.ModuleConfig();
            }
            BaselineArtifact artifact = resolveBaselineArtifact(resolver, module, moduleConfig);
            inventory.add("cold-baseline=" + logicalDataSourceKey(module, moduleConfig)
                    + "|" + module + "|" + artifact.version() + "|" + artifact.checksum());
        }
        return inventory;
    }

    private List<String> coldBaselineModules(PathMatchingResourcePatternResolver resolver) throws IOException {
        if (!properties.getModules().isEmpty()) {
            return properties.getModules().entrySet().stream()
                    .filter(entry -> entry.getValue() == null || entry.getValue().isEnabled())
                    .map(Map.Entry::getKey)
                    .sorted()
                    .toList();
        }
        Set<String> modules = new java.util.TreeSet<>();
        for (Resource resource : resolver.getResources("classpath*:db/migration/*/*.sql")) {
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
        return List.copyOf(modules);
    }

    private BaselineArtifact resolveBaselineArtifact(PathMatchingResourcePatternResolver resolver,
                                                     String module,
                                                     PersistenceFlywayProperties.ModuleConfig moduleConfig)
            throws IOException {
        PersistenceFlywayProperties.BaselineConfig baseline = moduleConfig.getBaseline();
        Resource resource;
        if (baseline != null && StringUtils.hasText(baseline.getLocation())) {
            resource = new DefaultResourceLoader().getResource(baseline.getLocation().trim());
            if (!resource.exists() || !resource.isReadable()) {
                throw new IllegalStateException("Mango cold baseline SQL is not readable: module=" + module
                        + ", location=" + baseline.getLocation());
            }
        } else {
            String pattern = "classpath*:db/baseline/" + module + "/B*__baseline.sql";
            Resource[] resources = resolver.getResources(pattern);
            if (resources.length != 1) {
                throw new IllegalStateException("Mango cold baseline requires exactly one current SQL per module: "
                        + "module=" + module + ", pattern=" + pattern + ", count=" + resources.length);
            }
            resource = resources[0];
        }
        String version = baseline != null && StringUtils.hasText(baseline.getVersion())
                ? baseline.getVersion().trim() : baselineVersion(resource, module);
        try (InputStream input = resource.getInputStream()) {
            return new BaselineArtifact(version, sha256(input.readAllBytes()));
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
        return version;
    }

    private String logicalDataSourceKey(String module,
                                        PersistenceFlywayProperties.ModuleConfig moduleConfig) {
        if (dataSourceResolver != null) {
            String registeredName = dataSourceResolver.resolveDataSource(module)
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .orElse(null);
            if (registeredName != null) {
                if (dataSourceRegistry == null) {
                    throw new IllegalStateException("Mango module datasource resolver requires a datasource registry: "
                            + "module=" + module + ", datasource=" + registeredName);
                }
                return registeredName;
            }
        }
        PersistenceFlywayProperties.DataSourceConfig datasource = moduleConfig.getDatasource();
        if (datasource != null && StringUtils.hasText(datasource.getUrl())) {
            if (!StringUtils.hasText(datasource.getLogicalName())) {
                throw new IllegalStateException("Mango cold baseline explicit datasource requires logical-name: module="
                        + module);
            }
            return datasource.getLogicalName().trim();
        }
        if (dataSourceRegistry != null && StringUtils.hasText(dataSourceRegistry.primaryName())) {
            return dataSourceRegistry.primaryName().trim();
        }
        return "default";
    }

    private final class FlywayColdBaselineStep implements BootstrapStep {

        @Override
        public String code() {
            return "FLYWAY_COLD_BASELINE";
        }

        @Override
        public BootstrapPhase phase() {
            return BootstrapPhase.EXPAND;
        }

        @Override
        public String fingerprintMaterial() {
            return PersistenceBootstrapStepContributor.this.fingerprintMaterial("cold-baseline");
        }

        @Override
        public BootstrapStepResult execute(BootstrapExecutionContext context) {
            PersistenceFlywayBootstrapExecutor.MigrationSummary summary = executor.applyColdBaseline();
            return new BootstrapStepResult("Flyway cold baseline applied", Map.of(
                    "modules", summary.moduleCount(),
                    "snapshotMigrations", summary.migrationCount()));
        }
    }

    private void collectMigrationInventory(PathMatchingResourcePatternResolver resolver,
                                           String pattern,
                                           List<String> inventory) throws IOException {
        for (Resource resource : resolver.getResources(pattern)) {
            String logicalPath = logicalMigrationPath(resource);
            try (InputStream input = resource.getInputStream()) {
                inventory.add(logicalPath + "=" + sha256(input.readAllBytes()));
            }
        }
    }

    private String logicalMigrationPath(Resource resource) throws IOException {
        String url = resource.getURL().toString();
        int contract = url.indexOf("/db/migration-contract/");
        int expand = url.indexOf("/db/migration/");
        int start = contract >= 0 ? contract + 1 : expand >= 0 ? expand + 1 : -1;
        return start >= 0 ? url.substring(start) : resource.getFilename();
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private BootstrapStepResult verifyMigrations(String phase) {
        List<PersistenceModuleMigrationStatus> statuses = statusRegistry.list().stream().toList();
        List<String> invalid = statuses.stream()
                .filter(status -> status.state() != PersistenceMigrationState.APPLIED || status.pendingCount() != 0)
                .map(status -> status.module() + ":" + status.state() + ":pending=" + status.pendingCount())
                .toList();
        if (!invalid.isEmpty()) {
            throw new IllegalStateException("Mango Flyway bootstrap verification failed: " + invalid);
        }
        return new BootstrapStepResult("Flyway " + phase + " verified",
                Map.of("modules", statuses.size()));
    }

    private BootstrapStepResult migrateAndVerify(BootstrapPhase phase) {
        PersistenceFlywayBootstrapExecutor.MigrationSummary summary = executor.migrate(phase);
        BootstrapStepResult verified = verifyMigrations(phase.name().toLowerCase());
        return new BootstrapStepResult(verified.summary(), Map.of(
                "modules", summary.moduleCount(),
                "migrations", summary.migrationCount(),
                "phase", summary.phase()));
    }

    private final class FlywayExpandStep implements BootstrapStep {

        @Override
        public String code() {
            return "FLYWAY_EXPAND";
        }

        @Override
        public BootstrapPhase phase() {
            return BootstrapPhase.EXPAND;
        }

        @Override
        public Set<String> dependencies() {
            return properties.getColdBaseline() != null && properties.getColdBaseline().isEnabled()
                    ? Set.of("FLYWAY_COLD_BASELINE") : Set.of();
        }

        @Override
        public String fingerprintMaterial() {
            return PersistenceBootstrapStepContributor.this.fingerprintMaterial("expand");
        }

        @Override
        public BootstrapStepResult execute(BootstrapExecutionContext context) {
            return migrateAndVerify(BootstrapPhase.EXPAND);
        }
    }

    private final class FlywayContractStep implements BootstrapStep {

        @Override
        public String code() {
            return "FLYWAY_CONTRACT";
        }

        @Override
        public BootstrapPhase phase() {
            return BootstrapPhase.FINALIZE;
        }

        @Override
        public Set<String> dependencies() {
            return Set.of("FLYWAY_EXPAND");
        }

        @Override
        public String fingerprintMaterial() {
            return PersistenceBootstrapStepContributor.this.fingerprintMaterial("contract");
        }

        @Override
        public BootstrapStepResult execute(BootstrapExecutionContext context) {
            return migrateAndVerify(BootstrapPhase.FINALIZE);
        }
    }

    private record BaselineArtifact(String version, String checksum) {
    }
}
