package io.mango.infra.docsign.core;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Provider;
import java.security.Security;

/**
 * Idempotent Bouncy Castle provider registration for CMS and SM2 operations.
 */
final class CryptoProviderSupport {

    private CryptoProviderSupport() {
    }

    static Provider provider() {
        Provider current = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME);
        if (current != null) {
            return current;
        }
        synchronized (CryptoProviderSupport.class) {
            Provider registered = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME);
            if (registered != null) {
                return registered;
            }
            Provider provider = new BouncyCastleProvider();
            Security.addProvider(provider);
            return provider;
        }
    }
}
