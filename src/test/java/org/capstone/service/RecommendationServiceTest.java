package org.capstone.service;

import org.capstone.domain.Category;
import org.capstone.domain.Order;
import org.capstone.domain.OrderItem;
import org.capstone.domain.Product;
import org.capstone.domain.User;
import org.capstone.domain.enums.OrderStatus;
import org.capstone.domain.enums.ProductStatus;
import org.capstone.repository.jpa.OrderRepository;
import org.capstone.repository.jpa.ProductRepository;
import org.capstone.repository.jpa.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {
    @Mock
    private ProductRepository productRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RecommendationService recommendationService;

    @Test
    void recommendationsRankPurchasedCategoriesAndExcludePreviouslyPurchasedProducts() {
        User user = User.builder().id(1L).build();
        Category preferredCategory = Category.builder().id(2L).build();
        Product purchased = product(5L, preferredCategory);
        Product recommendation = product(4L, preferredCategory);
        Product unrelated = product(6L, Category.builder().id(9L).build());
        Order purchase = Order.builder()
                .status(OrderStatus.COMPLETED)
                .items(List.of(OrderItem.builder().product(purchased).quantity(1).build()))
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.findAllByUserId(1L)).thenReturn(List.of(purchase));
        when(orderRepository.findAll()).thenReturn(List.of(purchase));
        when(productRepository.findAll()).thenReturn(List.of(unrelated, purchased, recommendation));

        List<Product> products = recommendationService.getRecommendations(1L, 2);

        assertEquals(List.of(recommendation, unrelated), products);
    }

    private Product product(Long id, Category category) {
        return Product.builder()
                .id(id)
                .categories(Set.of(category))
                .status(ProductStatus.ACTIVE)
                .price(BigDecimal.ONE)
                .build();
    }
}
