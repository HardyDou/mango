package io.mango.plugin.baseline;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BaselineMigrationCatalogTest {

    @TempDir
    Path directory;

    @Test
    void discoversModulesAndOrdersFlywayVersions() throws Exception {
        migration("beta", "V1__init.sql", "CREATE TABLE beta_record (id bigint primary key);");
        migration("alpha", "V10__later.sql", "ALTER TABLE alpha_record ADD note varchar(20);");
        migration("alpha", "V2__second.sql", "ALTER TABLE alpha_record ADD code varchar(20);");
        migration("alpha", "V1__init.sql", "CREATE TABLE alpha_record (id bigint primary key);");
        Files.createDirectories(directory.resolve("module/src/main/resources/META-INF/mango/resources"));
        Files.writeString(
                directory.resolve("module/src/main/resources/META-INF/mango/resources/ignored.sql"),
                "CREATE TABLE ignored_resource (id bigint primary key);");

        BaselineMigrationCatalog catalog = BaselineMigrationCatalog.discover(
                directory, new MavenProject(), Set.of());

        assertEquals(List.of("alpha", "beta"), List.copyOf(catalog.moduleNames()));
        assertEquals(
                List.of("V1__init.sql", "V2__second.sql", "V10__later.sql"),
                catalog.migrations("alpha").stream()
                        .map(BaselineMigrationCatalog.MigrationResource::fileName)
                        .toList());
        assertEquals("10", catalog.latestVersion("alpha"));
    }

    @Test
    void rejectsDuplicateSemanticVersions() throws Exception {
        migration("alpha", "V1__init.sql", "CREATE TABLE alpha_record (id bigint primary key);");
        migration("alpha", "V1.0__duplicate.sql", "ALTER TABLE alpha_record ADD code varchar(20);");

        MojoExecutionException exception = assertThrows(
                MojoExecutionException.class,
                () -> BaselineMigrationCatalog.discover(directory, new MavenProject(), Set.of()));

        assertTrue(exception.getMessage().contains("duplicate migration version"));
    }

    @Test
    void failsWhenIncludedModuleHasNoMigration() throws Exception {
        migration("alpha", "V1__init.sql", "CREATE TABLE alpha_record (id bigint primary key);");

        MojoExecutionException exception = assertThrows(
                MojoExecutionException.class,
                () -> BaselineMigrationCatalog.discover(
                        directory, new MavenProject(), Set.of("alpha", "missing")));

        assertTrue(exception.getMessage().contains("missing"));
    }

    @Test
    void discoversMigrationsFromRuntimeDependencyJar() throws Exception {
        Path dependency = dependencyJar(
                "db/migration/dependency-module/V1__init.sql",
                "CREATE TABLE dependency_record (id bigint primary key);");
        MavenProject project = projectWithArtifact(dependency);

        BaselineMigrationCatalog catalog = BaselineMigrationCatalog.discover(
                directory, project, Set.of("dependency-module"));

        assertEquals(Set.of("dependency-module"), catalog.moduleNames());
        assertEquals("1", catalog.latestVersion("dependency-module"));
    }

    @Test
    void rejectsDifferentSourceAndDependencyContentAtSameMigrationPath() throws Exception {
        migration("alpha", "V1__init.sql", "CREATE TABLE alpha_record (id bigint primary key);");
        Path dependency = dependencyJar(
                "db/migration/alpha/V1__init.sql",
                "CREATE TABLE alpha_record (id varchar(20) primary key);");

        MojoExecutionException exception = assertThrows(
                MojoExecutionException.class,
                () -> BaselineMigrationCatalog.discover(
                        directory, projectWithArtifact(dependency), Set.of("alpha")));

        assertTrue(exception.getMessage().contains("migration resource collision"));
    }

    @Test
    void ignoresMigrationsUnderTestAndIntegrationTestResources() throws Exception {
        migration("bootstrap", "V1__init_bootstrap_lifecycle.sql",
                "CREATE TABLE bootstrap_lifecycle (id bigint primary key);");
        testMigration("bootstrap", "V1__reserved_bootstrap_probe.sql",
                "CREATE TABLE bootstrap_probe (id bigint primary key);");
        testMigration("it-module", "V1__it_fixture.sql",
                "CREATE TABLE it_fixture (id bigint primary key);");

        BaselineMigrationCatalog catalog = BaselineMigrationCatalog.discover(
                directory, new MavenProject(), Set.of());

        assertEquals(List.of("bootstrap"), List.copyOf(catalog.moduleNames()));
        assertEquals(
                List.of("V1__init_bootstrap_lifecycle.sql"),
                catalog.migrations("bootstrap").stream()
                        .map(BaselineMigrationCatalog.MigrationResource::fileName)
                        .toList());
    }

    @Test
    void duplicateMainResourceVersionsReportBothPhysicalPaths() throws Exception {
        migration("alpha", "V1__init.sql", "CREATE TABLE alpha_record (id bigint primary key);");
        migration("alpha", "V1__other.sql", "CREATE TABLE alpha_other (id bigint primary key);");

        MojoExecutionException exception = assertThrows(
                MojoExecutionException.class,
                () -> BaselineMigrationCatalog.discover(directory, new MavenProject(), Set.of()));

        String message = exception.getMessage();
        assertTrue(message.contains("MANGO-BASELINE-009"));
        assertTrue(message.contains("V1__init.sql"));
        assertTrue(message.contains("V1__other.sql"));
        assertTrue(message.contains("/src/main/resources/db/migration/alpha/V1__init.sql"));
        assertTrue(message.contains("/src/main/resources/db/migration/alpha/V1__other.sql"));
    }

    @Test
    void ignoresMigrationsUnderIntegrationTestSourceSet() throws Exception {
        migration("payment", "V1__main_payment.sql", "CREATE TABLE main_payment (id bigint primary key);");
        migrationAt("it", "payment", "V1__it_payment.sql");

        BaselineMigrationCatalog catalog = BaselineMigrationCatalog.discover(
                directory, new MavenProject(), Set.of());

        assertEquals(
                List.of("V1__main_payment.sql"),
                catalog.migrations("payment").stream()
                        .map(BaselineMigrationCatalog.MigrationResource::fileName)
                        .toList());
    }

    @Test
    void ignoresMigrationsUnderIntegrationTestSourceSetVariant() throws Exception {
        migration("payment", "V1__main_payment_variant.sql", "CREATE TABLE main_payment_variant (id bigint primary key);");
        migrationAt("it-core", "payment", "V1__it_core.sql");

        BaselineMigrationCatalog catalog = BaselineMigrationCatalog.discover(
                directory, new MavenProject(), Set.of());

        assertEquals(
                List.of("V1__main_payment_variant.sql"),
                catalog.migrations("payment").stream()
                        .map(BaselineMigrationCatalog.MigrationResource::fileName)
                        .toList());
    }

    @Test
    void scansMigrationsUnderANonSourceTreeNamedIt() throws Exception {
        Path path = directory.resolve("it/resources/db/migration/payment/V1__loose_it.sql");
        Files.createDirectories(path.getParent());
        Files.writeString(path, "CREATE TABLE loose_it (id bigint primary key);");

        BaselineMigrationCatalog catalog = BaselineMigrationCatalog.discover(
                directory, new MavenProject(), Set.of());

        assertTrue(catalog.moduleNames().contains("payment"));
    }

    @Test
    void scansIntegrationTestSourceSetWhenItIsTheSearchRoot() throws Exception {
        migrationAt("it", "fixture", "V1__fixture.sql");

        BaselineMigrationCatalog catalog = BaselineMigrationCatalog.discover(
                directory.resolve("module/src/it").resolve("resources/db/migration"),
                new MavenProject(), Set.of());

        assertTrue(catalog.moduleNames().contains("fixture"));
    }

    private void migrationAt(String sourceSet, String module, String fileName) throws IOException {
        Path path = directory.resolve("module/src/" + sourceSet + "/resources/db/migration")
                .resolve(module)
                .resolve(fileName);
        Files.createDirectories(path.getParent());
        Files.writeString(path, "CREATE TABLE " + module + "_" + sourceSet.replace('-', '_') + " (id bigint primary key);");
    }

    private void migration(String module, String fileName, String sql) throws IOException {
        Path path = directory.resolve("module/src/main/resources/db/migration")
                .resolve(module)
                .resolve(fileName);
        Files.createDirectories(path.getParent());
        Files.writeString(path, sql);
    }

    private void testMigration(String module, String fileName, String sql) throws IOException {
        Path path = directory.resolve("module/src/test/resources/db/migration")
                .resolve(module)
                .resolve(fileName);
        Files.createDirectories(path.getParent());
        Files.writeString(path, sql);
    }

    private Path dependencyJar(String resource, String sql) throws IOException {
        Path jar = directory.resolve("dependency.jar");
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
            output.putNextEntry(new JarEntry(resource));
            output.write(sql.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            output.closeEntry();
        }
        return jar;
    }

    private static MavenProject projectWithArtifact(Path jar) {
        Artifact artifact = mock(Artifact.class);
        when(artifact.getFile()).thenReturn(jar.toFile());
        MavenProject project = new MavenProject();
        project.setArtifacts(Set.of(artifact));
        return project;
    }
}
