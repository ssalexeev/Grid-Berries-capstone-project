package org.capstone.repository.jdbc;

import lombok.RequiredArgsConstructor;
import org.capstone.domain.Order;
import org.capstone.domain.OrderItem;
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

@Repository("jdbcOrderItemRepository")
@RequiredArgsConstructor
public class OrderItemRepository implements JdbcRepository<OrderItem, Long> {
    private final JdbcUtil jdbcUtil;
    private final ProductRepository productRepository;

    @Override
    public Optional<OrderItem> findById(Long id) {
        return jdbcUtil.findOne("SELECT * FROM order_item WHERE id = ?", mapper(), id);
    }

    @Override
    public List<OrderItem> findAll() {
        return jdbcUtil.findMany("SELECT * FROM order_item", mapper());
    }

    public List<OrderItem> findAllByOrderId(Long orderId) {
        return jdbcUtil.findMany("SELECT * FROM order_item WHERE order_id = ?", mapper(), orderId);
    }

    @Override
    @Transactional
    public OrderItem save(OrderItem item) {
        if (item == null) {
            throw new IllegalArgumentException("Order item cannot be null");
        }
        if (item.getOrder() == null || item.getOrder().getId() == null) {
            throw new IllegalArgumentException("Order item must have an order ID");
        }
        if (item.getProduct() == null || item.getProduct().getId() == null) {
            throw new IllegalArgumentException("Order item must have a persisted product");
        }
        Long orderId = item.getOrder().getId();
        Long productId = item.getProduct() == null ? null : item.getProduct().getId();
        if (item.getId() == null) {
            long id = jdbcUtil.insertAndReturnId("""
                    INSERT INTO order_item (order_id, product_id, quantity, unit_price, created_at)
                    VALUES (?, ?, ?, ?, ?)
                    """, orderId, productId, item.getQuantity(), item.getUnitPrice(), item.getCreatedAt());
            item.setId(id);
        } else {
            int updated = jdbcUtil.executeAndReturnUpdateCount("""
                    UPDATE order_item
                    SET order_id = ?, product_id = ?, quantity = ?, unit_price = ?
                    WHERE id = ?
                    """, orderId, productId, item.getQuantity(), item.getUnitPrice(), item.getId());
            if (updated != 1) {
                throw new IllegalStateException("Order item not found while updating: " + item.getId());
            }
        }
        return findById(item.getId())
                .orElseThrow(() -> new IllegalStateException("Saved order item was not found: " + item.getId()));
    }

    @Override
    public void delete(OrderItem item) {
        if (item == null || item.getId() == null) {
            throw new IllegalArgumentException("Order item and order item ID cannot be null");
        }
        jdbcUtil.execute("DELETE FROM order_item WHERE id = ?", item.getId());
    }

    public void deleteAllByOrderId(Long orderId) {
        jdbcUtil.execute("DELETE FROM order_item WHERE order_id = ?", orderId);
    }

    @Override
    public Function<ResultSet, OrderItem> mapper() {
        return resultSet -> {
            try {
                Timestamp createdAt = resultSet.getTimestamp("created_at");
                Long orderId = nullableLong(resultSet, "order_id");
                return OrderItem.builder()
                        .id(resultSet.getLong("id"))
                        .order(orderId == null ? null : Order.builder().id(orderId).build())
                        .quantity((Integer) resultSet.getObject("quantity"))
                        .unitPrice(resultSet.getBigDecimal("unit_price"))
                        .product(productRepository.findById(nullableLong(resultSet, "product_id")).orElse(null))
                        .createdAt(createdAt == null ? null : createdAt.toLocalDateTime())
                        .build();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to map ResultSet to OrderItem", e);
            }
        };
    }

    private Long nullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }
}
