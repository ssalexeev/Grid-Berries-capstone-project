package org.capstone.repository.jdbc;

import lombok.RequiredArgsConstructor;
import org.capstone.domain.Category;
import org.capstone.domain.Product;
import org.capstone.domain.enums.ProductStatus;
import org.capstone.domain.enums.ProductType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

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
        Category category = product.getCategory();
        if (category != null && category.getId() != null) {
            jdbcUtil.execute("""
                    INSERT INTO product_category (product_id, category_id)
                    VALUES (?, ?)
                    """, product.getId(), category.getId());
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
                return Product.builder()
                        .id(resultSet.getLong("id"))
                        .title(resultSet.getString("title"))
                        .description(resultSet.getString("description"))
                        .price(resultSet.getBigDecimal("price"))
                        .stockQuantity((Integer) resultSet.getObject("stock_quantity"))
                        .imageUrl(resultSet.getString("image_url"))
                        .type(type == null ? null : ProductType.valueOf(type.toUpperCase()))
                        .status(status == null ? null : ProductStatus.valueOf(status.toUpperCase()))
                        .sellerProfile(nullableSellerId == null
                                ? null : sellerProfileRepository.findById(nullableSellerId).orElse(null))
                        .category(findCategoryByProductId(resultSet.getLong("id")))
                        .build();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to map ResultSet to Product", e);
            }
        };
    }

    private Category findCategoryByProductId(long productId) {
        return jdbcUtil.findOne("""
                SELECT category.*
                FROM category
                JOIN product_category ON product_category.category_id = category.id
                WHERE product_category.product_id = ?
                ORDER BY category.id
                LIMIT 1
                """, categoryRepository.mapper(), productId).orElse(null);
    }
}
