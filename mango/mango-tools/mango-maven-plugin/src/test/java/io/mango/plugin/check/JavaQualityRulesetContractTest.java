package io.mango.plugin.check;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class JavaQualityRulesetContractTest {

    @Test
    void pmdRulesetUsesPmd7BuiltInRulesWithoutLegacyP3cClasses() throws IOException {
        String ruleset = resource("/rulesets/java/pmd-mango.xml");

        assertTrue(ruleset.contains("category/java/codestyle.xml/ClassNamingConventions"));
        assertTrue(ruleset.contains("category/java/errorprone.xml/AvoidCatchingThrowable"));
        assertTrue(ruleset.contains("category/java/errorprone.xml/CloseResource"));
        assertTrue(!ruleset.contains("com.alibaba.p3c"));
    }

    @Test
    void pmdRulesetKeepsOnlyHighValueChecks() throws IOException {
        String ruleset = resource("/rulesets/java/pmd-mango.xml");

        assertFalse(ruleset.contains("category/java/multithreading.xml/UseConcurrentHashMap"));
        assertFalse(ruleset.contains("category/java/design.xml/AvoidCatchingGenericException"));
        assertFalse(ruleset.contains("category/java/design.xml/NPathComplexity"));
        assertFalse(ruleset.contains("category/java/design.xml/CyclomaticComplexity"));
        assertFalse(ruleset.contains("category/java/errorprone.xml/AvoidDuplicateLiterals"));
        assertFalse(ruleset.contains("category/java/errorprone.xml/AvoidLiteralsInIfCondition"));

        String checkstyle = resource("/rulesets/java/checkstyle.xml");
        assertTrue(checkstyle.contains("<module name=\"CyclomaticComplexity\">"));
        assertTrue(checkstyle.contains("<property name=\"max\" value=\"20\"/>"));
    }

    @Test
    void springCollaboratorExceptionsRemainCentralizedAndSpecific() throws IOException {
        String filter = resource("/rulesets/java/spotbugs-exclude.xml");

        assertTrue(filter.contains("<Bug pattern=\"EI_EXPOSE_REP2\"/>"));
        assertTrue(filter.contains("<Class name=\"io.mango.identity.core.service.impl.IdentityUserService\"/>"));
        assertTrue(filter.contains("<Class name=\"io.mango.link.core.integration.LinkConfigGateway\"/>"));
    }

    private String resource(String path) throws IOException {
        try (InputStream stream = getClass().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("Missing test resource: " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
