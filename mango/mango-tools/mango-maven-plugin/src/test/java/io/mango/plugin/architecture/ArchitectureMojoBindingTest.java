package io.mango.plugin.architecture;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Field;

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.junit.jupiter.api.Test;

@MojoTest
class ArchitectureMojoBindingTest {

    @Test
    void mavenConfiguratorBindsAllPathParametersAsFiles(
            @InjectMojo(
                            goal = "architecture",
                            pom = "src/test/resources/architecture-path-binding/pom.xml")
                    ArchitectureMojo mojo)
            throws Exception {
        assertNotNull(mojo);
        assertBoundFile(mojo, "reportFile", "target/architecture-report.json");
        assertBoundFile(mojo, "rootDirectory", "mango-maven-plugin");
        assertBoundFile(mojo, "debtBaselineFile", "debt-budget.json");
        assertBoundFile(mojo, "globalEntityManifest", "global-entity-exceptions.json");
    }

    private void assertBoundFile(ArchitectureMojo mojo, String fieldName, String pathSuffix)
            throws Exception {
        Field field = ArchitectureMojo.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        File value = (File) field.get(mojo);
        assertNotNull(value, fieldName);
        assertTrue(
                value.toPath().normalize().toString().replace('\\', '/').endsWith(pathSuffix),
                fieldName + "=" + value);
    }
}
