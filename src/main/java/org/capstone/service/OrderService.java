package org.capstone.service;

import lombok.RequiredArgsConstructor;
import org.capstone.domain.Order;
import org.capstone.domain.OrderItem;
import org.capstone.domain.Product;
import org.capstone.domain.SellerProfile;
import org.capstone.domain.User;
import org.capstone.domain.enums.OrderStatus;
import org.capstone.repository.jpa.OrderRepository;
import org.capstone.repository.jpa.ProductRepository;
import org.capstone.repository.jpa.SellerProfileRepository;
import org.capstone.repository.jpa.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final SellerProfileRepository sellerProfileRepository;


    @Transactional
    public Order placeOrder(User user, Order requestedOrder) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User must have an ID to place an order.");
        }
        if (requestedOrder == null || requestedOrder.getItems() == null || requestedOrder.getItems().isEmpty()) {
            throw new IllegalArgumentException("An order must contain at least one item.");
        }

        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.CREATED)
                .build();
        List<OrderItem> orderItems = new ArrayList<>();
        for (OrderItem requestedItem : requestedOrder.getItems()) {
            if (requestedItem == null
                    || requestedItem.getProduct() == null
                    || requestedItem.getProduct().getId() == null
                    || requestedItem.getQuantity() == null
                    || requestedItem.getQuantity() <= 0) {
                throw new IllegalArgumentException("Order items require a product ID and positive quantity.");
            }
            Integer quantity = requestedItem.getQuantity();
            Product product = getProduct(requestedItem.getProduct().getId());
            if (!product.isAvailable()) {
                throw new IllegalStateException("Product is no longer available: " + product.getId());
            }
            if (productRepository.reserveStockIfAvailable(product.getId(), quantity) != 1) {
                throw new IllegalStateException("Insufficient stock for product: " + product.getId());
            }
            orderItems.add(OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(quantity)
                    .unitPrice(product.getPrice())
                    .build());
        }

        order.setItems(orderItems);
        order.calculateTotal();
        return orderRepository.save(order);
    }

    public Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));
    }

    public List<Order> getOrdersHistory(Long userId) {
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
        Order order = getOrderById(orderId);
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
        Order order = getOrderById(orderId);
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

    private Order getOrderById(Long orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null.");
        }
        return orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));
    }

    private Product getProduct(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID cannot be null.");
        }
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
