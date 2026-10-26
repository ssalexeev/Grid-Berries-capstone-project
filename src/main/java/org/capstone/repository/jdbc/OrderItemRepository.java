package org.capstone.repository.jdbc;

import lombok.RequiredArgsConstructor;
import org.capstone.domain.OrderItem;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

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
    public OrderItem save(OrderItem item) {
        if (item == null) {
            throw new IllegalArgumentException("Order item cannot be null");
        }
        if (item.getOrderId() == null) {
            throw new IllegalArgumentException("Order item must have an order ID");
        }
        Long productId = item.getProduct() == null ? null : item.getProduct().getId();
        if (item.getId() == null) {
            long id = jdbcUtil.insertAndReturnId("""
                    INSERT INTO order_item (order_id, product_id, quantity, unit_price)
                    VALUES (?, ?, ?, ?)
                    RETURNING id
                    """, item.getOrderId(), productId, item.getQuantity(), item.getUnitPrice());
            item.setId(id);
        } else {
            jdbcUtil.execute("""
                    UPDATE order_item
                    SET order_id = ?, product_id = ?, quantity = ?, unit_price = ?
                    WHERE id = ?
                    """, item.getOrderId(), productId, item.getQuantity(), item.getUnitPrice(), item.getId());
        }
        return findById(item.getId()).orElse(null);
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
                return OrderItem.builder()
                        .id(resultSet.getLong("id"))
                        .orderId(nullableLong(resultSet, "order_id"))
                        .quantity((Integer) resultSet.getObject("quantity"))
                        .unitPrice(resultSet.getBigDecimal("unit_price"))
                        .product(productRepository.findById(nullableLong(resultSet, "product_id")).orElse(null))
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
