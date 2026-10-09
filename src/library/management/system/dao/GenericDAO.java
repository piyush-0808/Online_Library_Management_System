package library.management.system.dao;

import java.util.List;
import java.util.Optional;

import library.management.system.exception.DatabaseException;

/**
 * Generic DAO interface.
 *
 * T = entity type
 * K = primary-key type
 *
 * Every DAO uses this common structure for database operations.
 */
public interface GenericDAO<T, K> {

    /**
     * Finds one record by primary key.
     */
    Optional<T> findById(K id) throws DatabaseException;

    /**
     * Returns all records.
     */
    List<T> findAll() throws DatabaseException;

    /**
     * Saves a new record.
     */
    T save(T entity) throws DatabaseException;

    /**
     * Updates an existing record.
     */
    boolean update(T entity) throws DatabaseException;

    /**
     * Deletes a record by primary key.
     */
    boolean delete(K id) throws DatabaseException;

    /**
     * Default generic method.
     *
     * This also satisfies the GenericDAO default-method requirement.
     */
    default boolean existsById(K id) throws DatabaseException {
        return findById(id).isPresent();
    }
}