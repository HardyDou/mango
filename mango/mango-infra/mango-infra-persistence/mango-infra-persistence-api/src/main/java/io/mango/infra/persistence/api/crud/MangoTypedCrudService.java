package io.mango.infra.persistence.api.crud;

import io.mango.infra.persistence.api.query.PersistencePageResult;

/**
 * Compile-time typed CRUD contract for business services.
 *
 * @param <E> persistence entity type
 * @param <C> create command type
 * @param <U> update command type
 * @param <Q> page query type
 * @param <V> view object type
 * @param <ID> business identifier type
 */
public interface MangoTypedCrudService<E, C, U, Q, V, ID> extends MangoCrudService<E> {

    ID create(C command);

    boolean update(U command);

    boolean delete(DeleteCommand command);

    PersistencePageResult<V> page(Q query);

    V detail(ID id);
}
