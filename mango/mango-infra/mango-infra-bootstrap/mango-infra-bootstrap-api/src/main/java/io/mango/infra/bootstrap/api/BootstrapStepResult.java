package io.mango.infra.bootstrap.api;

import io.mango.common.contract.LocalCapabilityContract;

import java.util.Map;

@LocalCapabilityContract
public record BootstrapStepResult(String summary, Map<String, Object> details) {

    public BootstrapStepResult {
        summary = summary == null ? "" : summary;
        details = details == null ? Map.of() : Map.copyOf(details);
    }

    public static BootstrapStepResult completed(String summary) {
        return new BootstrapStepResult(summary, Map.of());
    }
}
