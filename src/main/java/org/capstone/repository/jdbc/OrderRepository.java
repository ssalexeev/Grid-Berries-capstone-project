package org.capstone.repository.jdbc;

import lombok.RequiredArgsConstructor;
import org.capstone.domain.Order;
import org.capstone.domain.OrderItem;
import org.capstone.domain.enums.OrderStatus;
import org.capstone.repository.jdbc.util.JdbcRepository;
import org.capstone.repository.jdbc.util.JdbcUtil;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;


@Repository
@RequiredArgsConstructor
public class OrderRepository implements JdbcRepository<Order, Long> {
    private final JdbcUtil jdbcUtil;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    public Optional<Order> findById(Long id) {
        return jdbcUtil.findOne("SELECT * FROM \"order\" WHERE id = ?", mapper(), id);
    }

    @Override
    public List<Order> findAll() {
        return jdbcUtil.findMany("SELECT * FROM \"order\"", mapper());
    }

    public List<Order> findAllByUserId(Long userId) {
        return jdbcUtil.findMany("SELECT * FROM \"order\" WHERE user_id = ?", mapper(), userId);
    }

    @Override
    public Order save(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }
        Long userId = order.getUser() == null ? null : order.getUser().getId();
        String status = order.getStatus() == null ? null : order.getStatus().name();
        if (order.getId() == null) {
            long id = jdbcUtil.insertAndReturnId("""
                    INSERT INTO "order" (user_id, status, total_amount, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?)
                    RETURNING id
                    """, userId, status, order.getTotalAmount(), order.getCreatedAt(), order.getUpdatedAt());
            order.setId(id);
        } else {
            jdbcUtil.execute("""
                    UPDATE "order"
                    SET user_id = ?, status = ?, total_amount = ?, created_at = ?, updated_at = ?
                    WHERE id = ?
                    """, userId, status, order.getTotalAmount(), order.getCreatedAt(), order.getUpdatedAt(),
                    order.getId());
        }

        List<OrderItem> existingItems = orderItemRepository.findAllByOrderId(order.getId());
        Set<Long> retainedIds = new HashSet<>();

        for (OrderItem item : order.getItems()) {
            if (item.getId() != null) {
                retainedIds.add(item.getId());
            }
        }
        for (OrderItem existingItem : existingItems) {
            if (!retainedIds.contains(existingItem.getId())) {
                orderItemRepository.delete(existingItem);
            }
        }
        for (OrderItem item : order.getItems()) {
            item.setOrderId(order.getId());
            orderItemRepository.save(item);
        }
        return findById(order.getId()).orElse(null);
    }

    @Override
    public void delete(Order order) {
        if (order == null || order.getId() == null) {
            throw new IllegalArgumentException("Order and order ID cannot be null");
        }
        jdbcUtil.execute("DELETE FROM \"order\" WHERE id = ?", order.getId());
    }

    @Override
    public Function<ResultSet, Order> mapper() {
        return resultSet -> {
            try {
                Timestamp createdAt = resultSet.getTimestamp("created_at");
                Timestamp updatedAt = resultSet.getTimestamp("updated_at");
                String status = resultSet.getString("status");
                long userId = resultSet.getLong("user_id");
                Long nullableUserId = resultSet.wasNull() ? null : userId;
                long orderId = resultSet.getLong("id");
                return Order.builder()
                        .id(orderId)
                        .status(status == null ? null : OrderStatus.valueOf(status.toUpperCase()))
                        .totalAmount(resultSet.getBigDecimal("total_amount"))
                        .createdAt(createdAt == null ? null : createdAt.toLocalDateTime())
                        .updatedAt(updatedAt == null ? null : updatedAt.toLocalDateTime())
                        .user(nullableUserId == null ? null : userRepository.findById(nullableUserId).orElse(null))
                        .items(orderItemRepository.findAllByOrderId(orderId))
                        .build();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to map ResultSet to Order", e);
            }
        };
    }
}
