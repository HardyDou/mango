package io.mango.crypto.support;

/**
 * Digester interface for hash algorithms
 *
 * @author Mango
 */
public interface Digester {

    /**
     * Digest string data
     *
     * @param data data to digest
     * @return hex-encoded digest
     */
    String digest(String data);

    /**
     * Digest bytes
     *
     * @param data data to digest
     * @return digest bytes
     */
    byte[] digest(byte[] data);
}
