package io.mango.infra.bootstrap.starter;

import java.util.Objects;

record MangoLocalRuntimeIdentity(
        String environmentKey,
        String releaseId,
        String revision,
        long generation,
        String fingerprint) {

    MangoLocalRuntimeIdentity {
        Objects.requireNonNull(environmentKey, "environmentKey");
        Objects.requireNonNull(releaseId, "releaseId");
        Objects.requireNonNull(revision, "revision");
        Objects.requireNonNull(fingerprint, "fingerprint");
        if (generation <= 0) {
            throw new IllegalArgumentException("generation must be positive");
        }
    }
}
