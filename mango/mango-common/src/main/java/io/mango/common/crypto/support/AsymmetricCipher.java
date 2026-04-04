package io.mango.common.crypto.support;

/**
 * Asymmetric cipher interface for encryption/decryption
 *
 * @author Mango
 */
public interface AsymmetricCipher {

    /**
     * Encrypt data with public key
     *
     * @param data plaintext data
     * @return ciphertext
     */
    String encrypt(String data);

    /**
     * Decrypt data with private key
     *
     * @param encrypted ciphertext
     * @return plaintext
     */
    String decrypt(String encrypted);
}
