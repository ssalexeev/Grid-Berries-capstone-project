package org.capstone.repository.jdbc;

import lombok.RequiredArgsConstructor;
import org.capstone.domain.SellerProfile;
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

@Repository("jdbcSellerProfileRepository")
@RequiredArgsConstructor
public class SellerProfileRepository implements JdbcRepository<SellerProfile, Long> {
    private final JdbcUtil jdbcUtil;

    private final UserRepository userRepository;


    @Override
    public Optional<SellerProfile> findById(Long id) {
        String query = "SELECT * FROM seller_profile WHERE id = ?";

        return jdbcUtil.findOne(query, mapper(), id);
    }

    public Optional<SellerProfile> findByUserId(Long userId) {
        String query = "SELECT * FROM seller_profile WHERE user_id = ?";
        return jdbcUtil.findOne(query, mapper(), userId);
    }

    @Override
    public List<SellerProfile> findAll() {
        String query = "SELECT * FROM seller_profile";

        return jdbcUtil.findMany(query, mapper());
    }

    @Override
    @Transactional
    public SellerProfile save(SellerProfile sellerProfile) {
        if (sellerProfile == null) {
            throw new IllegalArgumentException("Seller profile cannot be null");
        }
        if (sellerProfile.getUser() == null || sellerProfile.getUser().getId() == null) {
            throw new IllegalArgumentException("Seller profile must have a persisted user");
        }

        if (sellerProfile.getId() == null) {
            String saveQuery = """
                    INSERT INTO seller_profile (user_id, store_name, store_description, contact_info, created_at)
                    VALUES (?, ?, ?, ?, ?)
                    """;
            long id = jdbcUtil.insertAndReturnId(saveQuery, sellerProfile.getUser().getId(),
                    sellerProfile.getStoreName(), sellerProfile.getStoreDescription(),
                    sellerProfile.getContactInfo(), sellerProfile.getCreatedAt());
            sellerProfile.setId(id);
        } else {
            String updateQuery = """
                    UPDATE seller_profile
                    SET user_id = ?, store_name = ?, store_description = ?, contact_info = ?
                    WHERE id = ?
                    """;
            int updated = jdbcUtil.executeAndReturnUpdateCount(updateQuery,
                    sellerProfile.getUser().getId(),
                    sellerProfile.getStoreName(),
                    sellerProfile.getStoreDescription(),
                    sellerProfile.getContactInfo(),
                    sellerProfile.getId());
            if (updated != 1) {
                throw new IllegalStateException(
                        "Seller profile not found while updating: " + sellerProfile.getId());
            }
        }

        return findById(sellerProfile.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Saved seller profile was not found: " + sellerProfile.getId()));
    }

    @Override
    public void delete(SellerProfile sellerProfile) {
        if (sellerProfile == null || sellerProfile.getId() == null) {
            throw new IllegalArgumentException("SellerProfile or user ID cannot be null");
        }
        String query = "DELETE FROM seller_profile WHERE id = ?";
        jdbcUtil.execute(query, sellerProfile.getId());
    }

    @Override
    public Function<ResultSet, SellerProfile> mapper() {
        return rs -> {
            try {
                Timestamp createdAtTimestamp = rs.getTimestamp("created_at");
                return SellerProfile.builder()
                        .id(rs.getLong("id"))
                        .storeName(rs.getString("store_name"))
                        .storeDescription(rs.getString("store_description"))
                        .contactInfo(rs.getString("contact_info"))
                        .user(userRepository.findById(rs.getLong("user_id"))
                                .orElse(null)
                        )
                        .createdAt(createdAtTimestamp != null ? createdAtTimestamp.toLocalDateTime() : null)
                        .build();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to map ResultSet to SellerProfile", e);
            }
        };
    }
}
