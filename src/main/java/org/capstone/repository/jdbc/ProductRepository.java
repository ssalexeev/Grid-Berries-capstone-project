package org.capstone.repository.jdbc;

import lombok.RequiredArgsConstructor;
import org.capstone.domain.Category;
import org.capstone.domain.Product;
import org.capstone.domain.enums.ProductStatus;
import org.capstone.repository.jdbc.util.JdbcRepository;
import org.capstone.repository.jdbc.util.JdbcUtil;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;


@Repository("jdbcProductRepository")
@RequiredArgsConstructor
public class ProductRepository implements JdbcRepository<Product, Long> {
    private final JdbcUtil jdbcUtil;
    private final SellerProfileRepository sellerProfileRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public Optional<Product> findById(Long id) {
        return jdbcUtil.findOne("SELECT * FROM product WHERE id = ?", mapper(), id);
    }

    @Override
    public List<Product> findAll() {
        return jdbcUtil.findMany("SELECT * FROM product", mapper());
    }

    public List<Product> findBySellerId(Long sellerId) {
        return jdbcUtil.findMany("SELECT * FROM product WHERE seller_profile_id = ?", mapper(), sellerId);
    }

    public boolean reserveStock(Long productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Reserved quantity must be positive");
        }
        return jdbcUtil.executeAndReturnUpdateCount("""
                UPDATE product
                SET stock_quantity = stock_quantity - ?
                WHERE id = ? AND stock_quantity >= ?
                """, quantity, productId, quantity) == 1;
    }

    public void restoreStock(Long productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Restored quantity must be positive");
        }
        int updated = jdbcUtil.executeAndReturnUpdateCount("""
                UPDATE product
                SET stock_quantity = stock_quantity + ?
                WHERE id = ?
                """, quantity, productId);
        if (updated != 1) {
            throw new IllegalStateException("Product not found while restoring reserved stock: " + productId);
        }
    }

    @Override
    @Transactional
    public Product save(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null");
        }
        if (product.getSellerProfile() == null || product.getSellerProfile().getId() == null) {
            throw new IllegalArgumentException("Product must have a persisted seller profile");
        }
        Long sellerId = product.getSellerProfile().getId();
        String status = product.getStatus() == null ? null : product.getStatus().name();

        if (product.getId() == null) {
            long id = jdbcUtil.insertAndReturnId("""
                    INSERT INTO product
                        (seller_profile_id, title, description, price, stock_quantity, image_url, status, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, sellerId, product.getTitle(), product.getDescription(), product.getPrice(),
                    product.getStockQuantity(), product.getImageUrl(), status, product.getCreatedAt());
            product.setId(id);
        } else {
            int updated = jdbcUtil.executeAndReturnUpdateCount("""
                    UPDATE product
                    SET seller_profile_id = ?, title = ?, description = ?, price = ?, stock_quantity = ?,
                        image_url = ?, status = ?
                    WHERE id = ?
                    """, sellerId, product.getTitle(), product.getDescription(), product.getPrice(),
                    product.getStockQuantity(), product.getImageUrl(), status, product.getId());
            if (updated != 1) {
                throw new IllegalStateException("Product not found while updating: " + product.getId());
            }
        }

        jdbcUtil.execute("DELETE FROM product_category WHERE product_id = ?", product.getId());
        Set<Long> categoryIds = new HashSet<>();
        if (product.getCategories() != null) {
            for (Category category : product.getCategories()) {
                if (category != null && category.getId() != null) {
                    categoryIds.add(category.getId());
                }
            }
        }
        for (Long categoryId : categoryIds) {
            jdbcUtil.execute("""
                    INSERT INTO product_category (product_id, category_id)
                    VALUES (?, ?)
                    """, product.getId(), categoryId);
        }
        return findById(product.getId())
                .orElseThrow(() -> new IllegalStateException("Saved product was not found: " + product.getId()));
    }

    @Override
    public void delete(Product product) {
        if (product == null || product.getId() == null) {
            throw new IllegalArgumentException("Product and product ID cannot be null");
        }
        jdbcUtil.execute("DELETE FROM product WHERE id = ?", product.getId());
    }

    @Override
    public Function<ResultSet, Product> mapper() {
        return resultSet -> {
            try {
                long sellerId = resultSet.getLong("seller_profile_id");
                String status = resultSet.getString("status");
                long productId = resultSet.getLong("id");
                Timestamp createdAt = resultSet.getTimestamp("created_at");
                List<Category> categories = categoryRepository.findByProductId(productId);
                return Product.builder()
                        .id(productId)
                        .title(resultSet.getString("title"))
                        .description(resultSet.getString("description"))
                        .price(resultSet.getBigDecimal("price"))
                        .stockQuantity((Integer) resultSet.getObject("stock_quantity"))
                        .imageUrl(resultSet.getString("image_url"))
                        .status(status == null ? null : ProductStatus.valueOf(status.toUpperCase()))
                        .sellerProfile(sellerProfileRepository.findById(sellerId)
                                .orElseThrow(() -> new IllegalStateException(
                                        "Product references missing seller profile: " + sellerId)))
                        .categories(new HashSet<>(categories))
                        .createdAt(createdAt == null ? null : createdAt.toLocalDateTime())
                        .build();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to map ResultSet to Product", e);
            }
        };
    }

}
