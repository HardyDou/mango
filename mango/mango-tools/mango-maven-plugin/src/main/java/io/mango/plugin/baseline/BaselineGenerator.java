package io.mango.plugin.baseline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.FileVisitResult;
import java.nio.file.SimpleFileVisitor;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class BaselineGenerator {

    static final String GENERATOR_FORMAT_VERSION = "1";
    private static final String ADMIN_DATABASE = "mysql";
    private static final String MANIFEST_PATH = "META-INF/mango/baseline-manifest.json";
    private static final String DATASOURCE_GROUP_PATTERN = "[a-z][a-z0-9-]{0,31}";
    private static final String DETERMINISM_DATABASE_SEPARATOR = "_determinism_";
    private static final int RUN_ID_LENGTH = 10;
    private static final int MYSQL_IDENTIFIER_MAX_LENGTH = 64;
    private static final int DIFFERENCE_PREVIEW_LENGTH = 320;

    private final BaselineGenerationSettings settings;
    private final BaselineMigrationCatalog catalog;
    private final BaselineObjectOwnership ownership;
    private final Log log;
    private final MySqlBaselineStore store;

    BaselineGenerator(
            BaselineGenerationSettings settings,
            BaselineMigrationCatalog catalog,
            Log log) throws MojoExecutionException {
        this.settings = settings;
        this.catalog = catalog;
        this.ownership = BaselineObjectOwnership.analyze(catalog);
        this.log = log;
        this.store = new MySqlBaselineStore(
                settings.jdbcUrl(), settings.username(), settings.password());
    }

    GenerationResult generate() throws MojoExecutionException {
        long startedAt = System.nanoTime();
        List<String> moduleOrder = validateModuleOrder();
        Map<String, List<String>> groups = groupModules(moduleOrder);
        validateResourceBaselineTopology(groups);
        Path staging = createStagingDirectory();
        Path migrationExtraction = createTemporaryDirectory("mango-baseline-migrations-");
        Map<String, TemporaryDatabases> databases = new LinkedHashMap<>();
        try {
            MySqlBaselineStore.DatabaseIdentity databaseIdentity =
                    store.databaseIdentity(ADMIN_DATABASE);
            store.validateSchemaDefaults(ADMIN_DATABASE, settings.schemaDefaults());
            prepareDatabases(groups, migrationExtraction, databases);
            materializeResourceBaselines(groups, databases);
            canonicalizeRuntimeAuditTimestamps(databases);
            List<ModuleManifest> modules = generateAndVerifyBaselines(groups, databases, staging);
            String generationFingerprint = generationFingerprint(databaseIdentity, modules);
            writeManifest(staging, databaseIdentity, generationFingerprint, modules, groups);
            BaselineArtifactVerifier.verify(staging);
            installGeneratedResources(staging, settings.outputDirectory());
            Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);
            logResult(modules.size(), groups.size(), generationFingerprint, elapsed);
            return new GenerationResult(modules.size(), groups.size(), generationFingerprint, elapsed);
        } catch (IOException exception) {
            throw new MojoExecutionException(
                    "MANGO-BASELINE-020 failed to write generated baseline resources", exception);
        } finally {
            deleteQuietly(staging);
            deleteQuietly(migrationExtraction);
            cleanupWorkspaceDatabases(databases);
        }
    }

    private void materializeResourceBaselines(
            Map<String, List<String>> groups,
            Map<String, TemporaryDatabases> databases) throws MojoExecutionException {
        if (!settings.resourceBaselineEnabled()) {
            return;
        }
        TemporaryDatabases temporary = databases.values().iterator().next();
        ResourceBaselineApplicationRunner runner = new ResourceBaselineApplicationRunner(
                settings.resourceBaseline(), settings, store, log);
        runner.materialize(temporary.replay());
        runner.materialize(temporary.determinism());
    }

    private void validateResourceBaselineTopology(Map<String, List<String>> groups)
            throws MojoExecutionException {
        if (settings.resourceBaselineEnabled() && groups.size() != 1) {
            throw new MojoExecutionException(
                    "MANGO-BASELINE-048 Resource baseline generation currently requires one datasource group"
                            + "; groups=" + groups.keySet());
        }
    }

    private void canonicalizeRuntimeAuditTimestamps(
            Map<String, TemporaryDatabases> databases) throws MojoExecutionException {
        for (TemporaryDatabases temporary : databases.values()) {
            store.canonicalizeRuntimeAuditTimestamps(temporary.replay());
            store.canonicalizeRuntimeAuditTimestamps(temporary.determinism());
        }
    }

    private void prepareDatabases(
            Map<String, List<String>> groups,
            Path migrationExtraction,
            Map<String, TemporaryDatabases> databases) throws MojoExecutionException {
        for (Map.Entry<String, List<String>> group : groups.entrySet()) {
            TemporaryDatabases temporary = temporaryDatabases(group.getKey());
            databases.put(group.getKey(), temporary);
            store.createDatabase(ADMIN_DATABASE, temporary.replay(), settings.schemaDefaults());
            store.createDatabase(ADMIN_DATABASE, temporary.determinism(), settings.schemaDefaults());
            store.createDatabase(ADMIN_DATABASE, temporary.verify(), settings.schemaDefaults());
            migrateVersions(group.getValue(), temporary.replay(), migrationExtraction);
            migrateVersions(group.getValue(), temporary.determinism(), migrationExtraction);
        }
    }

    private List<ModuleManifest> generateAndVerifyBaselines(
            Map<String, List<String>> groups,
            Map<String, TemporaryDatabases> databases,
            Path staging) throws MojoExecutionException, IOException {
        List<ModuleManifest> modules = new ArrayList<>();
        for (Map.Entry<String, List<String>> group : groups.entrySet()) {
            String groupName = group.getKey();
            List<String> groupModules = group.getValue();
            TemporaryDatabases temporary = databases.get(groupName);
            Set<String> groupModuleSet = Set.copyOf(groupModules);
            modules.addAll(generateModuleBaselines(
                    groupName, groupModules, groupModuleSet, temporary.replay(), staging));
            MySqlBaselineStore.SchemaSnapshot replay =
                    store.snapshot(temporary.replay(), groupModuleSet, ownership);
            verifyDeterministicReplay(groupName, groupModuleSet, temporary);
            verifyGeneratedBaselines(groupName, groupModules, groupModuleSet, temporary, staging, replay);
        }
        return List.copyOf(modules);
    }

    private List<ModuleManifest> generateModuleBaselines(
            String groupName,
            List<String> modules,
            Set<String> groupModuleSet,
            String replayDatabase,
            Path staging) throws MojoExecutionException, IOException {
        List<ModuleManifest> manifests = new ArrayList<>();
        for (String module : modules) {
            String version = catalog.latestVersion(module);
            Path baseline = staging.resolve("db/baseline")
                    .resolve(module)
                    .resolve("B" + version + "__baseline.sql");
            writeBaseline(baseline,
                    store.dumpModule(replayDatabase, module, groupModuleSet, ownership));
            manifests.add(new ModuleManifest(
                    module,
                    groupName,
                    version,
                    "flyway_schema_history_" + sqlIdentifier(module),
                    "db/baseline/" + module + "/" + baseline.getFileName(),
                    sha256(Files.readAllBytes(baseline)),
                    catalog.fingerprint(module)));
        }
        return manifests;
    }

    private void verifyDeterministicReplay(
            String groupName,
            Set<String> groupModules,
            TemporaryDatabases temporary) throws MojoExecutionException {
        MySqlBaselineStore.SchemaSnapshot deterministic =
                store.determinismSnapshot(temporary.determinism(), groupModules, ownership);
        MySqlBaselineStore.SchemaSnapshot comparableReplay =
                store.determinismSnapshot(temporary.replay(), groupModules, ownership);
        if (!comparableReplay.equals(deterministic)) {
            String cause = settings.resourceBaselineEnabled()
                    ? "migrations and portable Resource handlers are not deterministic"
                    : "migrations are not deterministic across clean replays";
            throw new MojoExecutionException(
                    "MANGO-BASELINE-040 " + cause + "; datasourceGroup=" + groupName + "; difference="
                            + snapshotDifference(comparableReplay, deterministic));
        }
    }

    private void verifyGeneratedBaselines(
            String groupName,
            List<String> modules,
            Set<String> groupModules,
            TemporaryDatabases temporary,
            Path staging,
            MySqlBaselineStore.SchemaSnapshot replay) throws MojoExecutionException {
        migrateBaselines(modules, temporary.verify(), staging, false);
        migrateBaselines(modules, temporary.verify(), staging, true);
        MySqlBaselineStore.SchemaSnapshot verified =
                store.snapshot(temporary.verify(), groupModules, ownership);
        if (!replay.equals(verified)) {
            throw new MojoExecutionException(
                    "MANGO-BASELINE-019 generated baseline is not equivalent to V migrations"
                            + "; datasourceGroup=" + groupName + "; difference="
                            + snapshotDifference(replay, verified));
        }
    }

    private void logResult(
            int moduleCount,
            int groupCount,
            String fingerprint,
            Duration elapsed) {
        log.info("Mango baselines generated and verified: modules=" + moduleCount
                + ", datasourceGroups=" + groupCount
                + ", elapsedMs=" + elapsed.toMillis()
                + ", fingerprint=" + fingerprint);
    }

    private void cleanupWorkspaceDatabases(Map<String, TemporaryDatabases> databases) {
        if (!settings.keepSchemas()) {
            cleanupDatabases(databases);
        } else if (!databases.isEmpty()) {
            log.warn("Temporary baseline databases were retained by configuration: " + databases);
        }
    }

    private void migrateVersions(List<String> modules, String database, Path extractionRoot)
            throws MojoExecutionException {
        for (String module : modules) {
            Path location = catalog.extractModule(module, extractionRoot.resolve(database));
            migrate(database, module, location, "V", catalog.latestVersion(module), false);
        }
    }

    private void migrateBaselines(
            List<String> modules,
            String database,
            Path staging,
            boolean reentry)
            throws MojoExecutionException {
        for (String module : modules) {
            Path location = staging.resolve("db/baseline").resolve(module);
            migrate(
                    database,
                    module,
                    location,
                    "B",
                    catalog.latestVersion(module),
                    true,
                    reentry);
        }
    }

    private void migrate(
            String database,
            String module,
            Path location,
            String prefix,
            String targetVersion,
            boolean verifyBaseline) throws MojoExecutionException {
        migrate(database, module, location, prefix, targetVersion, verifyBaseline, false);
    }

    private void migrate(
            String database,
            String module,
            Path location,
            String prefix,
            String targetVersion,
            boolean verifyBaseline,
            boolean reentry) throws MojoExecutionException {
        String historyTable;
        if (reentry) {
            historyTable = "flyway_baseline_reentry_" + sqlIdentifier(module);
        } else if (verifyBaseline) {
            historyTable = "flyway_baseline_verify_" + sqlIdentifier(module);
        } else {
            historyTable = "flyway_schema_history_" + sqlIdentifier(module);
        }
        try {
            Flyway flyway = Flyway.configure()
                    .dataSource(store.databaseUrl(database), settings.username(), settings.password())
                    .locations("filesystem:" + location.toAbsolutePath())
                    .table(historyTable)
                    .baselineOnMigrate(true)
                    .baselineVersion(MigrationVersion.fromVersion("0"))
                    .sqlMigrationPrefix(prefix)
                    .target(MigrationVersion.fromVersion(targetVersion))
                    .outOfOrder(false)
                    .validateMigrationNaming(true)
                    .failOnMissingLocations(true)
                    .load();
            flyway.migrate();
        } catch (RuntimeException exception) {
            throw new MojoExecutionException(
                    "MANGO-BASELINE-021 Flyway "
                            + (reentry
                                    ? "baseline reentry verification"
                                    : verifyBaseline ? "baseline verification" : "migration replay")
                            + " failed; module=" + module + ", databaseGroupSchema=" + database,
                    exception);
        }
    }

    private List<String> validateModuleOrder() throws MojoExecutionException {
        Set<String> discovered = new LinkedHashSet<>(catalog.moduleNames());
        if (settings.moduleOrder().isEmpty()) {
            return List.copyOf(discovered);
        }
        Set<String> configured = new LinkedHashSet<>(settings.moduleOrder());
        if (configured.size() != settings.moduleOrder().size()) {
            throw new MojoExecutionException(
                    "MANGO-BASELINE-022 moduleOrder contains duplicate modules: "
                            + settings.moduleOrder());
        }
        if (!configured.equals(discovered)) {
            Set<String> missing = new LinkedHashSet<>(discovered);
            missing.removeAll(configured);
            Set<String> unknown = new LinkedHashSet<>(configured);
            unknown.removeAll(discovered);
            throw new MojoExecutionException(
                    "MANGO-BASELINE-023 moduleOrder must contain every discovered module exactly once"
                            + "; missing=" + missing + ", unknown=" + unknown);
        }
        return List.copyOf(settings.moduleOrder());
    }

    private Map<String, List<String>> groupModules(List<String> moduleOrder)
            throws MojoExecutionException {
        Set<String> unknown = new LinkedHashSet<>(settings.moduleGroups().keySet());
        unknown.removeAll(moduleOrder);
        if (!unknown.isEmpty()) {
            throw new MojoExecutionException(
                    "MANGO-BASELINE-024 moduleGroups references unknown modules: " + unknown);
        }
        Map<String, List<String>> groups = new LinkedHashMap<>();
        for (String module : moduleOrder) {
            String group = settings.moduleGroups().getOrDefault(module, "default");
            if (!group.matches(DATASOURCE_GROUP_PATTERN)) {
                throw new MojoExecutionException(
                        "MANGO-BASELINE-025 invalid datasource group for module " + module
                                + ": " + group);
            }
            groups.computeIfAbsent(group, ignored -> new ArrayList<>()).add(module);
        }
        return groups;
    }

    private TemporaryDatabases temporaryDatabases(String group) {
        String runId = UUID.randomUUID().toString().replace("-", "")
                .substring(0, RUN_ID_LENGTH);
        String stem = sqlIdentifier(settings.schemaPrefix() + "_" + group);
        int maximumStemLength = MYSQL_IDENTIFIER_MAX_LENGTH
                - DETERMINISM_DATABASE_SEPARATOR.length() - runId.length();
        if (stem.length() > maximumStemLength) {
            stem = stem.substring(0, maximumStemLength);
        }
        return new TemporaryDatabases(
                stem + "_replay_" + runId,
                stem + "_determinism_" + runId,
                stem + "_verify_" + runId);
    }

    private String generationFingerprint(
            MySqlBaselineStore.DatabaseIdentity databaseIdentity,
            List<ModuleManifest> modules) throws MojoExecutionException {
        MessageDigest digest = digest();
        update(digest, "format=" + GENERATOR_FORMAT_VERSION);
        update(digest, "database=" + databaseIdentity.product() + ":" + databaseIdentity.version());
        update(digest, "characterSet=" + settings.schemaDefaults().characterSet());
        update(digest, "collation=" + settings.schemaDefaults().collation());
        for (ModuleManifest module : modules) {
            update(digest, module.module());
            update(digest, module.datasourceGroup());
            update(digest, module.version());
            update(digest, module.migrationFingerprint());
            update(digest, module.baselineSha256());
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private void writeManifest(
            Path staging,
            MySqlBaselineStore.DatabaseIdentity databaseIdentity,
            String fingerprint,
            List<ModuleManifest> modules,
            Map<String, List<String>> groups) throws IOException {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("formatVersion", GENERATOR_FORMAT_VERSION);
        manifest.put("generator", "mango:baseline-generate");
        manifest.put("databaseProduct", databaseIdentity.product());
        manifest.put("databaseVersion", databaseIdentity.version());
        manifest.put("targetCharacterSet", settings.schemaDefaults().characterSet());
        manifest.put("targetCollation", settings.schemaDefaults().collation());
        manifest.put("generationFingerprint", fingerprint);
        manifest.put("datasourceGroups", groups);
        manifest.put("modules", modules);
        Path manifestFile = staging.resolve(MANIFEST_PATH);
        Files.createDirectories(manifestFile.getParent());
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(manifestFile.toFile(), manifest);
        Files.writeString(manifestFile,
                Files.readString(manifestFile, StandardCharsets.UTF_8) + "\n",
                StandardCharsets.UTF_8);
    }

    private static void writeBaseline(Path baseline, String sql) throws IOException {
        Files.createDirectories(baseline.getParent());
        Files.writeString(baseline, sql, StandardCharsets.UTF_8);
    }

    private static void installGeneratedResources(Path staging, Path output)
            throws IOException {
        Path stagedBaselines = staging.resolve("db/baseline");
        Path stagedManifest = staging.resolve(MANIFEST_PATH);
        Path outputBaselines = output.resolve("db/baseline");
        Path outputManifest = output.resolve(MANIFEST_PATH);
        Path backup = Files.createTempDirectory(
                output.toAbsolutePath().getParent(), "mango-baseline-backup-");
        Path backupBaselines = backup.resolve("db/baseline");
        Path backupManifest = backup.resolve(MANIFEST_PATH);
        Files.createDirectories(output);
        try {
            moveIfExists(outputBaselines, backupBaselines);
            moveIfExists(outputManifest, backupManifest);
            Files.createDirectories(outputBaselines.getParent());
            Files.createDirectories(outputManifest.getParent());
            move(stagedBaselines, outputBaselines);
            move(stagedManifest, outputManifest);
            deleteRecursively(staging);
            deleteRecursively(backup);
        } catch (IOException installFailure) {
            try {
                deleteRecursively(outputBaselines);
                Files.deleteIfExists(outputManifest);
                moveIfExists(backupBaselines, outputBaselines);
                moveIfExists(backupManifest, outputManifest);
            } catch (IOException rollbackFailure) {
                installFailure.addSuppressed(rollbackFailure);
            }
            throw installFailure;
        } finally {
            deleteQuietly(backup);
        }
    }

    private static void moveIfExists(Path source, Path target) throws IOException {
        if (!Files.exists(source)) {
            return;
        }
        Files.createDirectories(target.getParent());
        move(source, target);
    }

    private void cleanupDatabases(Map<String, TemporaryDatabases> databases) {
        for (TemporaryDatabases temporary : databases.values()) {
            dropQuietly(temporary.verify());
            dropQuietly(temporary.determinism());
            dropQuietly(temporary.replay());
        }
    }

    private void dropQuietly(String database) {
        try {
            store.dropDatabase(ADMIN_DATABASE, database);
        } catch (MojoExecutionException exception) {
            log.warn("Failed to clean temporary baseline database " + database + ": "
                    + exception.getMessage());
        }
    }

    private static Path createTemporaryDirectory(String prefix) throws MojoExecutionException {
        try {
            return Files.createTempDirectory(prefix);
        } catch (IOException exception) {
            throw new MojoExecutionException(
                    "MANGO-BASELINE-026 failed to create temporary directory", exception);
        }
    }

    private Path createStagingDirectory() throws MojoExecutionException {
        try {
            Path parent = settings.outputDirectory().toAbsolutePath().getParent();
            Files.createDirectories(parent);
            return Files.createTempDirectory(parent, "mango-baseline-staging-");
        } catch (IOException exception) {
            throw new MojoExecutionException(
                    "MANGO-BASELINE-027 failed to create generated-resource staging directory",
                    exception);
        }
    }

    private static void move(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target);
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            deleteRecursively(path);
        } catch (IOException ignored) {
            // Best-effort cleanup must not hide the generation failure.
        }
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (path == null || !Files.exists(path)) {
            return;
        }
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes)
                    throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path directory, IOException exception)
                    throws IOException {
                if (exception != null) {
                    throw exception;
                }
                Files.delete(directory);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static String sqlIdentifier(String value) {
        String normalized = value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return normalized.isBlank() ? "baseline" : normalized;
    }

    private static String sha256(byte[] content) throws MojoExecutionException {
        return HexFormat.of().formatHex(digest().digest(content));
    }

    private static MessageDigest digest() throws MojoExecutionException {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new MojoExecutionException("SHA-256 is unavailable", exception);
        }
    }

    private static void update(MessageDigest digest, String value) {
        digest.update(value.getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
    }

    private static String snapshotDifference(
            MySqlBaselineStore.SchemaSnapshot expected,
            MySqlBaselineStore.SchemaSnapshot actual) {
        Set<String> tableKeys = new LinkedHashSet<>(expected.tables().keySet());
        tableKeys.addAll(actual.tables().keySet());
        for (String tableKey : tableKeys) {
            MySqlBaselineStore.TableSnapshot expectedTable = expected.tables().get(tableKey);
            MySqlBaselineStore.TableSnapshot actualTable = actual.tables().get(tableKey);
            String tablePath = "table:" + tableKey;
            String difference = valueDifference(tablePath, expectedTable != null, actualTable != null);
            if (difference != null) {
                return difference;
            }
            difference = tableDifference(tablePath, expectedTable, actualTable);
            if (difference != null) {
                return difference;
            }
        }
        Set<String> definitionKeys = new LinkedHashSet<>(expected.definitions().keySet());
        definitionKeys.addAll(actual.definitions().keySet());
        for (String key : definitionKeys) {
            String expectedValue = expected.definitions().get(key);
            String actualValue = actual.definitions().get(key);
            if (!java.util.Objects.equals(expectedValue, actualValue)) {
                return key + " expected=" + abbreviate(expectedValue)
                        + " actual=" + abbreviate(actualValue);
            }
        }
        Set<String> dataKeys = new LinkedHashSet<>(expected.data().keySet());
        dataKeys.addAll(actual.data().keySet());
        for (String key : dataKeys) {
            List<List<String>> expectedRows = expected.data().get(key);
            List<List<String>> actualRows = actual.data().get(key);
            if (!java.util.Objects.equals(expectedRows, actualRows)) {
                return "data:" + key + " expected=" + abbreviate(String.valueOf(expectedRows))
                        + " actual=" + abbreviate(String.valueOf(actualRows));
            }
        }
        return "snapshot record differs without a field-level difference";
    }

    private static String tableDifference(
            String path,
            MySqlBaselineStore.TableSnapshot expected,
            MySqlBaselineStore.TableSnapshot actual) {
        String difference = valueDifference(path + ".engine", expected.engine(), actual.engine());
        if (difference == null) {
            difference = valueDifference(
                    path + ".characterSet", expected.characterSet(), actual.characterSet());
        }
        if (difference == null) {
            difference = valueDifference(path + ".collation", expected.collation(), actual.collation());
        }
        if (difference == null) {
            difference = valueDifference(path + ".rowFormat", expected.rowFormat(), actual.rowFormat());
        }
        if (difference == null) {
            difference = valueDifference(
                    path + ".createOptions", expected.createOptions(), actual.createOptions());
        }
        if (difference == null) {
            difference = valueDifference(path + ".comment", expected.comment(), actual.comment());
        }
        if (difference != null) {
            return difference;
        }
        difference = columnDifference(path, expected.columns(), actual.columns());
        if (difference != null) {
            return difference;
        }
        difference = indexDifference(path, expected.indexes(), actual.indexes());
        if (difference != null) {
            return difference;
        }
        return constraintDifference(path, expected.constraints(), actual.constraints());
    }

    private static String columnDifference(
            String tablePath,
            Map<String, MySqlBaselineStore.ColumnSnapshot> expected,
            Map<String, MySqlBaselineStore.ColumnSnapshot> actual) {
        Set<String> keys = new LinkedHashSet<>(expected.keySet());
        keys.addAll(actual.keySet());
        for (String key : keys) {
            MySqlBaselineStore.ColumnSnapshot expectedColumn = expected.get(key);
            MySqlBaselineStore.ColumnSnapshot actualColumn = actual.get(key);
            String path = tablePath + ".column:" + key;
            String difference = valueDifference(path, expectedColumn, actualColumn);
            if (difference != null) {
                return difference;
            }
        }
        return null;
    }

    private static String indexDifference(
            String tablePath,
            Map<String, MySqlBaselineStore.IndexSnapshot> expected,
            Map<String, MySqlBaselineStore.IndexSnapshot> actual) {
        Set<String> keys = new LinkedHashSet<>(expected.keySet());
        keys.addAll(actual.keySet());
        for (String key : keys) {
            MySqlBaselineStore.IndexSnapshot expectedIndex = expected.get(key);
            MySqlBaselineStore.IndexSnapshot actualIndex = actual.get(key);
            String path = tablePath + ".index:" + key;
            String difference = valueDifference(path, expectedIndex, actualIndex);
            if (difference != null) {
                return difference;
            }
        }
        return null;
    }

    private static String constraintDifference(
            String tablePath,
            Map<String, MySqlBaselineStore.ConstraintSnapshot> expected,
            Map<String, MySqlBaselineStore.ConstraintSnapshot> actual) {
        Set<String> keys = new LinkedHashSet<>(expected.keySet());
        keys.addAll(actual.keySet());
        for (String key : keys) {
            MySqlBaselineStore.ConstraintSnapshot expectedConstraint = expected.get(key);
            MySqlBaselineStore.ConstraintSnapshot actualConstraint = actual.get(key);
            String path = tablePath + ".constraint:" + key;
            String difference = valueDifference(path, expectedConstraint, actualConstraint);
            if (difference != null) {
                return difference;
            }
        }
        return null;
    }

    private static String valueDifference(String path, Object expected, Object actual) {
        if (java.util.Objects.equals(expected, actual)) {
            return null;
        }
        return path + " expected=" + abbreviate(String.valueOf(expected))
                + " actual=" + abbreviate(String.valueOf(actual));
    }

    private static String abbreviate(String value) {
        if (value == null || value.length() <= DIFFERENCE_PREVIEW_LENGTH) {
            return value;
        }
        return value.substring(0, DIFFERENCE_PREVIEW_LENGTH) + "...";
    }

    record GenerationResult(
            int moduleCount,
            int datasourceGroupCount,
            String generationFingerprint,
            Duration elapsed) {
    }

    record ModuleManifest(
            String module,
            String datasourceGroup,
            String version,
            String historyTable,
            String baselineResource,
            String baselineSha256,
            String migrationFingerprint) {
    }

    private record TemporaryDatabases(String replay, String determinism, String verify) {
    }
}
