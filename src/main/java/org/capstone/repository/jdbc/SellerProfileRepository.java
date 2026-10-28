package org.capstone.repository.jdbc;

import lombok.RequiredArgsConstructor;
import org.capstone.domain.SellerProfile;
import org.capstone.repository.jdbc.util.JdbcRepository;
import org.capstone.repository.jdbc.util.JdbcUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

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
    public SellerProfile save(SellerProfile sellerProfile) {
        if (sellerProfile == null) {
            throw new IllegalArgumentException("Seller profile cannot be null");
        }

        if (sellerProfile.getId() == null) {
            if (sellerProfile.getUser() == null || sellerProfile.getUser().getId() == null) {
                throw new IllegalArgumentException("Seller profile must have a persisted user");
            }

            String saveQuery = """
                    INSERT INTO "seller_profile" (user_id, store_name, store_description, contact_info, created_at)
                    VALUES (?, ?, ?, ?, ?)
                    RETURNING id
                    """;
            Long id = jdbcUtil.findOne(saveQuery, rs -> {
                                try {
                                    return rs.getLong("id");
                                } catch (SQLException e) {
                                    throw new RuntimeException("Failed to read generated seller profile ID", e);
                                }
                            }, sellerProfile.getUser().getId(), sellerProfile.getStoreName(),
                            sellerProfile.getStoreDescription(), sellerProfile.getContactInfo(),
                            sellerProfile.getCreatedAt())
                    .orElseThrow(() -> new IllegalStateException("SellerProfile insert did not return an ID"));
            sellerProfile.setId(id);
        } else {
            String updateQuery = """
                    UPDATE seller_profile
                    SET store_name = ?, store_description = ?, contact_info = ?, created_at = ?
                    WHERE id = ?
                    """;
            jdbcUtil.execute(updateQuery,
                    sellerProfile.getStoreName(),
                    sellerProfile.getStoreDescription(),
                    sellerProfile.getContactInfo(),
                    sellerProfile.getCreatedAt(),
                    sellerProfile.getId());
        }

        return findById(sellerProfile.getId()).orElse(null);
    }

    @Override
    public void delete(SellerProfile sellerProfile) {
        if (sellerProfile == null || sellerProfile.getId() == null) {
            throw new IllegalArgumentException("SellerProfile or user ID cannot be null");
        }
        String query = "DELETE FROM \"seller_profile\" WHERE id = ?";
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
