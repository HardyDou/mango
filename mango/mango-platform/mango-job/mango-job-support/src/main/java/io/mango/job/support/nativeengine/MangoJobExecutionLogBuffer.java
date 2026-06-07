package io.mango.job.support.nativeengine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Job 执行期间标准输出捕获结果。
 */
final class MangoJobExecutionLogBuffer {

    private final ByteArrayOutputStream stdout = new ByteArrayOutputStream();

    private final ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    private final PrintStream stdoutStream = new PrintStream(stdout, true, StandardCharsets.UTF_8);

    private final PrintStream stderrStream = new PrintStream(stderr, true, StandardCharsets.UTF_8);

    PrintStream stdoutStream() {
        return stdoutStream;
    }

    PrintStream stderrStream() {
        return stderrStream;
    }

    String stdout() {
        stdoutStream.flush();
        return stdout.toString(StandardCharsets.UTF_8);
    }

    String stderr() {
        stderrStream.flush();
        return stderr.toString(StandardCharsets.UTF_8);
    }
}
