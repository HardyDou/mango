package io.mango.plugin.check;

import java.nio.file.Path;

class CheckGateOptions {
    private final Path basePath;
    private final String changedFiles;
    private final String baseRef;
    private final String baselineFile;
    private final String gate;
    private final String staticFailurePolicy;

    CheckGateOptions(Path basePath, String changedFiles, String baseRef, String baselineFile,
                     String gate, String staticFailurePolicy) {
        this.basePath = basePath;
        this.changedFiles = changedFiles;
        this.baseRef = baseRef;
        this.baselineFile = baselineFile;
        this.gate = gate;
        this.staticFailurePolicy = staticFailurePolicy;
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
}
