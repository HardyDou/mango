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
     * Decrypt ciphertext.
     *
     * <p>IV handling: this method always extracts the IV from the ciphertext prefix
     * (first 16 bytes in CBC mode). The {@code iv} parameter is accepted for
     * API compatibility but is ignored — ciphertext must have IV prepended during
     * encryption ({@link #encrypt(String)} or {@link #encrypt(String, String)}).
     *
     * <p>If you need to decrypt ciphertext without an embedded IV, use a separate
     * service configuration that stores the IV externally, or provide the raw
     * ciphertext bytes directly to the underlying cipher.
     *
     * @param ciphertext ciphertext (Base64 encoded); in CBC mode must be
     *                  Base64(IV(16 bytes) || encryptedPayload)
     * @param iv        ignored — kept for API compatibility only
     * @return plaintext
     */
    String decrypt(String ciphertext, String iv);
}
