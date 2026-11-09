package org.capstone.service;

import lombok.RequiredArgsConstructor;
import org.capstone.domain.Category;
import org.capstone.domain.Order;
import org.capstone.domain.OrderItem;
import org.capstone.domain.Product;
import org.capstone.domain.enums.OrderStatus;
import org.capstone.domain.enums.ProductStatus;
import org.capstone.repository.jpa.OrderRepository;
import org.capstone.repository.jpa.ProductRepository;
import org.capstone.repository.jpa.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RecommendationService {
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public List<Product> getRecommendations(Long userId, int limit) {
        if (userId == null || userRepository.findById(userId).isEmpty()) {
            throw new IllegalArgumentException("A valid user ID is required.");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("Recommendation limit must be positive.");
        }

        Map<Long, Integer> categoryAffinity = new HashMap<>();
        Set<Long> purchasedProductIds = new HashSet<>();
        for (Order order : orderRepository.findAllByUserId(userId)) {
            collectOrderSignals(order, categoryAffinity, purchasedProductIds);
        }

        Map<Long, Integer> popularity = new HashMap<>();
        for (Order order : orderRepository.findAll()) {
            if (order.getStatus() != OrderStatus.CANCELLED) {
                for (OrderItem item : order.getItems()) {
                    if (item.getProduct() != null && item.getProduct().getId() != null) {
                        popularity.merge(item.getProduct().getId(), quantity(item), Integer::sum);
                    }
                }
            }
        }

        return productRepository.findAll().stream()
                .filter(product -> product.getStatus() == ProductStatus.ACTIVE)
                .filter(product -> !purchasedProductIds.contains(product.getId()))
                .sorted(Comparator
                        .comparingInt((Product product) -> categoryScore(product, categoryAffinity))
                        .thenComparingInt(product -> popularity.getOrDefault(product.getId(), 0))
                        .reversed()
                        .thenComparing(Product::getId))
                .limit(limit)
                .toList();
    }

    private void collectOrderSignals(
            Order order,
            Map<Long, Integer> categoryAffinity,
            Set<Long> purchasedProductIds) {
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return;
        }
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            if (product == null || product.getId() == null) {
                continue;
            }
            purchasedProductIds.add(product.getId());
            if (product.getCategories() == null) {
                continue;
            }
            for (Category category : product.getCategories()) {
                if (category != null && category.getId() != null) {
                    categoryAffinity.merge(category.getId(), quantity(item), Integer::sum);
                }
            }
        }
    }

    private int categoryScore(Product product, Map<Long, Integer> categoryAffinity) {
        if (product.getCategories() == null) {
            return 0;
        }
        return product.getCategories().stream()
                .filter(category -> category != null && category.getId() != null)
                .mapToInt(category -> categoryAffinity.getOrDefault(category.getId(), 0))
                .sum();
    }

    private int quantity(OrderItem item) {
        return item.getQuantity() == null ? 1 : item.getQuantity();
    }
}
