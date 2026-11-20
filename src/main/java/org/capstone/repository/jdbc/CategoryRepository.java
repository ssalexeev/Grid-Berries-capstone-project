package org.capstone.repository.jdbc;

import lombok.RequiredArgsConstructor;
import org.capstone.domain.Category;
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

@Repository("jdbcCategoryRepository")
@RequiredArgsConstructor
public class CategoryRepository implements JdbcRepository<Category, Long> {
    private final JdbcUtil jdbcUtil;

    @Override
    public Optional<Category> findById(Long id) {
        return jdbcUtil.findOne("SELECT * FROM category WHERE id = ?", mapper(), id);
    }

    @Override
    public List<Category> findAll() {
        return jdbcUtil.findMany("SELECT * FROM category", mapper());
    }

    public List<Category> findByParentCategoryId(Long parentCategoryId) {
        if (parentCategoryId == null) {
            return jdbcUtil.findMany("SELECT * FROM category WHERE parent_category_id IS NULL", mapper());
        }
        return jdbcUtil.findMany(
                "SELECT * FROM category WHERE parent_category_id = ?",
                mapper(),
                parentCategoryId
        );
    }

    public List<Category> findByProductId(Long productId) {
        return jdbcUtil.findMany("""
                SELECT category.*
                FROM category
                JOIN product_category ON product_category.category_id = category.id
                WHERE product_category.product_id = ?
                ORDER BY category.id
                """, mapper(), productId);
    }

    @Override
    @Transactional
    public Category save(Category category) {
        if (category == null) {
            throw new IllegalArgumentException("Category cannot be null");
        }
        if (category.getId() == null) {
            long id = jdbcUtil.insertAndReturnId("""
                    INSERT INTO category (parent_category_id, name, description, created_at)
                    VALUES (?, ?, ?, ?)
                    """, category.getParentCategoryId(), category.getName(), category.getDescription(),
                    category.getCreatedAt());
            category.setId(id);
        } else {
            int updated = jdbcUtil.executeAndReturnUpdateCount("""
                    UPDATE category
                    SET parent_category_id = ?, name = ?, description = ?
                    WHERE id = ?
                    """, category.getParentCategoryId(), category.getName(),
                    category.getDescription(), category.getId());
            if (updated != 1) {
                throw new IllegalStateException("Category not found while updating: " + category.getId());
            }
        }
        return findById(category.getId())
                .orElseThrow(() -> new IllegalStateException("Saved category was not found: " + category.getId()));
    }

    @Override
    public void delete(Category category) {
        if (category == null || category.getId() == null) {
            throw new IllegalArgumentException("Category and category ID cannot be null");
        }
        jdbcUtil.execute("DELETE FROM category WHERE id = ?", category.getId());
    }

    @Override
    public Function<ResultSet, Category> mapper() {
        return resultSet -> {
            try {
                Timestamp createdAt = resultSet.getTimestamp("created_at");
                Category category = Category.builder()
                        .id(resultSet.getLong("id"))
                        .name(resultSet.getString("name"))
                        .description(resultSet.getString("description"))
                        .createdAt(createdAt == null ? null : createdAt.toLocalDateTime())
                        .build();
                category.setParentCategoryId(nullableLong(resultSet, "parent_category_id"));
                return category;
            } catch (SQLException e) {
                throw new RuntimeException("Failed to map ResultSet to Category", e);
            }
        };
    }

    private Long nullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }
}
