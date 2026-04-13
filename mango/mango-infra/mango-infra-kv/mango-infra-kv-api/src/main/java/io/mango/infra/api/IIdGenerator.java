package io.mango.infra.kv.api;

/**
 * ID generator interface for distributed unique ID generation.
 */
public interface IIdGenerator {

    /**
     * Generate next unique ID.
     * @return next unique ID
     */
    long nextId();
}