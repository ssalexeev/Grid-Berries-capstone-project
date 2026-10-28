package org.capstone.repository.jdbc;

import lombok.RequiredArgsConstructor;
import org.capstone.domain.CartItem;
import org.capstone.repository.jdbc.util.JdbcRepository;
import org.capstone.repository.jdbc.util.JdbcUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@RequiredArgsConstructor
public class CartItemRepository implements JdbcRepository<CartItem, Long> {
    private final JdbcUtil jdbcUtil;
    private final ProductRepository productRepository;

    @Override
    public Optional<CartItem> findById(Long id) {
        return jdbcUtil.findOne("SELECT * FROM cart_item WHERE id = ?", mapper(), id);
    }

    @Override
    public List<CartItem> findAll() {
        return jdbcUtil.findMany("SELECT * FROM cart_item", mapper());
    }

    public List<CartItem> findAllByCartId(Long cartId) {
        return jdbcUtil.findMany("SELECT * FROM cart_item WHERE cart_id = ?", mapper(), cartId);
    }

    @Override
    public CartItem save(CartItem item) {
        if (item == null) {
            throw new IllegalArgumentException("Cart item cannot be null");
        }
        if (item.getCartId() == null) {
            throw new IllegalArgumentException("Cart item must have a cart ID");
        }
        Long productId = item.getProduct() == null ? null : item.getProduct().getId();
        if (item.getId() == null) {
            long id = jdbcUtil.insertAndReturnId("""
                    INSERT INTO cart_item (cart_id, product_id, quantity, created_at)
                    VALUES (?, ?, ?, ?)
                    RETURNING id
                    """, item.getCartId(), productId, item.getQuantity(), item.getAddedAt());
            item.setId(id);
        } else {
            jdbcUtil.execute("""
                    UPDATE cart_item
                    SET cart_id = ?, product_id = ?, quantity = ?, created_at = ?
                    WHERE id = ?
                    """, item.getCartId(), productId, item.getQuantity(), item.getAddedAt(), item.getId());
        }
        return findById(item.getId()).orElse(null);
    }

    @Override
    public void delete(CartItem item) {
        if (item == null || item.getId() == null) {
            throw new IllegalArgumentException("Cart item and cart item ID cannot be null");
        }
        jdbcUtil.execute("DELETE FROM cart_item WHERE id = ?", item.getId());
    }

    public void deleteAllByCartId(Long cartId) {
        jdbcUtil.execute("DELETE FROM cart_item WHERE cart_id = ?", cartId);
    }

    @Override
    public Function<ResultSet, CartItem> mapper() {
        return resultSet -> {
            try {
                Timestamp addedAt = resultSet.getTimestamp("created_at");
                return CartItem.builder()
                        .id(resultSet.getLong("id"))
                        .cartId(nullableLong(resultSet, "cart_id"))
                        .quantity((Integer) resultSet.getObject("quantity"))
                        .addedAt(addedAt == null ? null : addedAt.toLocalDateTime())
                        .product(productRepository.findById(nullableLong(resultSet, "product_id")).orElse(null))
                        .build();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to map ResultSet to CartItem", e);
            }
        };
    }

    private Long nullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }
}
