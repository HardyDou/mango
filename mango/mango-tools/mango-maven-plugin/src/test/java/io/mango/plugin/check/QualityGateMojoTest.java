package io.mango.plugin.check;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QualityGateMojoTest {

    @TempDir
    Path tempDir;

    @Test
    void execute_gatePasses_writesReport() throws Exception {
        Path tool = writeTool("""
                import fs from 'node:fs';
                const report = process.argv[process.argv.indexOf('--report') + 1];
                fs.mkdirSync(new URL('.', `file://${report}`).pathname, { recursive: true });
                fs.writeFileSync(report, '{"status":"PASS"}\\n');
                """);
        QualityGateMojo mojo = configuredMojo(tool);

        assertDoesNotThrow(mojo::execute);
        assertTrue(Files.exists(tempDir.resolve(".runtime/report.json")));
    }

    @Test
    void execute_gateBlocks_failsBuild() throws Exception {
        Path tool = writeTool("process.exit(1);\n");
        QualityGateMojo mojo = configuredMojo(tool);

        assertThrows(MojoExecutionException.class, mojo::execute);
    }

    @Test
    void execute_toolMissing_failsClosed() throws Exception {
        QualityGateMojo mojo = new QualityGateMojo();
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "toolFile", tempDir.resolve("missing.mjs").toString());

        MojoExecutionException failure = assertThrows(MojoExecutionException.class, mojo::execute);
        assertTrue(failure.getMessage().contains("does not exist"));
    }

    private QualityGateMojo configuredMojo(Path tool) throws Exception {
        QualityGateMojo mojo = new QualityGateMojo();
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "repositoryRoot", tempDir.toString());
        setField(mojo, "toolFile", tool.toString());
        setField(mojo, "nodeExecutable", "node");
        setField(mojo, "baseRef", "origin/main");
        setField(mojo, "headRef", "HEAD");
        setField(mojo, "reportFile", ".runtime/report.json");
        setField(mojo, "timeoutSeconds", 30L);
        return mojo;
    }

    private Path writeTool(String source) throws Exception {
        Path tool = tempDir.resolve("quality-gate.mjs");
        Files.writeString(tool, source);
        return tool;
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
