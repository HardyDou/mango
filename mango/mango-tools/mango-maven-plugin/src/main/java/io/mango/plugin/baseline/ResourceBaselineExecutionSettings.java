package io.mango.plugin.baseline;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

record ResourceBaselineExecutionSettings(
        String applicationClass,
        List<String> runtimeClasspath,
        Path workingDirectory,
        Duration timeout) {

    ResourceBaselineExecutionSettings {
        runtimeClasspath = List.copyOf(runtimeClasspath);
    }
}
