package org.capstone.repository.jdbc.util;

import java.sql.ResultSet;
import java.util.function.Function;

public interface JdbcRepository<T,ID> extends CrudRepository<T,ID> {
    Function<ResultSet, T> mapper();
}
