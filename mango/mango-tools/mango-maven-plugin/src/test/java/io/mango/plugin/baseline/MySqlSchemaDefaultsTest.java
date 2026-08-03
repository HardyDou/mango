package io.mango.plugin.baseline;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MySqlSchemaDefaultsTest {

    @Test
    void normalizesSafeMysqlIdentifiers() throws Exception {
        MySqlSchemaDefaults defaults = MySqlSchemaDefaults.from(
                " UTF8MB4 ", " UTF8MB4_UNICODE_CI ");

        assertEquals("utf8mb4", defaults.characterSet());
        assertEquals("utf8mb4_unicode_ci", defaults.collation());
    }

    @Test
    void rejectsUnsafeCharacterSetAndCollationValues() {
        MojoExecutionException characterSet = assertThrows(
                MojoExecutionException.class,
                () -> MySqlSchemaDefaults.from("utf8mb4; DROP DATABASE mysql", "utf8mb4_unicode_ci"));
        MojoExecutionException collation = assertThrows(
                MojoExecutionException.class,
                () -> MySqlSchemaDefaults.from("utf8mb4", "utf8mb4_unicode_ci --"));

        assertTrue(characterSet.getMessage().contains("MANGO-BASELINE-041"));
        assertTrue(collation.getMessage().contains("MANGO-BASELINE-042"));
    }
}
