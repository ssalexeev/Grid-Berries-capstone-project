package org.capstone.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.capstone.domain.User;
import org.capstone.domain.enums.Role;
import org.capstone.dto.in.UserRegistrationRequest;
import org.capstone.dto.out.UserRegistrationResponse;
import org.capstone.repository.jpa.UserRepository;
import org.capstone.util.exception.UserAlreadyExistsException;
import org.capstone.util.mapper.user.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;


    //todo: add email index and optimize query
    @Transactional
    public UserRegistrationResponse registerUser(UserRegistrationRequest userRegistrationRequest) {
        boolean emailExists = userRepository.existsByEmailIgnoreCase(userRegistrationRequest.getEmail());
        if (emailExists) {
            throw new UserAlreadyExistsException("User with email " + userRegistrationRequest.getEmail() + " already exists.");
        }

        User savedUser = userRepository.save(userMapper.toEntity(userRegistrationRequest));

        return userMapper.toResponse(savedUser);
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

    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User by email:" + email + "not found"));
    }
}