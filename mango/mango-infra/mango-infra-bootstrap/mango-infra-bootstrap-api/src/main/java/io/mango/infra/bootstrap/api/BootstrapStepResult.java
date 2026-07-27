package io.mango.infra.bootstrap.api;

import java.util.Map;

public record BootstrapStepResult(String summary, Map<String, Object> details) {

    public BootstrapStepResult {
        summary = summary == null ? "" : summary;
        details = details == null ? Map.of() : Map.copyOf(details);
    }

    public static BootstrapStepResult completed(String summary) {
        return new BootstrapStepResult(summary, Map.of());
    }
}
