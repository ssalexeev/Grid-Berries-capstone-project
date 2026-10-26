package org.capstone.repository.jdbc;

import org.capstone.repository.CrudRepository;

import java.sql.ResultSet;
import java.util.function.Function;

public interface JdbcRepository<T,ID> extends CrudRepository<T,ID> {
    Function<ResultSet, T> mapper();
}
