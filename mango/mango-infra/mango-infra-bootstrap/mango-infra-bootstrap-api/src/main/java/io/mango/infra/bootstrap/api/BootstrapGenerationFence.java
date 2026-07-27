package io.mango.infra.bootstrap.api;

public interface BootstrapGenerationFence {

    void assertAuthoritative(BootstrapWriteAuthority authority);
}
