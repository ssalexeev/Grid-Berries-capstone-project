package org.capstone.repository.jdbc.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.capstone.config.database.DBProperties;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class JdbcUtil {

    private static  String url = DBProperties.URL;
    private static  String username = DBProperties.USERNAME;
    private static  String password = DBProperties.PASSWORD;

    private final DataSource dataSource;

    public JdbcUtil(int maxPoolSize) {
        this.dataSource = createHikariPool(maxPoolSize);
    }
    public JdbcUtil() {
        this.dataSource = createHikariPool(5);
    }

    public static void setCredentials(String newUrl, String newUsername, String newPassword) {
        url = newUrl;
        username = newUsername;
        password = newPassword;
    }

    private HikariDataSource createHikariPool(int maxPoolSize) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(maxPoolSize);
        config.setConnectionTimeout(5000);
        return new HikariDataSource(config);
    }

    public  Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void execute(String query, Object... args) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            bindParameters(statement, args);

            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error executing database update: " + query, e);
        }
    }

    public long insertAndReturnId(String query, Object... args) {
        return findOne(query, resultSet -> {
            try {
                long id = resultSet.getLong(1);
                if (resultSet.wasNull()) {
                    throw new IllegalStateException("Insert returned a null ID");
                }
                return id;
            } catch (SQLException e) {
                throw new RuntimeException("Failed to read generated ID", e);
            }
        }, args).orElseThrow(() -> new IllegalStateException("Insert did not return an ID"));
    }

    public void execute(String query, Consumer<PreparedStatement> consumer) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            consumer.accept(statement);

            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error executing database update: " + query, e);
        }
    }

    public <T> Optional<T> findOne(String query, Function<ResultSet, T> mapper, Object... args) {

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
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
        }
    }

    public <T> List<T> findMany(String query, Function<ResultSet, T> mapper, Object... args) {
        List<T> results = new ArrayList<>();

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            bindParameters(statement, args);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    results.add(mapper.apply(rs));
                }
            }

            return results;
        } catch (SQLException e) {
            throw new RuntimeException("Error executing findMany query: " + query, e);
        }
    }

    private void bindParameters(PreparedStatement statement, Object... args) throws SQLException {
        for (int i = 0; i < args.length; i++) {
            statement.setObject(i + 1, args[i]);
        }
    }
}
