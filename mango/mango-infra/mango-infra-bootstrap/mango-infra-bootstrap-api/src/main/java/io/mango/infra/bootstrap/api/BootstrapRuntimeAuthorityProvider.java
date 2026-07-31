package io.mango.infra.bootstrap.api;

import java.util.Optional;

/**
 * Supplies the short-lived write authority of the current Runtime generation.
 * An empty result means this instance may serve compatible reads but must not
 * perform generation-owned framework writes.
 */
public interface BootstrapRuntimeAuthorityProvider {

    Optional<BootstrapWriteAuthority> currentWriteAuthority();
}
