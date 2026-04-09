package io.mango.infra.crypto.impl;

/**
 * Asymmetric encryption/decryption service interface.
 */
public interface IAsymmetricCryptoService {

    /**
     * Encrypt plaintext.
     *
     * @param plaintext plaintext data
     * @return ciphertext (Base64 encoded)
     */
    String encrypt(String plaintext);

    /**
     * Decrypt ciphertext.
     *
     * @param encrypted ciphertext (Base64 encoded)
     * @return plaintext
     */
    String decrypt(String encrypted);
}
