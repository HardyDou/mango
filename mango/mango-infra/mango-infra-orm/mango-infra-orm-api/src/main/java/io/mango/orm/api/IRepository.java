package io.mango.orm.api;

/**
 * Base repository interface for DDD-style data access.
 * All domain repositories should extend this interface.
 *
 * @param <T>  Entity type
 * @param <ID> Primary key type
 */
public interface IRepository<T, ID> {

    /**
     * Find entity by ID
     */
    T findById(ID id);

    /**
     * Save or update entity
     */
    T save(T entity);

    /**
     * Delete entity by ID
     */
    void deleteById(ID id);
}
