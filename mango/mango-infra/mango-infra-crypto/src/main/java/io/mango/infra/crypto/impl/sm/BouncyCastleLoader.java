package io.mango.infra.crypto.impl.sm;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

/**
 * Initializes BouncyCastle provider once globally.
 */
public final class BouncyCastleLoader {

    /** BouncyCastle provider name used in KeyFactory etc. */
    static final String PROVIDER_NAME = "BC";

    static {
        if (Security.getProvider(PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private BouncyCastleLoader() {
    }

    /** Ensures the provider is registered. Call from each service static block. */
    public static void ensure() {
        // Already registered in static initializer above
    }
}
