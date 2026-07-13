package io.mango.plugin.gen;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;

class GenModuleMojoTest {

    @Test
    void legacyGeneratorFailsClosedAndPointsToCanonicalCli() {
        MojoExecutionException exception = assertThrows(
                MojoExecutionException.class, () -> new GenModuleMojo().execute());

        assertTrue(exception.getMessage().contains("mango:gen-module is retired"));
        assertTrue(exception.getMessage().contains("mango module add"));
        assertTrue(exception.getMessage().contains("--aggregate-name"));
        assertTrue(exception.getMessage().contains("--module-name"));
    }
}
