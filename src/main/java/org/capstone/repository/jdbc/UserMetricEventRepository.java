package org.capstone.repository.jdbc;

import lombok.RequiredArgsConstructor;
import org.capstone.domain.UserMetricEvent;
import org.capstone.domain.enums.MetricEventType;
import org.capstone.repository.jdbc.util.JdbcRepository;
import org.capstone.repository.jdbc.util.JdbcUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@RequiredArgsConstructor
public class UserMetricEventRepository implements JdbcRepository<UserMetricEvent, Long> {
    private final JdbcUtil jdbcUtil;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    public Optional<UserMetricEvent> findById(Long id) {
        return jdbcUtil.findOne("SELECT * FROM user_metric_event WHERE id = ?", mapper(), id);
    }

    @Override
    public List<UserMetricEvent> findAll() {
        return jdbcUtil.findMany("SELECT * FROM user_metric_event", mapper());
    }

    public List<UserMetricEvent> findAllByUserId(Long userId) {
        return jdbcUtil.findMany("SELECT * FROM user_metric_event WHERE user_id = ?", mapper(), userId);
    }

    @Override
    public UserMetricEvent save(UserMetricEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("User metric event cannot be null");
        }
        Long userId = event.getUser() == null ? null : event.getUser().getId();
        Long productId = event.getProduct() == null ? null : event.getProduct().getId();
        Long orderItemId = event.getOrderItem() == null ? null : event.getOrderItem().getId();
        String eventType = event.getEventType() == null ? null : event.getEventType().name();
        if (event.getId() == null) {
            long id = jdbcUtil.insertAndReturnId("""
                    INSERT INTO user_metric_event
                        (user_id, product_id, order_item_id, event_type, search_query, created_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                    RETURNING id
                    """, userId, productId, orderItemId, eventType, event.getSearchQuery(), event.getTimestamp());
            event.setId(id);
        } else {
            jdbcUtil.execute("""
                    UPDATE user_metric_event
                    SET user_id = ?, product_id = ?, order_item_id = ?, event_type = ?, search_query = ?, created_at = ?
                    WHERE id = ?
                    """, userId, productId, orderItemId, eventType, event.getSearchQuery(), event.getTimestamp(),
                    event.getId());
        }
        return findById(event.getId()).orElse(null);
    }

    @Override
    public void delete(UserMetricEvent event) {
        if (event == null || event.getId() == null) {
            throw new IllegalArgumentException("User metric event and ID cannot be null");
        }
        jdbcUtil.execute("DELETE FROM user_metric_event WHERE id = ?", event.getId());
    }

    @Override
    public Function<ResultSet, UserMetricEvent> mapper() {
        return resultSet -> {
            try {
                Timestamp timestamp = resultSet.getTimestamp("created_at");
                Long userId = nullableLong(resultSet, "user_id");
                Long productId = nullableLong(resultSet, "product_id");
                Long orderItemId = nullableLong(resultSet, "order_item_id");
                String eventType = resultSet.getString("event_type");
                return UserMetricEvent.builder()
                        .id(resultSet.getLong("id"))
                        .eventType(eventType == null ? null : MetricEventType.valueOf(eventType.toUpperCase()))
                        .searchQuery(resultSet.getString("search_query"))
                        .timestamp(timestamp == null ? null : timestamp.toLocalDateTime())
                        .user(userId == null ? null : userRepository.findById(userId).orElse(null))
                        .product(productId == null ? null : productRepository.findById(productId).orElse(null))
                        .orderItem(orderItemId == null ? null : orderItemRepository.findById(orderItemId).orElse(null))
                        .build();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to map ResultSet to UserMetricEvent", e);
            }
        };
    }

    private Long nullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }
}
