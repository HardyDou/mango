package io.mango.plugin.check;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BundledRulesetResolverTest {

    @TempDir Path tempDir;

    private final BundledRulesetResolver resolver =
            new BundledRulesetResolver(BundledRulesetResolverTest.class.getClassLoader());

    @Test
    void extract_bundledSpotbugsAndPmdRulesetsExistOutsideMangoRepository() throws Exception {
        String spotbugs = resolver.extract(tempDir,
                BundledRulesetResolver.SPOTBUGS_RESOURCE, "spotbugs-security.xml");
        String pmd = resolver.extract(tempDir, BundledRulesetResolver.PMD_RESOURCE, "pmd-p3c.xml");

        assertTrue(Files.isRegularFile(Path.of(spotbugs)));
        assertTrue(Files.isRegularFile(Path.of(pmd)));
        assertTrue(Files.readString(Path.of(spotbugs)).contains("Bug"));
        assertTrue(Files.readString(Path.of(pmd)).contains("ali-naming.xml"));
        assertEquals(tempDir.resolve("target/mango-check").toAbsolutePath().toString(),
                Path.of(pmd).getParent().toString());
    }

    @Test
    void resolve_withExplicitRelativeFile_usesProjectFile() throws Exception {
        Path custom = tempDir.resolve("quality/spotbugs-custom.xml");
        Files.createDirectories(custom.getParent());
        Files.writeString(custom, "<FindBugsFilter/>\n");

        String location = resolver.resolve(tempDir, "quality/spotbugs-custom.xml", null,
                BundledRulesetResolver.SPOTBUGS_RESOURCE, "spotbugs-security.xml");

        assertEquals(custom.toAbsolutePath().toString(), location);
    }

    @Test
    void resolve_rulesetDirectoryPrecedesProjectAndBundledRules() throws Exception {
        Path directory = tempDir.resolve("quality");
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("pmd-p3c.xml"), "<ruleset name=\"dir\"/>\n");
        Path projectConfig = tempDir.resolve("config/quality/pmd-p3c.xml");
        Files.createDirectories(projectConfig.getParent());
        Files.writeString(projectConfig, "<ruleset name=\"project\"/>\n");

        String location = resolver.resolve(tempDir, null, "quality",
                BundledRulesetResolver.PMD_RESOURCE, "pmd-p3c.xml",
                "config/quality/pmd-p3c.xml", "config/quality/pmd.xml");

        assertEquals(directory.resolve("pmd-p3c.xml").toAbsolutePath().toString(), location);
    }

    @Test
    void resolve_projectConfigUsedWhenRulesetDirectoryIsMissing() throws Exception {
        Path projectConfig = tempDir.resolve("config/quality/spotbugs.xml");
        Files.createDirectories(projectConfig.getParent());
        Files.writeString(projectConfig, "<FindBugsFilter/>\n");

        String location = resolver.resolve(tempDir, null, "missing",
                BundledRulesetResolver.SPOTBUGS_RESOURCE, "spotbugs-security.xml",
                "config/quality/spotbugs-security.xml", "config/quality/spotbugs.xml");

        assertEquals(projectConfig.toAbsolutePath().toString(), location);
    }

    @Test
    void resolve_withoutExplicitFile_extractsBundledRuleset() throws Exception {
        String location = resolver.resolve(tempDir, null, null,
                BundledRulesetResolver.PMD_RESOURCE, "pmd-p3c.xml");

        assertTrue(Files.isRegularFile(Path.of(location)));
        assertTrue(location.endsWith("target/mango-check/pmd-p3c.xml"));
    }
}
