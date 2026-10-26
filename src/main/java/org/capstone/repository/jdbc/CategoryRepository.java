package org.capstone.repository.jdbc;

import lombok.RequiredArgsConstructor;
import org.capstone.domain.Category;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

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

    @Override
    public Category save(Category category) {
        if (category == null) {
            throw new IllegalArgumentException("Category cannot be null");
        }
        if (category.getId() == null) {
            long id = jdbcUtil.insertAndReturnId("""
                    INSERT INTO category (parent_category_id, name, description)
                    VALUES (?, ?, ?)
                    RETURNING id
                    """, category.getParentCategoryId(), category.getName(), category.getDescription());
            category.setId(id);
        } else {
            jdbcUtil.execute("""
                    UPDATE category
                    SET parent_category_id = ?, name = ?, description = ?
                    WHERE id = ?
                    """, category.getParentCategoryId(), category.getName(),
                    category.getDescription(), category.getId());
        }
        return findById(category.getId()).orElse(null);
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
                return Category.builder()
                        .id(resultSet.getLong("id"))
                        .parentCategoryId(nullableLong(resultSet, "parent_category_id"))
                        .name(resultSet.getString("name"))
                        .description(resultSet.getString("description"))
                        .build();
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
