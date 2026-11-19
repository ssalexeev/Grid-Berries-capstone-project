package org.capstone.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.capstone.domain.User;
import org.capstone.domain.enums.Role;
import org.capstone.repository.jpa.UserRepository;
import org.capstone.util.exception.UserAlreadyExistsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    //todo: add email index and optimize query
    @Transactional
    public User registerUser(User user) {
        boolean emailExists = userRepository.existsByEmailIgnoreCase(user.getEmail());
        if (emailExists) {
            throw new UserAlreadyExistsException("User with email " + user.getEmail() + " already exists.");
        }


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

    public User getUserByEmail(String email) {
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

    @Transactional
    public User patchUser(Long userId, User user) {
        User existing = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        if (user.getEmail() != null && !user.getEmail().equalsIgnoreCase(existing.getEmail())) {
            if (userRepository.existsByEmail(user.getEmail())) {
                throw new IllegalArgumentException("Email is already in use: " + user.getEmail());
            }
            existing.setEmail(user.getEmail().trim());
        }

        if (user.getIsActive() != null) {
            existing.setIsActive(user.getIsActive());
        }

        if (user.getPasswordHash() != null){
            existing.setPasswordHash(user.getPasswordHash());
        }

        if (user.getRole() != null) {
            existing.setRole(user.getRole());
        }

        return userRepository.save(existing);
    }

    public void deleteUser(Long id) {
        User user = getUserById(id);
        userRepository.delete(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User by email:" + email + "not found"));
    }
}