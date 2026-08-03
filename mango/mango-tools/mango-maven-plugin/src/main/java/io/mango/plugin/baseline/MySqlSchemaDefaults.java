package io.mango.plugin.baseline;

import org.apache.maven.plugin.MojoExecutionException;

import java.util.Locale;
import java.util.regex.Pattern;

record MySqlSchemaDefaults(String characterSet, String collation) {

    static final String DEFAULT_CHARACTER_SET = "utf8mb4";
    static final String DEFAULT_COLLATION = "utf8mb4_unicode_ci";

    private static final Pattern IDENTIFIER = Pattern.compile("[a-zA-Z0-9_]+");

    static MySqlSchemaDefaults from(String characterSet, String collation)
            throws MojoExecutionException {
        String normalizedCharacterSet = normalize(
                characterSet,
                "mango.baseline.characterSet",
                "MANGO-BASELINE-041");
        String normalizedCollation = normalize(
                collation,
                "mango.baseline.collation",
                "MANGO-BASELINE-042");
        return new MySqlSchemaDefaults(normalizedCharacterSet, normalizedCollation);
    }

    static MySqlSchemaDefaults cliStandard() {
        return new MySqlSchemaDefaults(DEFAULT_CHARACTER_SET, DEFAULT_COLLATION);
    }

    private static String normalize(String value, String property, String errorCode)
            throws MojoExecutionException {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (!IDENTIFIER.matcher(normalized).matches()) {
            throw new MojoExecutionException(
                    errorCode + " " + property + " must be a non-blank MySQL identifier");
        }
        return normalized;
    }
}
