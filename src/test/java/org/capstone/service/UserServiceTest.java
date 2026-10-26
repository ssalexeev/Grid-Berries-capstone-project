package org.capstone.service;


import org.capstone.domain.User;
import org.capstone.domain.enums.Role;
import org.capstone.repository.jdbc.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Nested
    class RegisterUserTests {

        @Test
        void registerUser_WhenEmailIsNullOrEmptyOrBlank_ThrowsIllegalArgumentException() {
            String invalidEmail = "";
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> userService.registerUser(invalidEmail, "hashedPass123", Role.BUYER)
            );

            assertEquals("Email cannot be empty.", ex.getMessage());
            verifyNoInteractions(userRepository);
        }

       @Test
        void registerUser_WhenPasswordHashIsNullOrEmptyOrBlank_ThrowsIllegalArgumentException() {
            String invalidPassword = "";
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> userService.registerUser("user@example.com", invalidPassword, Role.BUYER)
            );

            assertEquals("Password cannot be empty.", ex.getMessage());
            verifyNoInteractions(userRepository);
        }

        @Test
        void registerUser_WhenEmailAlreadyExists_ThrowsIllegalStateException() {
            String existingEmail = "existing@example.com";
            User existingUser = User.builder().email(existingEmail).build();

            when(userRepository.findAll()).thenReturn(List.of(existingUser));

            IllegalStateException ex = assertThrows(
                    IllegalStateException.class,
                    () -> userService.registerUser("EXISTING@example.com", "hash123", Role.BUYER)
            );

            assertEquals("User with email EXISTING@example.com already exists.", ex.getMessage());
            verify(userRepository, times(1)).findAll();
            verify(userRepository, never()).save(any());
        }

        @Test
        void registerUser_WhenValidInputsAndRoleGiven_SavesAndReturnsUser() {
            String email = "newuser@example.com";
            String passwordHash = "hash123";
            Role role = Role.ADMIN;

            when(userRepository.findAll()).thenReturn(List.of());
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            User result = userService.registerUser(email, passwordHash, role);

            assertNotNull(result);
            assertEquals(email, result.getEmail());
            assertEquals(passwordHash, result.getPasswordHash());
            assertEquals(Role.ADMIN, result.getRole());
            assertTrue(result.getIsActive());
            assertNotNull(result.getCreatedAt());

            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        void registerUser_WhenRoleIsNull_DefaultsToBuyerRole() {
            String email = "buyer@example.com";
            String passwordHash = "hash123";

            when(userRepository.findAll()).thenReturn(List.of());
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            User result = userService.registerUser(email, passwordHash, null);

            assertEquals(Role.BUYER, result.getRole());
            verify(userRepository, times(1)).save(any(User.class));
        }
    }

    @Nested
    class AuthenticateTests {

        @Test
        void authenticate_WhenEmailOrPasswordIsNull_ReturnsNull() {
            assertNull(userService.authenticate(null, "hash"));
            assertNull(userService.authenticate("test@example.com", null));
            verifyNoInteractions(userRepository);
        }

        @Test
        void authenticate_WhenCredentialsMatchAndUserIsActive_ReturnsUser() {
            String email = "active@example.com";
            String passHash = "correct_hash";

            User activeUser = User.builder()
                    .id(1L)
                    .email(email)
                    .passwordHash(passHash)
                    .isActive(true)
                    .build();

            when(userRepository.findAll()).thenReturn(List.of(activeUser));

            User result = userService.authenticate("ACTIVE@example.com", passHash);

            assertNotNull(result);
            assertEquals(1L, result.getId());
        }

        @Test
        void authenticate_WhenUserIsInactive_ReturnsNull() {
            User inactiveUser = User.builder()
                    .email("inactive@example.com")
                    .passwordHash("pass123")
                    .isActive(false)
                    .build();

            when(userRepository.findAll()).thenReturn(List.of(inactiveUser));

            User result = userService.authenticate("inactive@example.com", "pass123");

            assertNull(result);
        }

        @Test
        void authenticate_WhenPasswordMismatchesOrNull_ReturnsNull() {
            User user = User.builder()
                    .email("user@example.com")
                    .passwordHash("correct_pass")
                    .isActive(true)
                    .build();

            when(userRepository.findAll()).thenReturn(List.of(user));

            assertNull(userService.authenticate("user@example.com", "wrong_pass"));
        }
    }

    // ==========================================
    // getUserById & getUserByEmail Tests
    // ==========================================
    @Nested
    class GetUserTests {

        @Test
        void getUserById_WhenUserExists_ReturnsUser() {
            Long userId = 1L;
            User user = User.builder().id(userId).email("user@example.com").build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            User result = userService.getUserById(userId);

            assertEquals(userId, result.getId());
        }

        @Test
        void getUserById_WhenUserDoesNotExist_ThrowsIllegalArgumentException() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> userService.getUserById(99L)
            );

            assertEquals("User not found with ID: 99", ex.getMessage());
        }

        @Test
        void getUserByEmail_WhenUserExists_ReturnsUser() {
            String email = "test@example.com";
            User user = User.builder().id(1L).email(email).build();

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

            User result = userService.getUserByEmail(email);

            assertEquals(email, result.getEmail());
        }

        @Test
        void getUserByEmail_WhenUserDoesNotExist_ThrowsIllegalArgumentException() {
            when(userRepository.findByEmail("absent@example.com")).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> userService.getUserByEmail("absent@example.com")
            );

            assertEquals("User not found with email: absent@example.com", ex.getMessage());
        }
    }

    // ==========================================
    // getUsersByRole Tests
    // ==========================================
    @Nested
    class GetUsersByRoleTests {

        @Test
        void getUsersByRole_WhenRoleIsNull_ThrowsIllegalArgumentException() {
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> userService.getUsersByRole(null)
            );

            assertEquals("Role cannot be null.", ex.getMessage());
            verifyNoInteractions(userRepository);
        }

        @Test
        void getUsersByRole_WhenRoleIsValid_ReturnsUsersFromRepository() {
            User admin = User.builder().role(Role.ADMIN).build();
            when(userRepository.findAllByRole(Role.ADMIN)).thenReturn(List.of(admin));

            List<User> result = userService.getUsersByRole(Role.ADMIN);

            assertEquals(1, result.size());
            assertEquals(Role.ADMIN, result.get(0).getRole());
            verify(userRepository, times(1)).findAllByRole(Role.ADMIN);
        }
    }

    @Nested
    class MutationTests {

        @Test
        void updateUser_WhenUserOrIdIsNull_ThrowsIllegalArgumentException() {
            User userWithoutId = User.builder().email("noid@example.com").build();

            assertThrows(IllegalArgumentException.class, () -> userService.updateUser(null));
            assertThrows(IllegalArgumentException.class, () -> userService.updateUser(userWithoutId));

            verify(userRepository, never()).save(any());
        }

        @Test
        void updateUser_WhenUserExists_UpdatesUser() {
            Long userId = 10L;
            User existingUser = User.builder().id(userId).email("user@example.com").build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
            when(userRepository.save(existingUser)).thenReturn(existingUser);

            User result = userService.updateUser(existingUser);

            assertNotNull(result);
            verify(userRepository, times(1)).findById(userId);
            verify(userRepository, times(1)).save(existingUser);
        }

        @Test
        void deactivateUser_WhenUserExists_SetsIsActiveToFalseAndUpdateRepository() {
            Long userId = 5L;
            User activeUser = User.builder().id(userId).isActive(true).build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

            userService.deactivateUser(userId);

            assertFalse(activeUser.getIsActive());
            verify(userRepository, times(1)).save(activeUser);
        }

        @Test
        void deleteUser_WhenUserExists_DeletesFromRepository() {
            Long userId = 5L;
            User user = User.builder().id(userId).build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            userService.deleteUser(userId);

            verify(userRepository, times(1)).delete(user);
        }

        @Test
        void getAllUsers_ReturnsListFromRepository() {
            when(userRepository.findAll()).thenReturn(List.of(new User(), new User()));

            List<User> users = userService.getAllUsers();

            assertEquals(2, users.size());
            verify(userRepository, times(1)).findAll();
        }
    }
}
