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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

@Repository("jdbcCartRepository")
@RequiredArgsConstructor
public class CartRepository implements JdbcRepository<Cart, Long> {
    private final JdbcUtil jdbcUtil;
    private final UserRepository userRepository;
    private final CartItemRepository cartItemRepository;

    @Override
    public Optional<Cart> findById(Long id) {
        return jdbcUtil.findOne("SELECT * FROM cart WHERE id = ?", mapper(), id);
    }

    @Override
    public List<Cart> findAll() {
        return jdbcUtil.findMany("SELECT * FROM cart", mapper());
    }

    public Optional<Cart> findByUserId(Long userId) {
        return jdbcUtil.findOne("SELECT * FROM cart WHERE user_id = ?", mapper(), userId);
    }

    @Override
    @Transactional
    public Cart save(Cart cart) {
        if (cart == null) {
            throw new IllegalArgumentException("Cart cannot be null");
        }

        if (cart.getUser() == null || cart.getUser().getId() == null) {
            throw new IllegalArgumentException("Cart must have a persisted user");
        }
        Long userId = cart.getUser().getId();
        if (cart.getId() == null) {
            long id = jdbcUtil.insertAndReturnId("""
                    INSERT INTO cart (user_id, total_amount, updated_at)
                    VALUES (?, ?, ?)
                    """, userId, cart.getTotalAmount(), cart.getUpdatedAt());
            cart.setId(id);
        } else {
            int updated = jdbcUtil.executeAndReturnUpdateCount("""
                    UPDATE cart
                    SET user_id = ?, total_amount = ?, updated_at = ?
                    WHERE id = ?
                    """, userId, cart.getTotalAmount(), cart.getUpdatedAt(), cart.getId());
            if (updated != 1) {
                throw new IllegalStateException("Cart not found while updating: " + cart.getId());
            }
        }

        List<CartItem> existingItems = cartItemRepository.findAllByCartId(cart.getId());
        Set<Long> retainedIds = new HashSet<>();
        for (CartItem item : cart.getCartItems()) {
            if (item.getId() != null) {
                retainedIds.add(item.getId());
            }
        }
        for (CartItem existingItem : existingItems) {
            if (!retainedIds.contains(existingItem.getId())) {
                cartItemRepository.delete(existingItem);
            }
        }
        for (CartItem item : cart.getCartItems()) {
            item.setCart(cart);
            cartItemRepository.save(item);
        }
        return findById(cart.getId())
                .orElseThrow(() -> new IllegalStateException("Saved cart was not found: " + cart.getId()));
    }

    @Override
    public void delete(Cart cart) {
        if (cart == null || cart.getId() == null) {
            throw new IllegalArgumentException("Cart and cart ID cannot be null");
        }
        jdbcUtil.execute("DELETE FROM cart WHERE id = ?", cart.getId());
    }

    @Override
    public Function<ResultSet, Cart> mapper() {
        return resultSet -> {
            try {
                Timestamp updatedAt = resultSet.getTimestamp("updated_at");
                long userId = resultSet.getLong("user_id");
                Long nullableUserId = resultSet.wasNull() ? null : userId;
                long cartId = resultSet.getLong("id");
                return Cart.builder()
                        .id(cartId)
                        .totalAmount(resultSet.getBigDecimal("total_amount"))
                        .updatedAt(updatedAt == null ? null : updatedAt.toLocalDateTime())
                        .user(nullableUserId == null ? null : userRepository.findById(nullableUserId).orElse(null))
                        .cartItems(cartItemRepository.findAllByCartId(cartId))
                        .build();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to map ResultSet to Cart", e);
            }
        };
    }
}
