package io.mango.plugin.check;

import java.nio.file.Path;

class CheckGateOptions {
    private final Path basePath;
    private final String changedFiles;
    private final String baseRef;
    private final String baselineFile;
    private final String gate;
    private final String staticFailurePolicy;
    private final boolean changedOnly;
    private final String reportOnlyRules;

    CheckGateOptions(
            Path basePath,
            String changedFiles,
            String baseRef,
            String baselineFile,
            String gate,
            String staticFailurePolicy) {
        this(basePath, changedFiles, baseRef, baselineFile, gate, staticFailurePolicy, false, null);
    }

    CheckGateOptions(
            Path basePath,
            String changedFiles,
            String baseRef,
            String baselineFile,
            String gate,
            String staticFailurePolicy,
            boolean changedOnly) {
        this(basePath, changedFiles, baseRef, baselineFile, gate, staticFailurePolicy, changedOnly, null);
    }

    CheckGateOptions(
            Path basePath,
            String changedFiles,
            String baseRef,
            String baselineFile,
            String gate,
            String staticFailurePolicy,
            boolean changedOnly,
            String reportOnlyRules) {
        this.basePath = basePath;
        this.changedFiles = changedFiles;
        this.baseRef = baseRef;
        this.baselineFile = baselineFile;
        this.gate = gate;
        this.staticFailurePolicy = staticFailurePolicy;
        this.changedOnly = changedOnly;
        this.reportOnlyRules = reportOnlyRules;
    }

    Path basePath() {
        return basePath;
    }

    String changedFiles() {
        return changedFiles;
    }

    String baseRef() {
        return baseRef;
    }

    String baselineFile() {
        return baselineFile;
    }

    String gate() {
        return gate;
    }

    String staticFailurePolicy() {
        return staticFailurePolicy;
    }

    boolean changedOnly() {
        return changedOnly;
    }

    String reportOnlyRules() {
        return reportOnlyRules;
    }
}
