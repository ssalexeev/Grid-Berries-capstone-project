package org.capstone.repository.jdbc.util;

import java.util.Optional;

public interface CrudRepository<T, ID> {
    Optional<T> findById(ID id);

    Iterable<T> findAll();

    T save(T entity);

    void delete(T entity);


}
