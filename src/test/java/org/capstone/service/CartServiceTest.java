package org.capstone.service;

import org.capstone.domain.Cart;
import org.capstone.domain.CartItem;
import org.capstone.domain.Product;
import org.capstone.domain.User;
import org.capstone.domain.enums.ProductStatus;
import org.capstone.repository.jdbc.CartRepository;
import org.capstone.repository.jdbc.ProductRepository;
import org.capstone.repository.jdbc.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {
    @Mock
    private CartRepository cartRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMetricEventService metricEventService;

    @InjectMocks
    private CartService cartService;

    @Test
    void addProductIncrementsExistingLineAndRecalculatesTotal() {
        User user = User.builder().id(1L).build();
        Product product = Product.builder()
                .id(4L)
                .price(new BigDecimal("3.50"))
                .stockQuantity(10)
                .status(ProductStatus.ACTIVE)
                .build();
        CartItem existingItem = CartItem.builder().id(8L).product(product).quantity(2).build();
        Cart cart = Cart.builder()
                .id(2L)
                .user(user)
                .items(new ArrayList<>(List.of(existingItem)))
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(4L)).thenReturn(Optional.of(product));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cart updated = cartService.addProduct(1L, 4L, 3);

        assertEquals(5, updated.getItems().get(0).getQuantity());
        assertEquals(new BigDecimal("17.50"), updated.getTotalAmount());
        verify(metricEventService).recordEvent(
                org.capstone.domain.enums.MetricEventType.ADD_TO_CART, 1L, 4L, null, null);
    }
}
