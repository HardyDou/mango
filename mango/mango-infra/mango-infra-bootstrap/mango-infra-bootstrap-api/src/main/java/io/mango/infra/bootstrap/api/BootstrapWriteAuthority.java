package io.mango.infra.bootstrap.api;

public record BootstrapWriteAuthority(
        String environmentKey,
        long generation,
        String manifestFingerprint,
        long fencingToken) {
}
