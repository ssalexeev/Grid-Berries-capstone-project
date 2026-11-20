package org.capstone.service;

import lombok.RequiredArgsConstructor;
import org.capstone.domain.Cart;
import org.capstone.domain.CartItem;
import org.capstone.domain.Order;
import org.capstone.domain.OrderItem;
import org.capstone.domain.Product;
import org.capstone.domain.SellerProfile;
import org.capstone.domain.User;
import org.capstone.domain.enums.OrderStatus;
import org.capstone.domain.enums.ProductStatus;
import org.capstone.repository.jpa.CartRepository;
import org.capstone.repository.jpa.OrderRepository;
import org.capstone.repository.jpa.ProductRepository;
import org.capstone.repository.jpa.SellerProfileRepository;
import org.capstone.repository.jpa.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final SellerProfileRepository sellerProfileRepository;

    @Transactional
    public Order placeOrder(Long userId) {
        User user = getUser(userId);
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found for user ID: " + userId));
        if (cart.getCartItems() == null || cart.getCartItems().isEmpty()) {
            throw new IllegalStateException("Cannot place an order from an empty cart.");
        }

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cart.getCartItems()) {
            if (cartItem.getProduct() == null || cartItem.getProduct().getId() == null) {
                throw new IllegalStateException("Cart contains an item without a product.");
            }
            Integer quantity = cartItem.getQuantity();
            if (quantity == null || quantity < 1) {
                throw new IllegalStateException("Cart item quantity must be positive.");
            }
            Product product = getProduct(cartItem.getProduct().getId());
            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw new IllegalStateException("Product is no longer available: " + product.getId());
            }
           //todo
            /*if (!productRepository.reserveStock(product.getId(), quantity)) {
                throw new IllegalStateException("Insufficient stock for product: " + product.getId());
            }*/

            orderItems.add(OrderItem.builder()
                    .product(product)
                    .quantity(quantity)
                    .unitPrice(product.getPrice())
                    .build());
        }

        LocalDateTime now = LocalDateTime.now();
        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.CREATED)
                .createdAt(now)
                .updatedAt(now)
                .items(orderItems)
                .build();
        order.calculateTotal();
        Order savedOrder = orderRepository.save(order);

        cart.setCartItems(new ArrayList<>());
        cart.setTotalAmount(BigDecimal.ZERO);
        cart.setUpdatedAt(now);
        cartRepository.save(cart);

        return savedOrder;
    }

    public Order getOrder(Long orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null.");
        }
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));
    }

    public List<Order> getOrderHistory(Long userId) {
        getUser(userId);
        return orderRepository.findAllByUserId(userId);
    }

    public List<Order> getIncomingOrders(Long sellerProfileId) {
        getSellerProfile(sellerProfileId);
        return orderRepository.findAll().stream()
                .filter(order -> containsSellerProduct(order, sellerProfileId))
                .toList();
    }

    @Transactional
    public Order updateStatus(Long orderId, Long sellerProfileId, OrderStatus nextStatus) {
        Order order = getOrderForUpdate(orderId);
        getSellerProfile(sellerProfileId);
        if (!containsSellerProduct(order, sellerProfileId)) {
            throw new IllegalStateException("Order does not contain products from this seller.");
        }
        transition(order, nextStatus);
        if (nextStatus == OrderStatus.CANCELLED) {
            restoreReservedStock(order);
        }
        return orderRepository.save(order);
    }

    @Transactional
    public Order cancelOrder(Long orderId, Long userId) {
        Order order = getOrderForUpdate(orderId);
        getUser(userId);
        if (order.getUser() == null || !userId.equals(order.getUser().getId())) {
            throw new IllegalStateException("Order does not belong to this user.");
        }
        transition(order, OrderStatus.CANCELLED);
        restoreReservedStock(order);
        return orderRepository.save(order);
    }

    private void transition(Order order, OrderStatus nextStatus) {
        if (nextStatus == null) {
            throw new IllegalArgumentException("Order status cannot be null.");
        }
        if (order.getStatus() == null) {
            throw new IllegalStateException("Order has no current status.");
        }
        boolean allowed = switch (order.getStatus()) {
            case CREATED -> nextStatus == OrderStatus.IN_PROCESSING || nextStatus == OrderStatus.CANCELLED;
            case IN_PROCESSING -> nextStatus == OrderStatus.SHIPPED || nextStatus == OrderStatus.CANCELLED;
            case SHIPPED -> nextStatus == OrderStatus.COMPLETED;
            case COMPLETED, CANCELLED -> false;
        };
        if (!allowed) {
            throw new IllegalStateException(
                    "Order cannot transition from " + order.getStatus() + " to " + nextStatus + ".");
        }
        order.updateStatus(nextStatus);
    }

    private void restoreReservedStock(Order order) {
        for (OrderItem item : order.getItems()) {
            if (item.getProduct() == null || item.getProduct().getId() == null || item.getQuantity() == null) {
                throw new IllegalStateException("Order contains an invalid item and stock cannot be restored.");
            }
            productRepository.restoreStockQuantity(item.getProduct().getId(), item.getQuantity());
        }
    }

    private boolean containsSellerProduct(Order order, Long sellerProfileId) {
        return order.getItems().stream()
                .map(OrderItem::getProduct)
                .anyMatch(product -> product != null
                        && product.getSellerProfile() != null
                        && sellerProfileId.equals(product.getSellerProfile().getId()));
    }

    private User getUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null.");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
    }

    private Order getOrderForUpdate(Long orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null.");
        }
        return orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));
    }

    private Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));
    }

    private SellerProfile getSellerProfile(Long sellerProfileId) {
        if (sellerProfileId == null) {
            throw new IllegalArgumentException("Seller profile ID cannot be null.");
        }
        return sellerProfileRepository.findById(sellerProfileId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Seller profile not found with ID: " + sellerProfileId));
    }
}
