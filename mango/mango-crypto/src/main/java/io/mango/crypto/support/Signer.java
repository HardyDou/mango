package io.mango.crypto.support;

/**
 * Signer interface for digital signatures
 *
 * @author Mango
 */
public interface Signer {

    /**
     * Sign data
     *
     * @param data data to sign
     * @return signature
     */
    String sign(String data);

    /**
     * Verify signature
     *
     * @param data original data
     * @param signature signature to verify
     * @return true if valid
     */
    boolean verify(String data, String signature);
}
