package io.mango.infra.crypto.impl;

/**
 * Signature service interface for signing and verification.
 */
public interface ISignService {

    /**
     * Sign data.
     *
     * @param data data to sign
     * @return signature (Base64 encoded)
     */
    String sign(String data);

    /**
     * Verify signature.
     *
     * @param data      original data
     * @param signature signature to verify (Base64 encoded)
     * @return true if signature is valid
     */
    boolean verify(String data, String signature);
}
