package io.mango.plugin.baseline;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaselineObjectOwnershipTest {

    @TempDir
    Path directory;

    @Test
    void ignoresCommentsAndStringLiteralsWhileCollectingOwnership() throws Exception {
        migration("alpha", "V1__init.sql", """
                -- CREATE TABLE ignored_comment (id bigint primary key);
                CREATE TABLE IF NOT EXISTS `alpha_record` (id bigint primary key, note varchar(100));
                INSERT INTO alpha_record (id, note) VALUES (1, 'CREATE TABLE ignored_string (id int)');
                CREATE VIEW alpha_record_view AS SELECT id FROM alpha_record;
                """);
        BaselineMigrationCatalog catalog = BaselineMigrationCatalog.discover(
                directory, new MavenProject(), Set.of());

        BaselineObjectOwnership ownership = BaselineObjectOwnership.analyze(catalog);

        assertEquals("alpha", ownership.tableOwner("ALPHA_RECORD"));
        assertEquals("alpha", ownership.viewOwner("alpha_record_view"));
        assertNull(ownership.tableOwner("ignored_comment"));
        assertNull(ownership.tableOwner("ignored_string"));
    }

    @Test
    void rejectsCrossModuleTableOwnership() throws Exception {
        migration("alpha", "V1__init.sql", "CREATE TABLE shared_record (id bigint primary key);");
        migration("beta", "V1__init.sql", "CREATE TABLE shared_record (id bigint primary key);");
        BaselineMigrationCatalog catalog = BaselineMigrationCatalog.discover(
                directory, new MavenProject(), Set.of());

        MojoExecutionException exception = assertThrows(
                MojoExecutionException.class,
                () -> BaselineObjectOwnership.analyze(catalog));

        assertTrue(exception.getMessage().contains("cross-module table ownership conflict"));
    }

    private void migration(String module, String fileName, String sql) throws Exception {
        Path path = directory.resolve("module/src/main/resources/db/migration")
                .resolve(module)
                .resolve(fileName);
        Files.createDirectories(path.getParent());
        Files.writeString(path, sql);
    }
}
