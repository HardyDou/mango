package io.mango.common.crypto.support;

/**
 * Symmetric cipher interface
 *
 * @author Mango
 */
public interface SymmetricCipher {

    /**
     * Encrypt data
     *
     * @param data plaintext data
     * @return ciphertext
     */
    String encrypt(String data);

    /**
     * Decrypt data
     *
     * @param encrypted ciphertext
     * @return plaintext
     */
    String decrypt(String encrypted);

    /**
     * Encrypt bytes
     *
     * @param data plaintext bytes
     * @return ciphertext bytes
     */
    byte[] encrypt(byte[] data);

    /**
     * Decrypt bytes
     *
     * @param encrypted ciphertext bytes
     * @return plaintext bytes
     */
    byte[] decrypt(byte[] encrypted);
}
