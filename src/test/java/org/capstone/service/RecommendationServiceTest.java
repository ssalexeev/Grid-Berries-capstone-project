package org.capstone.service;

import org.capstone.domain.Category;
import org.capstone.domain.Product;
import org.capstone.domain.User;
import org.capstone.domain.UserMetricEvent;
import org.capstone.domain.enums.MetricEventType;
import org.capstone.domain.enums.ProductStatus;
import org.capstone.repository.jdbc.ProductRepository;
import org.capstone.repository.jdbc.UserMetricEventRepository;
import org.capstone.repository.jdbc.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserMetricEventRepository eventRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RecommendationService recommendationService;

    @Test
    void recommendationsRankUserInterestsAndExcludePurchasedProducts() {
        User user = User.builder().id(1L).build();
        Category preferredCategory = Category.builder().id(2L).build();
        Product viewed = product(3L, preferredCategory);
        Product recommendation = product(4L, preferredCategory);
        Product purchased = product(5L, preferredCategory);
        Product unrelated = product(6L, Category.builder().id(9L).build());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findAllByUserId(1L)).thenReturn(List.of(
                UserMetricEvent.builder().eventType(MetricEventType.VIEW).product(viewed).build(),
                UserMetricEvent.builder().eventType(MetricEventType.PURCHASE).product(purchased).build()));
        when(eventRepository.findAll()).thenReturn(List.of());
        when(productRepository.findAll()).thenReturn(List.of(unrelated, purchased, recommendation));

        List<Product> products = recommendationService.getRecommendations(1L, 2);

        assertEquals(List.of(recommendation, unrelated), products);
    }

    private Product product(Long id, Category category) {
        return Product.builder()
                .id(id)
                .category(category)
                .status(ProductStatus.ACTIVE)
                .price(BigDecimal.ONE)
                .build();
    }
}
