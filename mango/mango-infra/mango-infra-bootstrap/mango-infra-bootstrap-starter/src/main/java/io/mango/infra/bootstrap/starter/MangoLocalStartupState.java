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
