package org.capstone.transactions;

import org.capstone.repository.jdbc.util.JdbcUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class TransactionsWorkTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withInitScript("db/changelog/changes/v1__init_schema.sql");

    private static JdbcUtil jdbcUtil;
    private long userId;
    private long productId;

    @BeforeAll
    static void setupDatabase() {
        JdbcUtil.setCredentials(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        jdbcUtil = new JdbcUtil();
    }

    @AfterAll
    static void resetCredentials() {
        JdbcUtil.setCredentials(
                "jdbc:postgresql://localhost:5432/mydatabase",
                "myuser",
                "mypassword"
        );
    }

    @BeforeEach
    void setup() {
        jdbcUtil.execute("TRUNCATE TABLE \"user\" RESTART IDENTITY CASCADE");
        jdbcUtil.execute("ALTER TABLE product DROP CONSTRAINT IF EXISTS chk_product_stock_non_negative");
        jdbcUtil.execute("""
                ALTER TABLE product
                ADD CONSTRAINT chk_product_stock_non_negative CHECK (stock_quantity >= 0)
                """);

        userId = jdbcUtil.insertAndReturnId("""
                INSERT INTO "user" (email, password_hash, role)
                VALUES (?, ?, ?)
                RETURNING id
                """, "buyer@example.com", "password-hash", "BUYER");
        productId = jdbcUtil.insertAndReturnId("""
                INSERT INTO product (title, price, stock_quantity, type)
                VALUES (?, ?, ?, ?)
                RETURNING id
                """, "Wireless Headphones", 150.00, 2, "PHYSICAL_GOOD");
    }

    @Nested
    @DisplayName("Consistency cases in transactions")
    class ConsistencyTests {

        @Test
        @DisplayName("Inconsistent state: order says 3 items were bought but stock still has 2")
        void autoCommitLeavesPartialOrderWhenStockIsInsufficient() throws SQLException {
            try (Connection connection = jdbcUtil.getConnection()) {
                assertTrue(connection.getAutoCommit());

                long orderId = createOrder(connection, 3);
                createOrderItem(connection, orderId, 3);

                SQLException exception = assertThrows(
                        SQLException.class,
                        () -> deductStock(connection, 3)
                );

                assertEquals("23514", exception.getSQLState());
            }

            long orderedQuantity = countRows(
                    "SELECT COALESCE(SUM(quantity), 0) FROM order_item WHERE product_id = ?", productId);
            long availableStock = countRows("SELECT stock_quantity FROM product WHERE id = ?", productId);

            assertEquals(1L, countRows("SELECT COUNT(*) FROM \"order\""));
            assertEquals(3L, orderedQuantity, "The order item records 3 purchased units");
            assertEquals(2L, availableStock, "Inventory was not deducted because the update failed");
            assertTrue(orderedQuantity > availableStock,
                    "Inconsistent state: recorded purchases exceed the available inventory");
        }

        @Test
        @DisplayName("Rollback removes all order writes when stock update fails")
        void transactionRollsBackOrderWhenStockIsInsufficient() throws SQLException {
            try (Connection connection = jdbcUtil.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    long orderId = createOrder(connection, 3);
                    createOrderItem(connection, orderId, 3);

                    SQLException exception = assertThrows(
                            SQLException.class,
                            () -> deductStock(connection, 3)
                    );
                    assertEquals("23514", exception.getSQLState());
                    connection.rollback();
                } catch (SQLException | RuntimeException e) {
                    connection.rollback();
                    throw e;
                }
            }

            assertEquals(0L, countRows("SELECT COUNT(*) FROM \"order\""));
            assertEquals(0L, countRows("SELECT COUNT(*) FROM order_item"));
            assertEquals(2L, countRows("SELECT stock_quantity FROM product WHERE id = ?", productId));
        }

        @Test
        @DisplayName("Commit persists order, item, and stock deduction together")
        void transactionCommitsOrderAndStockTogether() throws SQLException {
            try (Connection connection = jdbcUtil.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    long orderId = createOrder(connection, 2);
                    createOrderItem(connection, orderId, 2);
                    deductStock(connection, 2);
                    connection.commit();
                } catch (SQLException | RuntimeException e) {
                    connection.rollback();
                    throw e;
                }
            }

            assertEquals(1L, countRows("SELECT COUNT(*) FROM \"order\""));
            assertEquals(1L, countRows("SELECT COUNT(*) FROM order_item"));
            assertEquals(0L, countRows("SELECT stock_quantity FROM product WHERE id = ?", productId));
        }
    }

    private long createOrder(Connection connection, int quantity) throws SQLException {
        return queryForId(connection, """
                INSERT INTO "order" (user_id, status, total_amount)
                VALUES (?, ?, ?)
                RETURNING id
                """, statement -> {
            statement.setLong(1, userId);
            statement.setString(2, "CREATED");
            statement.setBigDecimal(3, BigDecimal.valueOf(150L * quantity));
        });
    }

    private void createOrderItem(Connection connection, long orderId, int quantity) throws SQLException {
        execute(connection, """
                INSERT INTO order_item (order_id, product_id, quantity, unit_price)
                VALUES (?, ?, ?, ?)
                """, statement -> {
            statement.setLong(1, orderId);
            statement.setLong(2, productId);
            statement.setInt(3, quantity);
            statement.setBigDecimal(4, BigDecimal.valueOf(150));
        });
    }

    private void deductStock(Connection connection, int quantity) throws SQLException {
        execute(connection, "UPDATE product SET stock_quantity = stock_quantity - ? WHERE id = ?", statement -> {
            statement.setInt(1, quantity);
            statement.setLong(2, productId);
        });
    }

    private long queryForId(Connection connection, String sql, StatementBinder binder)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.accept(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Insert did not return an ID");
                }
                return resultSet.getLong(1);
            }
        }
    }

    private void execute(Connection connection, String sql, StatementBinder binder) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.accept(statement);
            statement.executeUpdate();
        }
    }

    @FunctionalInterface
    private interface StatementBinder {
        void accept(PreparedStatement statement) throws SQLException;
    }

    private long countRows(String sql, Object... parameters) {
        return jdbcUtil.findOne(sql, resultSet -> {
            try {
                return resultSet.getLong(1);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to read transaction test result", e);
            }
        }, parameters).orElseThrow(() -> new IllegalStateException("Count query returned no result"));
    }
}
