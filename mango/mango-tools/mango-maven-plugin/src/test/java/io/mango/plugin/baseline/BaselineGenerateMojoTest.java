package io.mango.plugin.baseline;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaselineGenerateMojoTest {

    @Test
    void generatedBaselineCopyRemovesStaleModuleFiles(@TempDir Path directory) throws Exception {
        Path generated = directory.resolve("generated");
        Path classes = directory.resolve("classes");
        Files.createDirectories(generated.resolve("db/baseline/current"));
        Files.createDirectories(generated.resolve("META-INF/mango"));
        Files.writeString(generated.resolve("db/baseline/current/B2__baseline.sql"), "current");
        Files.writeString(generated.resolve("META-INF/mango/baseline-manifest.json"), "manifest");
        Files.createDirectories(classes.resolve("db/baseline/removed"));
        Files.writeString(classes.resolve("db/baseline/removed/B1__baseline.sql"), "stale");

        BaselineGenerateMojo.copyGeneratedResourcesToClasses(generated, classes);

        assertFalse(Files.exists(classes.resolve("db/baseline/removed/B1__baseline.sql")));
        assertEquals("current", Files.readString(classes.resolve("db/baseline/current/B2__baseline.sql")));
        assertEquals("manifest", Files.readString(classes.resolve("META-INF/mango/baseline-manifest.json")));
    }

    @Test
    void resourceApplicationMustBeCompiledBeforeBaselineGeneration(@TempDir Path directory) throws Exception {
        BaselineGenerateMojo mojo = new BaselineGenerateMojo();
        setField(mojo, "jdbcUrl", "jdbc:mysql://127.0.0.1:3306/mysql");
        setField(mojo, "username", "root");
        setField(mojo, "schemaPrefix", "mango_baseline_test");
        setField(mojo, "searchDirectory", directory.toFile());
        setField(mojo, "outputDirectory", directory.resolve("target/generated-resources").toFile());
        setField(mojo, "projectDirectory", directory.toFile());
        setField(mojo, "classesDirectory", directory.resolve("target/classes").toFile());
        setField(mojo, "project", new MavenProject());
        setField(mojo, "resourceApplicationClass", "example.MissingApplication");
        setField(mojo, "resourceTimeoutSeconds", 30L);

        MojoExecutionException exception = assertThrows(MojoExecutionException.class, mojo::execute);

        assertTrue(exception.getMessage().contains("MANGO-BASELINE-050"));
        assertTrue(exception.getMessage().contains("bind this execution to prepare-package"));
    }

    @Test
    void resourceRuntimeClasspathIncludesReadableAdditionalElements(@TempDir Path directory) throws Exception {
        Path externalAssets = Files.createDirectories(directory.resolve("external-assets"));

        List<String> classpath = BaselineGenerateMojo.resourceRuntimeClasspath(
                List.of("target/classes", externalAssets.toString()),
                List.of(new File("external-assets"), externalAssets.toFile()),
                directory);

        assertEquals(List.of("target/classes", externalAssets.toString()), classpath);
    }

    @Test
    void resourceRuntimeClasspathRejectsUnreadableAdditionalElement(@TempDir Path directory) {
        Path missing = directory.resolve("missing-assets");

        MojoExecutionException exception = assertThrows(
                MojoExecutionException.class,
                () -> BaselineGenerateMojo.resourceRuntimeClasspath(
                        List.of("target/classes"), List.of(missing.toFile()), directory));

        assertTrue(exception.getMessage().contains("MANGO-BASELINE-053"));
        assertTrue(exception.getMessage().contains(missing.toString()));
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
