package io.mango.infra.crypto.impl;

/**
 * Digest service interface for hash operations.
 */
public interface IDigester {

    /**
     * Digest string data and return hex-encoded result.
     *
     * @param data data to digest
     * @return hex-encoded digest
     */
    String digest(String data);

    /**
     * Digest byte array data.
     *
     * @param data data to digest
     * @return raw digest bytes
     */
    byte[] digest(byte[] data);
}
