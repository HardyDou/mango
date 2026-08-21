package io.mango.infra.docsign.core;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Validated signing key and certificate chain.
 */
record LoadedKeyMaterial(
        PrivateKey privateKey,
        X509Certificate certificate,
        List<X509Certificate> certificateChain) {

    LoadedKeyMaterial {
        certificateChain = List.copyOf(certificateChain);
    }
}
