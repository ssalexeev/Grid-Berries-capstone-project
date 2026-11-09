package org.capstone.repository.jdbc.util;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

@Repository
@RequiredArgsConstructor
public class JdbcUtil {
    private final DataSource dataSource;

    public Connection getConnection() {
        return DataSourceUtils.getConnection(dataSource);
    }

    public void execute(String query, Object... args) {
        Connection connection = getConnection();
        try (PreparedStatement statement = connection.prepareStatement(query)) {

            bindParameters(statement, args);

            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error executing database update: " + query, e);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public int executeAndReturnUpdateCount(String query, Object... args) {
        Connection connection = getConnection();
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            bindParameters(statement, args);
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error executing database update: " + query, e);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public long insertAndReturnId(String query, Object... args) {
        Connection connection = getConnection();
        try (PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            bindParameters(statement, args);
            int updatedRows = statement.executeUpdate();
            if (updatedRows != 1) {
                throw new IllegalStateException("Insert affected " + updatedRows + " rows: " + query);
            }
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new IllegalStateException("Insert did not return a generated ID: " + query);
                }
                long id = generatedKeys.getLong(1);
                if (generatedKeys.wasNull()) {
                    throw new IllegalStateException("Insert returned a null ID: " + query);
                }
                return id;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error executing database insert: " + query, e);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public void execute(String query, Consumer<PreparedStatement> consumer) {
        Connection connection = getConnection();
        try (PreparedStatement statement = connection.prepareStatement(query)) {

            consumer.accept(statement);

            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error executing database update: " + query, e);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public <T> Optional<T> findOne(String query, Function<ResultSet, T> mapper, Object... args) {
        Connection connection = getConnection();
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            bindParameters(statement, args);

            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }

                T result = mapper.apply(rs);

                if (rs.next()) {
                    throw new IllegalStateException("Query returned more than 1 result: " + query);
                }

                return Optional.ofNullable(result);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error executing findOne query: " + query, e);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public <T> List<T> findMany(String query, Function<ResultSet, T> mapper, Object... args) {
        List<T> results = new ArrayList<>();

        Connection connection = getConnection();
        try (PreparedStatement statement = connection.prepareStatement(query)) {

            bindParameters(statement, args);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    results.add(mapper.apply(rs));
                }
            }

            return results;
        } catch (SQLException e) {
            throw new RuntimeException("Error executing findMany query: " + query, e);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    private void bindParameters(PreparedStatement statement, Object... args) throws SQLException {
        for (int i = 0; i < args.length; i++) {
            statement.setObject(i + 1, args[i]);
        }
    }
}
