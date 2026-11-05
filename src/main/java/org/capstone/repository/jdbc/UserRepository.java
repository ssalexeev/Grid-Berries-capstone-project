package org.capstone.repository.jdbc;

import lombok.RequiredArgsConstructor;
import org.capstone.domain.User;
import org.capstone.domain.enums.Role;
import org.capstone.repository.jdbc.util.JdbcRepository;
import org.capstone.repository.jdbc.util.JdbcUtil;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Repository("jdbcUserRepository")
@RequiredArgsConstructor
public class UserRepository implements JdbcRepository<User, Long> {
    private final JdbcUtil jdbcUtil;


    @Override
    public Optional<User> findById(Long id) {
        String query = "SELECT * FROM \"user\" WHERE id = ?";
        return jdbcUtil.findOne(query, mapper(), id);
    }


    public Optional<User> findByEmail(String email) {
        String query = "SELECT * FROM \"user\" WHERE email = ?";
        return jdbcUtil.findOne(query, mapper(), email);
    }


    @Override
    public List<User> findAll() {
        String query = "SELECT * FROM \"user\"";
        return jdbcUtil.findMany(query, mapper());
    }

    public List<User> findAllByRole(Role role) {
        if (role == null) {
            return List.of();
        }
        String query = "SELECT * FROM \"user\" WHERE role = ?";
        return jdbcUtil.findMany(query, mapper(), role.toString());
    }


    @Override
    @Transactional
    public User save(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        if (user.getId() == null) {
            String saveQuery = """
                    INSERT INTO "user" (email, password_hash, role, is_active, created_at)
                    VALUES (?, ?, ?, ?, ?)
                    """;
            Long id = jdbcUtil.insertAndReturnId(saveQuery, user.getEmail(), user.getPasswordHash(),
                    user.getRole() != null ? user.getRole().name() : null,
                    user.getIsActive(), user.getCreatedAt());
            user.setId(id);
        } else {
            String updateQuery = """
                    UPDATE "user"
                    SET email = ?, password_hash = ?, role = ?, is_active = ?
                    WHERE id = ?
                    """;
            int updated = jdbcUtil.executeAndReturnUpdateCount(updateQuery,
                    user.getEmail(),
                    user.getPasswordHash(),
                    user.getRole() != null ? user.getRole().name() : null,
                    user.getIsActive(),
                    user.getId());
            if (updated != 1) {
                throw new IllegalStateException("User not found while updating: " + user.getId());
            }
        }

        return findById(user.getId())
                .orElseThrow(() -> new IllegalStateException("Saved user was not found: " + user.getId()));
    }

    @Override
    public void delete(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User and user ID cannot be null");
        }
        String query = "DELETE FROM \"user\" WHERE id = ?";
        jdbcUtil.execute(query, user.getId());
    }

    @Override
    public Function<ResultSet, User> mapper() {
        return rs -> {
            try {
                Timestamp createdAtTimestamp = rs.getTimestamp("created_at");
                return User.builder()
                        .id(rs.getLong("id"))
                        .email(rs.getString("email"))
                        .passwordHash(rs.getString("password_hash"))
                        .role(rs.getString("role") != null
                                ? Role.valueOf(rs.getString("role").toUpperCase())
                                : null).isActive(rs.getBoolean("is_active"))
                        .createdAt(createdAtTimestamp != null ? createdAtTimestamp.toLocalDateTime() : null).build();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to map ResultSet to User", e);
            }
        };
    }
}
