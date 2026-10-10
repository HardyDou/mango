package io.mango.infra.bootstrap.starter;

import java.util.Objects;

final class MangoLocalStartupState {

    private MangoLocalRuntimeIdentity identity;

    synchronized void accept(MangoLocalRuntimeIdentity value) {
        identity = Objects.requireNonNull(value, "value");
    }

    synchronized MangoLocalRuntimeIdentity requireIdentity() {
        if (identity == null) {
            throw new IllegalStateException("MANGO_LOCAL_STARTUP_IDENTITY_MISSING");
        }
        return identity;
    }
}

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
