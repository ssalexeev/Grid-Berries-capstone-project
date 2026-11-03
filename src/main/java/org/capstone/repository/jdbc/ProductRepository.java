package org.capstone.repository.jdbc;

import lombok.RequiredArgsConstructor;
import org.capstone.domain.Category;
import org.capstone.domain.Product;
import org.capstone.domain.enums.ProductStatus;
import org.capstone.domain.enums.ProductType;
import org.capstone.repository.jdbc.util.JdbcRepository;
import org.capstone.repository.jdbc.util.JdbcUtil;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;


@Repository
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
        return jdbcUtil.findMany("SELECT * FROM product WHERE seller_id = ?", mapper(), sellerId);
    }

    public boolean reserveStock(Long productId, int quantity) {
        return jdbcUtil.executeAndReturnUpdateCount("""
                UPDATE product
                SET stock_quantity = stock_quantity - ?
                WHERE id = ? AND stock_quantity >= ?
                """, quantity, productId, quantity) == 1;
    }

    public void restoreStock(Long productId, int quantity) {
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
    public Product save(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null");
        }
        Long sellerId = product.getSellerProfile() == null ? null : product.getSellerProfile().getId();
        String type = product.getType() == null ? null : product.getType().name();
        String status = product.getStatus() == null ? null : product.getStatus().name();

        if (product.getId() == null) {
            long id = jdbcUtil.insertAndReturnId("""
                    INSERT INTO product
                        (seller_id, title, description, price, stock_quantity, image_url, type, status)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    RETURNING id
                    """, sellerId, product.getTitle(), product.getDescription(), product.getPrice(),
                    product.getStockQuantity(), product.getImageUrl(), type, status);
            product.setId(id);
        } else {
            jdbcUtil.execute("""
                    UPDATE product
                    SET seller_id = ?, title = ?, description = ?, price = ?, stock_quantity = ?,
                        image_url = ?, type = ?, status = ?
                    WHERE id = ?
                    """, sellerId, product.getTitle(), product.getDescription(), product.getPrice(),
                    product.getStockQuantity(), product.getImageUrl(), type, status, product.getId());
        }

        jdbcUtil.execute("DELETE FROM product_category WHERE product_id = ?", product.getId());
        Map<Long, Category> categoriesById = new LinkedHashMap<>();
        if (product.getCategories() != null) {
            for (Category category : product.getCategories()) {
                if (category != null && category.getId() != null) {
                    categoriesById.put(category.getId(), category);
                }
            }
        }
        if (categoriesById.isEmpty() && product.getCategory() != null
                && product.getCategory().getId() != null) {
            categoriesById.put(product.getCategory().getId(), product.getCategory());
        }
        for (Long categoryId : categoriesById.keySet()) {
            jdbcUtil.execute("""
                    INSERT INTO product_category (product_id, category_id)
                    VALUES (?, ?)
                    """, product.getId(), categoryId);
        }
        return findById(product.getId()).orElse(null);
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
                long sellerId = resultSet.getLong("seller_id");
                Long nullableSellerId = resultSet.wasNull() ? null : sellerId;
                String type = resultSet.getString("type");
                String status = resultSet.getString("status");
                long productId = resultSet.getLong("id");
                List<Category> categories = categoryRepository.findByProductId(productId);
                return Product.builder()
                        .id(productId)
                        .title(resultSet.getString("title"))
                        .description(resultSet.getString("description"))
                        .price(resultSet.getBigDecimal("price"))
                        .stockQuantity((Integer) resultSet.getObject("stock_quantity"))
                        .imageUrl(resultSet.getString("image_url"))
                        .type(type == null ? null : ProductType.valueOf(type.toUpperCase()))
                        .status(status == null ? null : ProductStatus.valueOf(status.toUpperCase()))
                        .sellerProfile(nullableSellerId == null
                                ? null : sellerProfileRepository.findById(nullableSellerId).orElse(null))
                        .category(categories.isEmpty() ? null : categories.get(0))
                        .categories(new ArrayList<>(categories))
                        .build();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to map ResultSet to Product", e);
            }
        };
    }

}
