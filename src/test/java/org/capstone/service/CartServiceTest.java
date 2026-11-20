package org.capstone.service;

import org.capstone.domain.Cart;
import org.capstone.domain.CartItem;
import org.capstone.domain.Product;
import org.capstone.domain.User;
import org.capstone.domain.enums.ProductStatus;
import org.capstone.repository.jpa.CartItemRepository;
import org.capstone.repository.jpa.CartRepository;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {
    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private UserService userService;
    @Mock
    private ProductService productService;

    @InjectMocks
    private CartService cartService;

    @Test
    void addItemIncrementsExistingLineAndRecalculatesTotal() {
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
                .cartItems(new ArrayList<>(List.of(existingItem)))
                .build();
        when(userService.getUserById(1L)).thenReturn(user);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cart updated = cartService.addItem(1L, product, 3);

        assertEquals(5, updated.getCartItems().get(0).getQuantity());
        assertEquals(new BigDecimal("17.50"), updated.getTotalAmount());
    }

    @Test
    void addItemRejectsQuantityThatExceedsRemainingStock() {
        Product product = Product.builder()
                .id(4L)
                .price(new BigDecimal("3.50"))
                .stockQuantity(4)
                .status(ProductStatus.ACTIVE)
                .build();
        CartItem existingItem = CartItem.builder().id(8L).product(product).quantity(2).build();
        Cart cart = Cart.builder()
                .id(2L)
                .cartItems(new ArrayList<>(List.of(existingItem)))
                .build();
        when(userService.getUserById(1L)).thenReturn(User.builder().id(1L).build());
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        assertThrows(IllegalStateException.class, () -> cartService.addItem(1L, product, 3));
    }
}
