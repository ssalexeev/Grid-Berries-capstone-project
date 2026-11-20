package org.capstone.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.capstone.domain.Cart;
import org.capstone.domain.CartItem;
import org.capstone.domain.Product;
import org.capstone.domain.User;
import org.capstone.repository.jpa.CartItemRepository;
import org.capstone.repository.jpa.CartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserService userService;
    private final ProductService productService;

    @Transactional
    public Cart getCart(Long userId) {
        return getOrCreateCart(userId);
    }

    @Transactional
    public Cart addItem(Long userId, Product product, Integer requiredQuantity) {
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null.");
        }
        if (requiredQuantity == null || requiredQuantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive.");
        }
        if (!product.isAvailable()) {
            throw new IllegalStateException("Product is not available");
        }

        Cart cart = getOrCreateCart(userId);
        CartItem existingItem = cart.getCartItems().stream()
                .filter(item -> item.getProduct() != null
                        && product.getId().equals(item.getProduct().getId()))
                .findFirst()
                .orElse(null);

        int resultingQuantity = requiredQuantity;
        if (existingItem != null) {
            if (existingItem.getQuantity() == null) {
                throw new IllegalStateException("Existing cart item has no quantity.");
            }
            resultingQuantity += existingItem.getQuantity();
        }
        if (!product.hasRequiredStockQuantity(resultingQuantity)) {
            throw new IllegalStateException("Insufficient stock for product.");
        }

        if (existingItem == null) {
            cart.addItem(
                    CartItem.builder()
                            .cart(cart)
                            .product(product)
                            .quantity(requiredQuantity)
                            .createdAt(LocalDateTime.now())
                            .build()
            );
        } else {
            existingItem.setQuantity(existingItem.getQuantity() + requiredQuantity);
        }

        cart.calculateTotalAmount();

        return cartRepository.save(cart);

    }

    @Transactional
    @Validated
    public Cart updateQuantity(Cart cart, Long cartItemId, @Positive int quantity) {
        CartItem item = getCartItem(cartItemId, cart.getId());
        Product product = productService.getProductById(item.getProduct().getId());

        if (!product.isAvailable()) {
            throw new IllegalStateException("Product is not available");
        }
        if (!product.hasRequiredStockQuantity(quantity)) {
            throw new IllegalStateException("Product has not required stock quantity");
        }

        item.setQuantity(quantity);

        cart.calculateTotalAmount();

        return cartRepository.save(cart);
    }

    @Transactional
    public Cart removeItem(Long userId, Long cartItemId) {
        Cart cart = getCartByUserId(userId);
        getCartItem(cartItemId, cart.getId());
        cart.getCartItems().removeIf(item -> cartItemId.equals(item.getId()));
        cart.calculateTotalAmount();
        return cartRepository.save(cart);
    }

    private CartItem getCartItem(Long cartItemId, Long cartId) {
        return cartItemRepository.findByIdAndCart_Id(cartItemId, cartId)
                .orElseThrow(() -> new EntityNotFoundException("Cart item not found by id: " + cartId + " and cart_id: " + cartId));
    }

    @Transactional
    public Cart clearUserCart(Long userId) {
        Cart cart = getCartByUserId(userId);
        cart.clearItems();
        return cartRepository.save(cart);
    }

    private Cart getOrCreateCart(Long userId) {
        User user = userService.getUserById(userId);
        return cartRepository.findByUserId(userId)
                .orElseGet(
                        () -> cartRepository.save(Cart.builder()
                                .user(user)
                                .totalAmount(java.math.BigDecimal.ZERO)
                                .updatedAt(LocalDateTime.now())
                                .build())
                );
    }

    private Cart getCartByUserId(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found for user ID: " + userId));
    }



}
