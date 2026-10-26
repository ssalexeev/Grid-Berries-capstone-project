package org.capstone.service;

import org.capstone.domain.User;
import org.capstone.domain.enums.Role;
import org.capstone.repository.jdbc.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User registerUser(String email, String passwordHash, Role role) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be empty.");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }

        // Check for duplicate email across all existing users
        boolean emailExists = userRepository.findAll().stream()
                .anyMatch(u -> email.equalsIgnoreCase(u.getEmail()));
        if (emailExists) {
            throw new IllegalStateException("User with email " + email + " already exists.");
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordHash)
                .role(role != null ? role : Role.BUYER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        return userRepository.save(user);
    }

    public User authenticate(String email, String passwordHash) {
        if (email == null || passwordHash == null) {
            return null;
        }

        return userRepository.findAll().stream()
                .filter(u -> email.equalsIgnoreCase(u.getEmail()) && u.getIsActive())
                .filter(u -> u.getPasswordHash() != null && u.getPasswordHash().equals(passwordHash))
                .findFirst()
                .orElse(null);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));
    }

    public User getUserByEmail(String email){
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
    }

    public List<User> getUsersByRole(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null.");
        }
        return userRepository.findAllByRole(role);
    }

    public User updateUser(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User or User ID cannot be null for update.");
        }
        getUserById(user.getId());

        return userRepository.save(user);
    }

    public void deactivateUser(Long id) {
        User user = getUserById(id);
        user.setIsActive(false);
        userRepository.save(user);
    }

    public void deleteUser(Long id) {
        User user = getUserById(id);
        userRepository.delete(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}