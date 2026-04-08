package io.mango.infra.crypto.impl;

/**
 * Symmetric encryption/decryption service interface.
 */
public interface ICryptoService {

    /**
     * Encrypt plaintext.
     *
     * @param plaintext plaintext data
     * @return ciphertext (Base64 encoded)
     */
    String encrypt(String plaintext);

    /**
     * Encrypt plaintext with custom IV.
     *
     * @param plaintext plaintext data
     * @param iv        IV (Base64 encoded)
     * @return ciphertext (Base64 encoded)
     */
    String encrypt(String plaintext, String iv);

    /**
     * Decrypt ciphertext.
     *
     * @param ciphertext ciphertext (Base64 encoded)
     * @return plaintext
     */
    String decrypt(String ciphertext);

    /**
     * Decrypt ciphertext with custom IV.
     *
     * @param ciphertext ciphertext (Base64 encoded)
     * @param iv         IV (Base64 encoded)
     * @return plaintext
     */
    String decrypt(String ciphertext, String iv);
}
