package org.capstone.service;

import org.capstone.domain.Cart;
import org.capstone.domain.CartItem;
import org.capstone.domain.Order;
import org.capstone.domain.OrderItem;
import org.capstone.domain.Product;
import org.capstone.domain.SellerProfile;
import org.capstone.domain.User;
import org.capstone.domain.enums.OrderStatus;
import org.capstone.domain.enums.ProductStatus;
import org.capstone.repository.jdbc.CartRepository;
import org.capstone.repository.jdbc.OrderRepository;
import org.capstone.repository.jdbc.ProductRepository;
import org.capstone.repository.jdbc.SellerProfileRepository;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SellerProfileRepository sellerProfileRepository;
    @Mock
    private UserMetricEventService metricEventService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void placeOrderReservesStockSnapshotsPriceAndClearsCart() {
        User user = User.builder().id(1L).build();
        Product product = product();
        Cart cart = Cart.builder().id(2L).user(user).items(new ArrayList<>(List.of(
                CartItem.builder().cartId(2L).product(product).quantity(2).build()))).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(4L)).thenReturn(Optional.of(product));
        when(productRepository.reserveStock(4L, 2)).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(7L);
            order.getItems().get(0).setId(9L);
            order.getItems().get(0).setOrderId(7L);
            return order;
        });
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order order = orderService.placeOrder(1L);

        assertEquals(OrderStatus.CREATED, order.getStatus());
        assertEquals(new BigDecimal("15.00"), order.getTotalAmount());
        assertEquals(0, cart.getItems().size());
        verify(productRepository).reserveStock(4L, 2);
        verify(metricEventService).recordEvent(
                org.capstone.domain.enums.MetricEventType.PURCHASE, 1L, 4L, null, 9L);
    }

    @Test
    void sellerCancellationRestoresReservedStock() {
        SellerProfile seller = SellerProfile.builder().id(3L).build();
        Product product = product();
        product.setSellerProfile(seller);
        Order order = Order.builder()
                .id(7L)
                .status(OrderStatus.CREATED)
                .items(List.of(OrderItem.builder().product(product).quantity(2).build()))
                .build();
        when(orderRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(order));
        when(sellerProfileRepository.findById(3L)).thenReturn(Optional.of(seller));
        when(orderRepository.save(order)).thenReturn(order);

        Order cancelled = orderService.updateStatus(7L, 3L, OrderStatus.CANCELLED);

        assertEquals(OrderStatus.CANCELLED, cancelled.getStatus());
        verify(productRepository).restoreStock(4L, 2);
    }

    @Test
    void completedOrderCannotBeCancelled() {
        Order order = Order.builder().id(7L).status(OrderStatus.COMPLETED).items(List.of()).build();
        when(orderRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(order));
        when(userRepository.findById(1L)).thenReturn(Optional.of(User.builder().id(1L).build()));

        assertThrows(IllegalStateException.class, () -> orderService.cancelOrder(7L, 1L));
    }

    private Product product() {
        return Product.builder()
                .id(4L)
                .price(new BigDecimal("7.50"))
                .stockQuantity(8)
                .status(ProductStatus.ACTIVE)
                .build();
    }
}
