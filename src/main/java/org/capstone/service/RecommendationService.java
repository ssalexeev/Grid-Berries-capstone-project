package org.capstone.service;

import lombok.RequiredArgsConstructor;
import org.capstone.domain.Category;
import org.capstone.domain.Product;
import org.capstone.domain.UserMetricEvent;
import org.capstone.domain.enums.MetricEventType;
import org.capstone.domain.enums.ProductStatus;
import org.capstone.repository.jdbc.ProductRepository;
import org.capstone.repository.jdbc.UserMetricEventRepository;
import org.capstone.repository.jdbc.UserRepository;
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
    private final UserMetricEventRepository eventRepository;
    private final UserRepository userRepository;
    private final UserMetricEventService userMetricEventService;
    public List<Product> getRecommendations(Long userId, int limit) {
        if (userId == null || userRepository.findById(userId).isEmpty()) {
            throw new IllegalArgumentException("A valid user ID is required.");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("Recommendation limit must be positive.");
        }

        Map<Long, Integer> categoryAffinity = new HashMap<>();
        Set<Long> purchasedProductIds = new HashSet<>();
        for (UserMetricEvent event : userMetricEventService.getEventsForUser(userId)) {
            Product product = event.getProduct();
            if (product == null) {
                continue;
            }
            List<Category> categories = product.getCategories() == null || product.getCategories().isEmpty()
                    ? product.getCategory() == null ? List.of() : List.of(product.getCategory())
                    : product.getCategories();

            if (event.getEventType() == MetricEventType.PURCHASE) {
                purchasedProductIds.add(product.getId());
            }
            int weight = switch (event.getEventType()) {
                case VIEW -> 1;
                case ADD_TO_CART -> 2;
                case PURCHASE -> 3;
                case SEARCH -> 0;
            };
            for (Category category : categories) {
                if (category != null && category.getId() != null) {
                    categoryAffinity.merge(category.getId(), weight, Integer::sum);
                }
            }
        }

        Map<Long, Integer> popularity = new HashMap<>();
        for (UserMetricEvent event : eventRepository.findAll()) {
            if (event.getEventType() == MetricEventType.VIEW && event.getProduct() != null) {
                popularity.merge(event.getProduct().getId(), 1, Integer::sum);
            }
        }

        return productRepository.findAll().stream()
                .filter(product -> product.getStatus() == ProductStatus.ACTIVE)
                .filter(product -> !purchasedProductIds.contains(product.getId()))
                .sorted(Comparator
                        .comparingInt((Product product) -> categoryScore(product.getCategory(), categoryAffinity))
                        .thenComparingInt(product -> popularity.getOrDefault(product.getId(), 0))
                        .reversed()
                        .thenComparing(Product::getId))
                .limit(limit)
                .toList();
    }

    private int categoryScore(Category category, Map<Long, Integer> categoryAffinity) {
        return category == null || category.getId() == null
                ? 0
                : categoryAffinity.getOrDefault(category.getId(), 0);
    }
}
