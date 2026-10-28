package org.capstone.repository.jdbc;

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

import org.capstone.domain.User;
import org.capstone.domain.enums.Role;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class JdbcUtilTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withInitScript("db/changelog/changes/v1__init_schema.sql");

    private JdbcUtil jdbcUtil;

    @BeforeAll
    static void setupDatabase() {
        JdbcUtil.setCredentials(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
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
    void setUp() {
        jdbcUtil = new JdbcUtil();

        jdbcUtil.execute("TRUNCATE TABLE \"user\" RESTART IDENTITY CASCADE");

        jdbcUtil.execute("""
                    INSERT INTO "user" (email, password_hash, role, is_active) VALUES
                    ('john.doe@example.com', '$2a$12$eImiTXuWVxfM37uY4JANjO.Gk4A4L4dE2f1Gz6h1.g98j4X5Y6Z7a', 'buyer', true),
                    ('jane.smith@example.com', '$2a$12$eImiTXuWVxfM37uY4JANjO.Gk4A4L4dE2f1Gz6h1.g98j4X5Y6Z7b', 'seller', true),
                    ('tech.vendor@example.com', '$2a$12$eImiTXuWVxfM37uY4JANjO.Gk4A4L4dE2f1Gz6h1.g98j4X5Y6Z7c', 'seller', true),
                    ('admin@marketplace.com', '$2a$12$eImiTXuWVxfM37uY4JANjO.Gk4A4L4dE2f1Gz6h1.g98j4X5Y6Z7d', 'admin', true),
                    ('inactive.user@example.com', '$2a$12$eImiTXuWVxfM37uY4JANjO.Gk4A4L4dE2f1Gz6h1.g98j4X5Y6Z7e', 'buyer', false);
                """);
    }

    private User mapUser(ResultSet rs) {
        try {
            return User.builder()
                    .id(rs.getLong("id"))
                    .email(rs.getString("email"))
                    .passwordHash(rs.getString("password_hash"))
                    .role(Role.valueOf(rs.getString("role").toUpperCase()))
                    .isActive(rs.getBoolean("is_active"))
                    .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                    .build();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to map User record", e);
        }
    }

    @Nested
    @DisplayName("findOne() Tests")
    class FindOneTests {

        @Test
        @DisplayName("Positive: Find single active user by email")
        void findOne_Success() {
            Optional<User> user = jdbcUtil.findOne(
                    "SELECT * FROM \"user\" WHERE email = ?",
                    JdbcUtilTest.this::mapUser,
                    "admin@marketplace.com"
            );

            assertTrue(user.isPresent());
            assertEquals(4L, user.get().getId());
            assertEquals(Role.ADMIN, user.get().getRole());
            assertTrue(user.get().getIsActive());
        }

        @Test
        @DisplayName("Positive: Should return Optional.empty when email does not exist")
        void findOne_NotFound() {
            Optional<User> user = jdbcUtil.findOne(
                    "SELECT * FROM \"user\" WHERE email = ?",
                    JdbcUtilTest.this::mapUser,
                    "nonexistent@example.com"
            );

            assertTrue(user.isEmpty());
        }

        @Test
        @DisplayName("Negative: Throw IllegalStateException cause when query matches multiple rows")
        void findOne_MultipleResults_ThrowsException() {
            IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                    jdbcUtil.findOne("SELECT * FROM \"user\" WHERE role = ?", JdbcUtilTest.this::mapUser, "seller")
            );

            assertTrue(exception.getMessage().contains("Query returned more than 1 result"));
        }
    }

    @Nested
    @DisplayName("findMany() Tests")
    class FindManyTests {

        @Test
        @DisplayName("Positive: Return all active buyers")
        void findMany_Filtered() {
            List<User> buyers = jdbcUtil.findMany(
                    "SELECT * FROM \"user\" WHERE role = ? AND is_active = ?",
                    JdbcUtilTest.this::mapUser,
                    "buyer",
                    true
            );

            assertEquals(1, buyers.size());
            assertEquals("john.doe@example.com", buyers.get(0).getEmail());
        }

        @Test
        @DisplayName("Positive: Fetch all records ordered by ID")
        void findMany_AllUsers() {
            List<User> users = jdbcUtil.findMany(
                    "SELECT * FROM \"user\" ORDER BY id ASC",
                    JdbcUtilTest.this::mapUser
            );

            assertEquals(5, users.size());
            assertEquals("john.doe@example.com", users.get(0).getEmail());
            assertFalse(users.get(4).getIsActive()); // inactive.user@example.com
        }

        @Test
        @DisplayName("Positive: Return empty list when condition matches no rows")
        void findMany_NoMatches() {
            List<User> superusers = jdbcUtil.findMany(
                    "SELECT * FROM \"user\" WHERE role = ?",
                    JdbcUtilTest.this::mapUser,
                    "superuser"
            );

            assertNotNull(superusers);
            assertTrue(superusers.isEmpty());
        }
    }

    @Nested
    @DisplayName("execute() Updates & Writes")
    class ExecuteTests {

        @Test
        @DisplayName("Positive: Insert a new user record")
        void execute_InsertUser() {
            jdbcUtil.execute(
                    "INSERT INTO \"user\" (email, password_hash, role, is_active) VALUES (?, ?, ?, ?)",
                    "new.buyer@example.com", "$2a$12$hash", "buyer", true
            );

            Optional<User> user = jdbcUtil.findOne(
                    "SELECT * FROM \"user\" WHERE email = ?",
                    JdbcUtilTest.this::mapUser,
                    "new.buyer@example.com"
            );

            assertTrue(user.isPresent());
            assertEquals(Role.BUYER, user.get().getRole());
        }

        @Test
        @DisplayName("Positive: Deactivate user by updating is_active flag")
        void execute_UpdateUser() {
            jdbcUtil.execute("UPDATE \"user\" SET is_active = ? WHERE email = ?", false, "john.doe@example.com");

            Optional<User> user = jdbcUtil.findOne(
                    "SELECT * FROM \"user\" WHERE email = ?",
                    JdbcUtilTest.this::mapUser,
                    "john.doe@example.com"
            );

            assertTrue(user.isPresent());
            assertFalse(user.get().getIsActive());
        }

        @Test
        @DisplayName("Positive: Delete inactive user account")
        void execute_DeleteUser() {
            jdbcUtil.execute("DELETE FROM \"user\" WHERE is_active = ?", false);

            List<User> remaining = jdbcUtil.findMany("SELECT * FROM \"user\"", JdbcUtilTest.this::mapUser);
            assertEquals(4, remaining.size());
        }

        @Test
        @DisplayName("Negative: Reject duplicate email insertion due to UNIQUE constraint")
        void execute_DuplicateEmail_ThrowsException() {
            assertThrows(RuntimeException.class, () ->
                    jdbcUtil.execute(
                            """
                                    INSERT INTO "user" (email, password_hash, role) VALUES (?, ?, ?)""",
                            "john.doe@example.com", "$2a$12$hash", "buyer"
                    )
            );
        }
    }

    @Nested
    @DisplayName("execute() with Consumer PreparedStatement")
    class ConsumerExecuteTests {

        @Test
        @DisplayName("Positive: Insert user using Consumer parameter setting")
        void execute_ConsumerBinding() {
            jdbcUtil.execute(
                    """
                             INSERT INTO "user" (email, password_hash, role, is_active) VALUES (?, ?, ?, ?)
                            """,
                    stmt -> {
                        try {
                            stmt.setString(1, "consumer.user@example.com");
                            stmt.setString(2, "$2a$12$hashconsumer");
                            stmt.setString(3, "seller");
                            stmt.setBoolean(4, true);
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    }
            );

            Optional<User> user = jdbcUtil.findOne(
                    """
                            SELECT * FROM "user" WHERE email = ?""",
                    JdbcUtilTest.this::mapUser,
                    "consumer.user@example.com"
            );

            assertTrue(user.isPresent());
            assertEquals(Role.SELLER, user.get().getRole());
        }
    }
}