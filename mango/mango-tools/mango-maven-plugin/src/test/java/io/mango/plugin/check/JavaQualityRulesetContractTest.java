package io.mango.plugin.check;

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
        assertTrue(!ruleset.contains("com.alibaba.p3c"));
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
