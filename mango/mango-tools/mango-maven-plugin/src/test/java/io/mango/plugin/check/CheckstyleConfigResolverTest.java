package io.mango.plugin.check;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CheckstyleConfigResolverTest {

    @TempDir Path tempDir;

    private final CheckstyleConfigResolver resolver =
            new CheckstyleConfigResolver(CheckstyleConfigResolverTest.class.getClassLoader());

    @Test
    void resolve_withoutProjectConfig_extractsBundledMangoRules() throws Exception {
        CheckstyleConfigResolver.ResolvedConfig config = resolver.resolve(tempDir, null, null);

        Path configFile = Path.of(config.location());
        String content = Files.readString(configFile);
        assertEquals("default:mango-bundled", config.source());
        assertTrue(Files.isRegularFile(configFile));
        assertTrue(content.contains("CyclomaticComplexity"));
        assertFalse(content.contains("DesignForExtension"));
    }

    @Test
    void resolve_withProjectConfig_usesProjectRules() throws Exception {
        Path projectConfig = tempDir.resolve(CheckstyleConfigResolver.PROJECT_CONFIG_PATH);
        Files.createDirectories(projectConfig.getParent());
        Files.writeString(projectConfig, "<module name=\"Checker\"/>\n");

        CheckstyleConfigResolver.ResolvedConfig config = resolver.resolve(tempDir, null, null);

        assertEquals(projectConfig.toAbsolutePath().toString(), config.location());
        assertEquals("project:config/quality/checkstyle.xml", config.source());
    }

    @Test
    void resolve_withCustomConfig_prefersMangoPropertyOverStandardProperty() throws Exception {
        Path mangoConfig = tempDir.resolve("quality/mango-custom.xml");
        Path standardConfig = tempDir.resolve("quality/standard-custom.xml");
        Files.createDirectories(mangoConfig.getParent());
        Files.writeString(mangoConfig, "<module name=\"Checker\"/>\n");
        Files.writeString(standardConfig, "<module name=\"Checker\"/>\n");

        CheckstyleConfigResolver.ResolvedConfig config =
                resolver.resolve(
                        tempDir,
                        tempDir.relativize(mangoConfig).toString(),
                        tempDir.relativize(standardConfig).toString());

        assertEquals(mangoConfig.toAbsolutePath().toString(), config.location());
        assertEquals("custom:mango.check.checkstyleConfigLocation", config.source());
    }

    @Test
    void resolve_withStandardProperty_usesCustomRules() throws Exception {
        Path customConfig = tempDir.resolve("quality/company-checkstyle.xml");
        Files.createDirectories(customConfig.getParent());
        Files.writeString(customConfig, "<module name=\"Checker\"/>\n");

        CheckstyleConfigResolver.ResolvedConfig config =
                resolver.resolve(tempDir, null, customConfig.toString());

        assertEquals(customConfig.toAbsolutePath().toString(), config.location());
        assertEquals("custom:checkstyle.config.location", config.source());
    }

    @Test
    void resolve_withMissingCustomConfig_doesNotFallBackToBundledRules() throws Exception {
        String missingConfig = "quality/missing-checkstyle.xml";

        CheckstyleConfigResolver.ResolvedConfig config =
                resolver.resolve(tempDir, missingConfig, null);

        assertEquals(missingConfig, config.location());
        assertEquals("custom:mango.check.checkstyleConfigLocation", config.source());
        assertFalse(Files.exists(tempDir.resolve("target/mango-check/checkstyle.xml")));
    }
}
