package org.capstone.repository.jdbc;

import org.capstone.domain.SellerProfile;
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

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class SellerProfileRepositoryTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withInitScript("db/changelog/changes/v1__init_schema.sql");

    private JdbcUtil jdbcUtil;
    private UserRepository userRepository;
    private SellerProfileRepository repository;

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
        userRepository = new UserRepository(jdbcUtil);
        repository = new SellerProfileRepository(jdbcUtil, userRepository);
        jdbcUtil.execute("TRUNCATE TABLE \"user\" RESTART IDENTITY CASCADE");
    }

    private User createUser() {
        return userRepository.save(User.builder()
                .email("seller@example.com")
                .passwordHash("password-hash")
                .role(Role.SELLER)
                .isActive(true)
                .build());
    }

    private SellerProfile createProfile(User user) {
        return SellerProfile.builder()
                .user(user)
                .storeName("Berry Farm")
                .storeDescription("Fresh berries")
                .contactInfo("contact@berryfarm.example")
                .createdAt(LocalDateTime.of(2025, 3, 4, 5, 6, 7))
                .build();
    }

    @Test
    void findAll_WhenNoProfilesExist_ReturnsEmptyList() {
        assertTrue(repository.findAll().isEmpty());
    }

    @Nested
    class FindByIdTests {

        @Test
        void findById_WhenProfileExists_ReturnsProfileWithUser() {
            User user = createUser();
            SellerProfile saved = repository.save(createProfile(user));

            Optional<SellerProfile> result = repository.findById(saved.getId());

            assertTrue(result.isPresent());
            assertEquals(saved, result.orElseThrow());
            assertEquals(user, result.orElseThrow().getUser());
        }

        @Test
        void findById_WhenProfileDoesNotExist_ReturnsEmpty() {
            assertTrue(repository.findById(Long.MAX_VALUE).isEmpty());
        }

        @Test
        void findById_WhenIdIsNull_ReturnsEmpty() {
            assertTrue(repository.findById(null).isEmpty());
        }
    }

    @Test
    void findByUserId_WhenProfileExists_ReturnsProfile() {
        User user = createUser();
        SellerProfile saved = repository.save(createProfile(user));

        assertEquals(saved, repository.findByUserId(user.getId()).orElseThrow());
    }

    @Nested
    class SaveTests {

        @Test
        void save_WhenProfileIsNull_ThrowsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () -> repository.save(null));
        }

        @Test
        void save_WhenUserIsMissing_ThrowsIllegalArgumentException() {
            SellerProfile profile = createProfile(null);

            assertThrows(IllegalArgumentException.class, () -> repository.save(profile));
        }

        @Test
        void save_WhenUserHasNoId_ThrowsIllegalArgumentException() {
            User user = User.builder().email("unsaved@example.com").build();

            assertThrows(IllegalArgumentException.class, () -> repository.save(createProfile(user)));
        }

        @Test
        void save_WhenNewProfile_PersistsAllFieldsAndGeneratedId() {
            User user = createUser();
            SellerProfile profile = createProfile(user);

            SellerProfile saved = repository.save(profile);

            assertNotNull(saved.getId());
            assertEquals("Berry Farm", saved.getStoreName());
            assertEquals("Fresh berries", saved.getStoreDescription());
            assertEquals("contact@berryfarm.example", saved.getContactInfo());
            assertEquals(profile.getCreatedAt(), saved.getCreatedAt());
            assertEquals(user, saved.getUser());
        }

        @Test
        void save_WhenExistingProfile_UpdatesAllProfileFields() {
            User user = createUser();
            SellerProfile saved = repository.save(createProfile(user));
            LocalDateTime updatedAt = LocalDateTime.of(2026, 6, 7, 8, 9, 10);
            saved.setStoreName("Updated Farm");
            saved.setStoreDescription("Updated description");
            saved.setContactInfo("updated@berryfarm.example");
            saved.setCreatedAt(updatedAt);

            SellerProfile updated = repository.save(saved);

            assertEquals(saved.getId(), updated.getId());
            assertEquals("Updated Farm", updated.getStoreName());
            assertEquals("Updated description", updated.getStoreDescription());
            assertEquals("updated@berryfarm.example", updated.getContactInfo());
            assertEquals(updatedAt, updated.getCreatedAt());
            assertEquals(user, updated.getUser());
        }
    }

    @Nested
    class FindAllTests {

        @Test
        void findAll_ReturnsAllProfiles() {
            User firstUser = createUser();
            User secondUser = userRepository.save(User.builder()
                    .email("another-seller@example.com")
                    .passwordHash("password-hash")
                    .role(Role.SELLER)
                    .isActive(true)
                    .build());
            SellerProfile firstProfile = repository.save(createProfile(firstUser));
            SellerProfile secondProfile = repository.save(SellerProfile.builder()
                    .user(secondUser)
                    .storeName("Second Farm")
                    .storeDescription("More berries")
                    .contactInfo("second@berryfarm.example")
                    .build());

            List<SellerProfile> profiles = repository.findAll();

            assertEquals(2, profiles.size());
            assertTrue(profiles.contains(firstProfile));
            assertTrue(profiles.contains(secondProfile));
        }

    }

    @Nested
    class DeleteTests {

        @Test
        void delete_WhenProfileIsNull_ThrowsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () -> repository.delete(null));
        }

        @Test
        void delete_WhenProfileHasNoId_ThrowsIllegalArgumentException() {
            SellerProfile profile = SellerProfile.builder().storeName("Unpersisted").build();

            assertThrows(IllegalArgumentException.class, () -> repository.delete(profile));
        }

        @Test
        void delete_WhenProfileExists_RemovesIt() {
            SellerProfile saved = repository.save(createProfile(createUser()));

            repository.delete(saved);

            assertTrue(repository.findById(saved.getId()).isEmpty());
            assertTrue(repository.findAll().isEmpty());
        }
    }
}
