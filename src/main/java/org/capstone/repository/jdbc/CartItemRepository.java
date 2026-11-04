package org.capstone.repository.jdbc;

import lombok.RequiredArgsConstructor;
import org.capstone.domain.Cart;
import org.capstone.domain.CartItem;
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

@Repository("jdbcCartItemRepository")
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
    @Transactional
    public CartItem save(CartItem item) {
        if (item == null) {
            throw new IllegalArgumentException("Cart item cannot be null");
        }
        if (item.getCart() == null || item.getCart().getId() == null) {
            throw new IllegalArgumentException("Cart item must have a cart ID");
        }
        if (item.getProduct() == null || item.getProduct().getId() == null) {
            throw new IllegalArgumentException("Cart item must have a persisted product");
        }
        Long productId = item.getProduct() == null ? null : item.getProduct().getId();
        Long cartId = item.getCart().getId();
        if (item.getId() == null) {
            long id = jdbcUtil.insertAndReturnId("""
                    INSERT INTO cart_item (cart_id, product_id, quantity, created_at)
                    VALUES (?, ?, ?, ?)
                    """, cartId, productId, item.getQuantity(), item.getCreatedAt());
            item.setId(id);
        } else {
            int updated = jdbcUtil.executeAndReturnUpdateCount("""
                    UPDATE cart_item
                    SET cart_id = ?, product_id = ?, quantity = ?
                    WHERE id = ?
                    """, cartId, productId, item.getQuantity(), item.getId());
            if (updated != 1) {
                throw new IllegalStateException("Cart item not found while updating: " + item.getId());
            }
        }
        return findById(item.getId())
                .orElseThrow(() -> new IllegalStateException("Saved cart item was not found: " + item.getId()));
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
                        .cart(Cart.builder().id(nullableLong(resultSet, "cart_id")).build())
                        .quantity((Integer) resultSet.getObject("quantity"))
                        .createdAt(addedAt == null ? null : addedAt.toLocalDateTime())
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
