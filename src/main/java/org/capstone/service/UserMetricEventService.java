package org.capstone.service;

import lombok.RequiredArgsConstructor;
import org.capstone.domain.OrderItem;
import org.capstone.domain.Product;
import org.capstone.domain.User;
import org.capstone.domain.UserMetricEvent;
import org.capstone.domain.enums.MetricEventType;
import org.capstone.repository.jdbc.OrderItemRepository;
import org.capstone.repository.jdbc.ProductRepository;
import org.capstone.repository.jdbc.UserMetricEventRepository;
import org.capstone.repository.jdbc.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserMetricEventService {
    private final UserMetricEventRepository eventRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    public UserMetricEvent recordEvent(
            MetricEventType eventType,
            Long userId,
            Long productId,
            String searchQuery,
            Long orderItemId) {
        if (eventType == null) {
            throw new IllegalArgumentException("Metric event type cannot be null.");
        }

        User user = userId == null ? null : userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        Product product = productId == null ? null : productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));
        OrderItem orderItem = orderItemId == null ? null : orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new IllegalArgumentException("Order item not found with ID: " + orderItemId));

        return eventRepository.save(UserMetricEvent.builder()
                .eventType(eventType)
                .user(user)
                .product(product)
                .searchQuery(searchQuery)
                .orderItem(orderItem)
                .timestamp(LocalDateTime.now())
                .build());
    }

    public List<UserMetricEvent> getEventsForUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null.");
        }
        if (userRepository.findById(userId).isEmpty()) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }
        return eventRepository.findAllByUserId(userId);
    }
}
