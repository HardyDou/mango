package io.mango.infra.log.starter;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("flow")
@Tag("infra-log")
class LogbackRuntimeFlowTest {

    private static final String APP_NAME = "mango-log-flow";

    @TempDir
    Path logDirectory;

    @Test
    void shouldApplyDocumentedPropertiesInRealSpringBootProcess() throws Exception {
        String output = runProbe(
                "--spring.profiles.active=dev",
                "--mango.log.level.root=ERROR",
                "--mango.log.level.mango=TRACE",
                "--mango.log.file.max-history=7",
                "--mango.log.operation.enabled=false",
                "--mango.log.json.enabled=true");

        assertThat(output)
                .contains("PROBE_ROOT_LEVEL=ERROR", "PROBE_MANGO_LEVEL=TRACE", "PROBE_FILE_MAX_HISTORY=7")
                .doesNotContain("condition' attribute in <if> element is deprecated");
        assertThat(logDirectory.resolve(APP_NAME + "-operation-plain.log")).doesNotExist();
        Path jsonLog = logDirectory.resolve(APP_NAME + ".json.log");
        assertThat(jsonLog).exists();
        assertThat(Files.readString(jsonLog))
                .contains("\"message\":\"regular-flow\"")
                .contains("\"requestId\":\"request-flow\"")
                .contains("\"traceId\":\"trace-flow\"")
                .contains("\"clientIp\":\"127.0.0.1\"")
                .contains("\"env\":\"dev\"");
    }

    @Test
    void shouldWriteSeparatedProdAndOperationLogs() throws Exception {
        String output = runProbe(
                "--spring.profiles.active=prod",
                "--mango.log.level.root=ERROR",
                "--mango.log.file.max-history=7",
                "--mango.log.operation.enabled=true",
                "--mango.log.operation.max-history=5");

        assertThat(output).contains(
                "PROBE_ROOT_LEVEL=ERROR",
                "PROBE_FILE_MAX_HISTORY=7",
                "PROBE_OPERATION_MAX_HISTORY=5");
        assertThat(Files.readString(logDirectory.resolve(APP_NAME + ".json.log")))
                .contains("\"message\":\"regular-flow\"");
        assertThat(Files.readString(logDirectory.resolve(APP_NAME + "-error.log")))
                .contains("\"message\":\"regular-flow\"");
        assertThat(Files.readString(logDirectory.resolve(APP_NAME + "-operation.log")))
                .contains("\"message\":\"operation-flow\"")
                .contains("\"type\":\"operation\"");
    }

    private String runProbe(String... arguments) throws IOException, InterruptedException {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        String[] command = new String[arguments.length + 4];
        command[0] = java;
        command[1] = "-cp";
        command[2] = classpath;
        command[3] = LogProbeApplication.class.getName();
        System.arraycopy(arguments, 0, command, 4, arguments.length);

        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
        builder.environment().put("LOG_PATH", logDirectory.toString());
        builder.environment().put("APP_NAME", APP_NAME);
        Process process = builder.start();
        boolean completed = process.waitFor(30, TimeUnit.SECONDS);
        if (!completed) {
            process.destroyForcibly();
        }
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        assertThat(completed).as(output).isTrue();
        assertThat(process.exitValue()).as(output).isZero();
        return output;
    }
}
