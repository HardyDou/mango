package io.mango.infra.bootstrap.api;

import io.mango.common.contract.LocalCapabilityContract;

@LocalCapabilityContract
public record BootstrapWriteAuthority(
        String environmentKey,
        long generation,
        String manifestFingerprint,
        long fencingToken) {
}
