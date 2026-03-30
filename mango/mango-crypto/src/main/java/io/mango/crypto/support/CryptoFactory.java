package io.mango.crypto.support;

import io.mango.crypto.symmetric.AesCipher;
import io.mango.crypto.symmetric.Sm4Cipher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Cryptographic factory for obtaining cipher instances
 *
 * @author Mango
 */
@Component
public class CryptoFactory {

    @Autowired
    private Sm4Cipher sm4Cipher;

    @Autowired
    private AesCipher aesCipher;

    /**
     * Get symmetric cipher by algorithm name
     *
     * @param algorithm SM4 or AES
     * @return SymmetricCipher instance
     */
    public SymmetricCipher getSymmetricCipher(String algorithm) {
        return switch (algorithm.toUpperCase()) {
            case "SM4" -> sm4Cipher;
            case "AES" -> aesCipher;
            default -> throw new IllegalArgumentException("Unsupported symmetric algorithm: " + algorithm);
        };
    }
}
