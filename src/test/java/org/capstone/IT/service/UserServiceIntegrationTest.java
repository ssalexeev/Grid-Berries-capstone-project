package org.capstone.IT.service;


import org.capstone.IT.AbstractIntegrationTest;
import org.capstone.domain.User;
import org.capstone.domain.enums.Role;
import org.capstone.repository.jdbc.UserRepository;
import org.capstone.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest
public class UserServiceIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Persists new user with default BUYER role when role is null")
    void shouldRegisterUserWithDefaultRole() {
        String email = "john.doe@example.com";
        String passwordHash = "argon2id$hashed_secret";

        User savedUser = userService.registerUser(email, passwordHash, null);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getEmail()).isEqualTo(email);
        assertThat(savedUser.getPasswordHash()).isEqualTo(passwordHash);
        assertThat(savedUser.getRole()).isEqualTo(Role.BUYER);
        assertThat(savedUser.getIsActive()).isTrue();
        assertThat(savedUser.getCreatedAt()).isNotNull();

        // Verify persistence directly in the database
        Optional<User> found = userRepository.findById(savedUser.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo(email);
    }

    @Test
    @DisplayName("Authenticate User with correct credentials")
    void shouldAuthenticateUserWithCorrectCredentials() {
        User user = getRegisteredUser();

        User authUser = userService.authenticate(user.getEmail(), user.getPasswordHash());

        assertThat(authUser.getEmail()).isEqualTo(user.getEmail());
        assertThat(authUser.getPasswordHash()).isEqualTo(user.getPasswordHash());
    }

    @Test
    void shouldGetUserById() {
        User user = getRegisteredUser();


        User userById = userService.getUserById(user.getId());

        assertThat(userById.getId()).isEqualTo(user.getId());
        assertThat(userById.getEmail()).isEqualTo(user.getEmail());
    }

    @Test
    void shouldUpdateExistUserEmail() {
                User user = getRegisteredUser();


        String newEmail = "newjohn..doe@example.com";
        user.setEmail(newEmail);
        User updatedUser = userService.updateUser(user);

        assertThat(updatedUser.getEmail()).isEqualTo(newEmail);
    }

    @Test
    void shouldDeactivateUser() {
        User user = getRegisteredUser();

        userService.deactivateUser(user.getId());

        User deactivatedUser = userService.getUserById(user.getId());
        assertThat(deactivatedUser.getIsActive()).isFalse();
    }

    @Test
    void shouldDeleteUser() {
        User user = getRegisteredUser();
        userService.deleteUser(user.getId());

        assertThatThrownBy(() -> userService.getUserById(user.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User not found with ID: " + user.getId());
    }


    User getRegisteredUser() {
        String email = "john.doe@example.com";
        String passwordHash = "argon2id$hashed_secret";
        return userService.registerUser(email, passwordHash, null);
    }
}
