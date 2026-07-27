package io.mango.infra.bootstrap.core;

public record BootstrapControl(
        String environmentKey,
        long stableGeneration,
        String stableFingerprint,
        Long candidateGeneration,
        String candidateFingerprint,
        long authoritativeGeneration,
        String state,
        long fencingToken) {
}
