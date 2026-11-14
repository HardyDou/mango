package io.mango.plugin.check;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Field;

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.junit.jupiter.api.Test;

/**
 * Regression for Issue #1270: Maven 3 has no Plexus converter for {@code java.nio.file.Path}, so a
 * POM path element bound to a {@code Path} parameter fails before the goal runs. The {@code check}
 * goal must accept the same plain path strings as the {@code architecture} goal.
 */
@MojoTest
class CheckMojoBindingTest {

    @Test
    void mavenConfiguratorBindsGlobalEntityManifestAsFile(
            @InjectMojo(goal = "check", pom = "src/test/resources/check-path-binding/pom.xml")
                    CheckMojo mojo)
            throws Exception {
        assertNotNull(mojo);
        Field field = CheckMojo.class.getDeclaredField("globalEntityManifest");
        field.setAccessible(true);
        File value = (File) field.get(mojo);
        assertNotNull(value, "globalEntityManifest");
        assertTrue(
                value.toPath().normalize().toString().replace('\\', '/')
                        .endsWith("global-entity-exceptions.json"),
                "globalEntityManifest=" + value);
    }
}
