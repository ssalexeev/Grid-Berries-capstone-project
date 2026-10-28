package org.capstone.repository.jdbc;


import org.capstone.domain.User;
import org.capstone.domain.enums.Role;
import org.capstone.repository.jdbc.util.JdbcUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


@Testcontainers
class UserRepositoryTest {
    private UserRepository repository;

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withInitScript("db/changelog/changes/v1__init_schema.sql");


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
        JdbcUtil jdbcUtil = new JdbcUtil();
        repository = new UserRepository(jdbcUtil);
        jdbcUtil.execute("TRUNCATE TABLE \"user\" RESTART IDENTITY CASCADE");
    }


    @Nested
    class FindByIdTests {

        @Test
        void findById_WhenIdIsNull_ReturnsEmptyOptional() {
            Optional<User> result = repository.findById(null);

            assertTrue(result.isEmpty());
        }

        @Test
        void findById_WhenUserExists_ReturnsOptionalWithUser() {
            User user = User.builder()
                    .email("alex@example.com")
                    .passwordHash("hashed_password_123")
                    .role(Role.BUYER)
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .build();

            User saved = repository.save(user);

            Optional<User> result = repository.findById(saved.getId());

            assertTrue(result.isPresent());
            assertEquals(saved.getId(), result.get().getId());
            assertEquals("alex@example.com", result.get().getEmail());
            assertEquals("hashed_password_123", result.get().getPasswordHash());
            assertEquals(Role.BUYER, result.get().getRole());
            assertTrue(result.get().getIsActive());
        }

        @Test
        void findById_WhenUserDoesNotExist_ReturnsEmptyOptional() {
            Optional<User> result = repository.findById(999L);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class FindByEmailTests {

        @Test
        void findByEmail_WhenEmailDoesNotExist_ReturnsEmptyOptional() {
            Optional<User> result = repository.findByEmail("notfound@example.com");

            assertTrue(result.isEmpty());
        }

        @Test
        void findByEmail_WhenUserExists_ReturnsUser() {
            User user = User.builder()
                    .email("Alex@Example.com")
                    .passwordHash("secret123")
                    .role(Role.SELLER)
                    .isActive(true)
                    .build();

            repository.save(user);

            Optional<User> result = repository.findByEmail("Alex@Example.com");

            assertTrue(result.isPresent());
            assertEquals("Alex@Example.com", result.get().getEmail());
            assertEquals(Role.SELLER, result.get().getRole());
        }

        @Test
        void findByEmail_WhenEmailDiffersInCaseOrWhitespace_ReturnsEmptyOptional() {
            User user = User.builder()
                    .email("Alex@Example.com")
                    .passwordHash("secret123")
                    .role(Role.SELLER)
                    .isActive(true)
                    .build();
            repository.save(user);

            Optional<User> result = repository.findByEmail("  alex@example.com  ");

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class SaveAndUpdateTests {

        @Test
        void save_WhenUserIsNull_ThrowsIllegalArgumentException() {
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> repository.save(null)
            );

            assertEquals("User cannot be null", ex.getMessage());
        }

        @Test
        void save_WhenNewUser_AssignsDatabaseIdAndPersistsUser() {
            LocalDateTime createdAt = LocalDateTime.of(2024, 1, 2, 3, 4, 5, 123_000_000);
            User newUser = User.builder()
                    .email("sam@example.com")
                    .passwordHash("pass456")
                    .role(Role.BUYER)
                    .isActive(true)
                    .createdAt(createdAt)
                    .build();

            User savedUser = repository.save(newUser);

            assertNotNull(savedUser.getId());
            assertEquals(savedUser, repository.findById(savedUser.getId()).orElseThrow());
            assertEquals(savedUser, repository.findByEmail("sam@example.com").orElseThrow());
        }

        @Test
        void save_WhenExistingUser_UpdatesAllUserParameters() {
            LocalDateTime createdAt = LocalDateTime.of(2025, 5, 6, 7, 8, 9, 456_000_000);
            User user = User.builder()
                    .email("original@example.com")
                    .passwordHash("old-hash")
                    .role(Role.BUYER)
                    .isActive(true)
                    .createdAt(LocalDateTime.of(2024, 1, 2, 3, 4, 5, 123_000_000))
                    .build();

            User saved = repository.save(user);
            saved.setEmail("updated@example.com");
            saved.setPasswordHash("new-hash");
            saved.setRole(Role.ADMIN);
            saved.setIsActive(false);
            saved.setCreatedAt(createdAt);

            User updated = repository.save(saved);

            assertEquals(saved.getId(), updated.getId());
            assertEquals("updated@example.com", updated.getEmail());
            assertEquals("new-hash", updated.getPasswordHash());
            assertEquals(Role.ADMIN, updated.getRole());
            assertFalse(updated.getIsActive());
            assertEquals(createdAt, updated.getCreatedAt());
            assertEquals(updated, repository.findById(saved.getId()).orElseThrow());
        }
    }

    @Nested
    class DeleteTests {

        @Test
        void delete_WhenUserIsNull_ThrowsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () -> repository.delete(null));
        }

        @Test
        void delete_WhenUserHasNullId_ThrowsIllegalArgumentException() {
            User userWithoutId = User.builder().email("noid@example.com").build();

            assertThrows(IllegalArgumentException.class, () -> repository.delete(userWithoutId));
        }

        @Test
        void delete_WhenUserExists_RemovesUserFromDatabase() {
            User user = User.builder()
                    .email("alex@example.com")
                    .passwordHash("hash")
                    .role(Role.BUYER)
                    .isActive(true)
                    .build();

            User savedUser = repository.save(user);

            repository.delete(savedUser);

            assertTrue(repository.findById(savedUser.getId()).isEmpty());
            assertTrue(repository.findByEmail("alex@example.com").isEmpty());
        }
    }

    @Nested
    class QueryAllTests {

        @Test
        void findAll_ReturnsAllSavedUsers() {
            repository.save(User.builder().email("u1@example.com").passwordHash("hash")
                    .role(Role.BUYER).isActive(true).build());
            repository.save(User.builder().email("u2@example.com").passwordHash("hash")
                    .role(Role.BUYER).isActive(true).build());

            List<User> allUsers = repository.findAll();

            assertEquals(2, allUsers.size());
        }

        @Test
        void findAllByRole_WhenRoleIsNull_ReturnsEmptyList() {
            assertTrue(repository.findAllByRole(null).isEmpty());
        }

        @Test
        void findAllByRole_ReturnsOnlyMatchingUsers() {
            User admin1 = User.builder().email("a1@example.com").passwordHash("hash")
                    .role(Role.ADMIN).isActive(true).build();
            User admin2 = User.builder().email("a2@example.com").passwordHash("hash")
                    .role(Role.ADMIN).isActive(true).build();
            User customer = User.builder().email("c1@example.com").passwordHash("hash")
                    .role(Role.BUYER).isActive(true).build();

            repository.save(admin1);
            repository.save(admin2);
            repository.save(customer);

            List<User> admins = repository.findAllByRole(Role.ADMIN);

            assertEquals(2, admins.size());
            assertTrue(admins.stream().allMatch(u -> Role.ADMIN.equals(u.getRole())));
        }
    }
}