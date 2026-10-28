package org.capstone.transactions;

import org.capstone.repository.jdbc.util.JdbcUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *  Task:
 *  Keeping your domain and relational models in mind, think about the cases
 *  when you might need non-default transaction isolation levels.
 *  Describe these cases and, using the JDBC code from the previous modules,
 *  demonstrate how the system behaves when using the default isolation level and the correct isolation level
 */
@Testcontainers
class TransactionIsolationTest {
    private static final int PURCHASE_COUNT = 2;

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withInitScript("db/changelog/changes/v1__init_schema.sql");

    private static JdbcUtil jdbcUtil;
    private long userId;
    private long productId;

    @BeforeAll
    static void setupDatabase() {
        JdbcUtil.setCredentials(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        jdbcUtil = new JdbcUtil(4);
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
        userId = jdbcUtil.insertAndReturnId("""
                INSERT INTO "user" (email, password_hash, role)
                VALUES (?, ?, ?)
                RETURNING id
                """, "buyer@example.com", "password-hash", "BUYER");
        long sellerUserId = jdbcUtil.insertAndReturnId("""
                INSERT INTO "user" (email, password_hash, role)
                VALUES (?, ?, ?)
                RETURNING id
                """, "seller@example.com", "password-hash", "SELLER");
        long sellerId = jdbcUtil.insertAndReturnId("""
                INSERT INTO seller_profile (user_id, store_name)
                VALUES (?, ?)
                RETURNING id
                """, sellerUserId, "Berry Farm");
        productId = jdbcUtil.insertAndReturnId("""
                INSERT INTO product (seller_id, title, price, stock_quantity, type)
                VALUES (?, ?, ?, ?, ?)
                RETURNING id
                """, sellerId, "Last Box of Berries", 12.50, 1, "PHYSICAL_GOOD");
    }

    @Test
    @DisplayName("READ_COMMITTED can record two orders for the same last unit")
    void readCommittedAllowsConcurrentOverselling() throws Exception {
        PurchaseResults results = runConcurrentPurchases(Connection.TRANSACTION_READ_COMMITTED);

        assertEquals(Connection.TRANSACTION_READ_COMMITTED, results.observedIsolationLevel());
        assertEquals(PURCHASE_COUNT, results.successCount());
        assertEquals(2L, countRows("SELECT COUNT(*) FROM order_item"));
        assertEquals(-1L, countRows("SELECT stock_quantity FROM product WHERE id = ?", productId));
    }

    @Test
    @DisplayName("SERIALIZABLE aborts one order so the last unit is sold only once")
    void serializablePreventsConcurrentOverselling() throws Exception {
        PurchaseResults results = runConcurrentPurchases(Connection.TRANSACTION_SERIALIZABLE);

        assertEquals(Connection.TRANSACTION_SERIALIZABLE, results.observedIsolationLevel());
        assertEquals(1, results.successCount());
        assertEquals(1, results.serializationFailureCount());
        assertEquals(1L, countRows("SELECT COUNT(*) FROM order_item"));
        assertEquals(0L, countRows("SELECT stock_quantity FROM product WHERE id = ?", productId));
    }

    private PurchaseResults runConcurrentPurchases(int isolationLevel)
            throws InterruptedException, ExecutionException, TimeoutException {
        CountDownLatch transactionsReady = new CountDownLatch(PURCHASE_COUNT);
        CountDownLatch startPurchases = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(PURCHASE_COUNT);
        Future<PurchaseResult> first = executor.submit(
                () -> purchaseOneUnit(isolationLevel, transactionsReady, startPurchases));
        Future<PurchaseResult> second = executor.submit(
                () -> purchaseOneUnit(isolationLevel, transactionsReady, startPurchases));

        try {
            assertTrue(transactionsReady.await(10, TimeUnit.SECONDS),
                    "Both transactions should read inventory before either purchases");
        } finally {
            startPurchases.countDown();
        }

        try {
            PurchaseResult firstResult = first.get(15, TimeUnit.SECONDS);
            PurchaseResult secondResult = second.get(15, TimeUnit.SECONDS);
            return new PurchaseResults(
                    firstResult.isolationLevel(),
                    (firstResult.committed() ? 1 : 0) + (secondResult.committed() ? 1 : 0),
                    (firstResult.isSerializationFailure() ? 1 : 0)
                            + (secondResult.isSerializationFailure() ? 1 : 0)
            );
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS),
                    "Purchase transactions should terminate");
        }
    }

    private PurchaseResult purchaseOneUnit(int isolationLevel,
                                           CountDownLatch transactionsReady,
                                           CountDownLatch startPurchases) {
        boolean announcedReady = false;
        try (Connection connection = jdbcUtil.getConnection()) {
            connection.setTransactionIsolation(isolationLevel);
            connection.setAutoCommit(false);
            try {
                int observedIsolation = connection.getTransactionIsolation();
                int stockBeforePurchase = readStock(connection);
                if (stockBeforePurchase < 1) {
                    throw new IllegalStateException("Test expected each transaction to observe one unit");
                }

                transactionsReady.countDown();
                announcedReady = true;
                if (!startPurchases.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting to start concurrent purchases");
                }

                long orderId = insertOrder(connection);
                insertOrderItem(connection, orderId);
                deductOneUnit(connection);
                connection.commit();
                return new PurchaseResult(observedIsolation, true, false);
            } catch (SQLException e) {
                connection.rollback();
                return new PurchaseResult(
                        connection.getTransactionIsolation(),
                        false,
                        "40001".equals(e.getSQLState())
                );
            } catch (InterruptedException | RuntimeException e) {
                connection.rollback();
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                throw new IllegalStateException("Concurrent purchase failed unexpectedly", e);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not run concurrent purchase", e);
        } finally {
            if (!announcedReady) {
                transactionsReady.countDown();
            }
        }
    }

    private int readStock(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT stock_quantity FROM product WHERE id = ?")) {
            statement.setLong(1, productId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Product not found");
                }
                return resultSet.getInt(1);
            }
        }
    }

    private long insertOrder(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO "order" (user_id, status, total_amount)
                VALUES (?, 'CREATED', ?)
                RETURNING id
                """)) {
            statement.setLong(1, userId);
            statement.setBigDecimal(2, java.math.BigDecimal.valueOf(12.50));
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Order insert did not return an ID");
                }
                return resultSet.getLong(1);
            }
        }
    }

    private void insertOrderItem(Connection connection, long orderId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO order_item (order_id, product_id, quantity, unit_price)
                VALUES (?, ?, 1, ?)
                """)) {
            statement.setLong(1, orderId);
            statement.setLong(2, productId);
            statement.setBigDecimal(3, java.math.BigDecimal.valueOf(12.50));
            statement.executeUpdate();
        }
    }

    private void deductOneUnit(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE product SET stock_quantity = stock_quantity - 1 WHERE id = ?
                """)) {
            statement.setLong(1, productId);
            statement.executeUpdate();
        }
    }

    private long countRows(String query, Object... parameters) {
        return jdbcUtil.findOne(query, resultSet -> {
            try {
                return resultSet.getLong(1);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to read transaction isolation test result", e);
            }
        }, parameters).orElseThrow(() -> new IllegalStateException("Count query returned no result"));
    }

    private record PurchaseResult(int isolationLevel, boolean committed, boolean isSerializationFailure) {
    }

    private record PurchaseResults(int observedIsolationLevel, int successCount, int serializationFailureCount) {
    }
}
