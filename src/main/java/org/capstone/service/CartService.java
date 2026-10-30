package org.capstone.service;

import lombok.RequiredArgsConstructor;
import org.capstone.domain.Cart;
import org.capstone.domain.CartItem;
import org.capstone.domain.Product;
import org.capstone.domain.User;
import org.capstone.domain.enums.MetricEventType;
import org.capstone.domain.enums.ProductStatus;
import org.capstone.repository.jdbc.CartRepository;
import org.capstone.repository.jdbc.ProductRepository;
import org.capstone.repository.jdbc.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final UserMetricEventService metricEventService;

    @Transactional
    public Cart getCart(Long userId) {
        return getOrCreateCart(userId);
    }

    @Transactional
    public Cart addProduct(Long userId, Long productId, int quantity) {
        validateQuantity(quantity);
        Cart cart = getOrCreateCart(userId);
        Product product = getProduct(productId);
        requireAvailable(product);

        CartItem existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct() != null
                        && productId.equals(item.getProduct().getId()))
                .findFirst()
                .orElse(null);

        int resultingQuantity = quantity + (existingItem == null ? 0 : existingItem.getQuantity());

        requireStock(product, resultingQuantity);

        if (existingItem == null) {
            cart.getItems().add(
                    CartItem.builder()
                            .cartId(cart.getId())
                            .product(product)
                            .quantity(quantity)
                            .addedAt(LocalDateTime.now())
                            .build()
            );
        } else {
            existingItem.setQuantity(resultingQuantity);
        }

        cart.setUpdatedAt(LocalDateTime.now());
        cart.calculateTotal();

        Cart saved = cartRepository.save(cart);

        metricEventService.recordEvent(MetricEventType.ADD_TO_CART, userId, productId, null, null);
        return saved;

    }

    @Transactional
    public Cart updateQuantity(Long userId, Long cartItemId, int quantity) {
        validateQuantity(quantity);
        Cart cart = getCartOrFail(userId);
        CartItem item = findCartItem(cart, cartItemId);
        Product product = getProduct(item.getProduct().getId());
        requireAvailable(product);
        requireStock(product, quantity);
        item.setProduct(product);
        item.setQuantity(quantity);
        cart.setUpdatedAt(LocalDateTime.now());
        cart.calculateTotal();
        return cartRepository.save(cart);
    }

    @Transactional
    public Cart removeItem(Long userId, Long cartItemId) {
        Cart cart = getCartOrFail(userId);
        findCartItem(cart, cartItemId);
        cart.getItems().removeIf(item -> cartItemId.equals(item.getId()));
        cart.setUpdatedAt(LocalDateTime.now());
        cart.calculateTotal();
        return cartRepository.save(cart);
    }

    @Transactional
    public Cart clearCart(Long userId) {
        Cart cart = getCartOrFail(userId);
        cart.getItems().clear();
        cart.setUpdatedAt(LocalDateTime.now());
        cart.calculateTotal();
        return cartRepository.save(cart);
    }

    private Cart getOrCreateCart(Long userId) {
        User user = getUser(userId);
        return cartRepository.findByUserId(userId).orElseGet(() -> cartRepository.save(Cart.builder()
                .user(user)
                .totalAmount(java.math.BigDecimal.ZERO)
                .updatedAt(LocalDateTime.now())
                .build()));
    }

    private Cart getCartOrFail(Long userId) {
        getUser(userId);
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found for user ID: " + userId));
    }

    private User getUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null.");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
    }

    private Product getProduct(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID cannot be null.");
        }
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));
    }

    private CartItem findCartItem(Cart cart, Long cartItemId) {
        if (cartItemId == null) {
            throw new IllegalArgumentException("Cart item ID cannot be null.");
        }
        return cart.getItems().stream()
                .filter(item -> cartItemId.equals(item.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cart item not found in this user's cart: " + cartItemId));
    }

    private void requireAvailable(Product product) {
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new IllegalStateException("Product is not available for purchase.");
        }
    }

    private void requireStock(Product product, int quantity) {
        if (product.getStockQuantity() == null || product.getStockQuantity() < quantity) {
            throw new IllegalStateException("Requested quantity exceeds available stock.");
        }
    }

    private void validateQuantity(int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be positive.");
        }
    }
}
