package org.capstone.IT.service;

import org.capstone.IT.AbstractIntegrationTest;
import org.capstone.domain.Cart;
import org.capstone.domain.CartItem;
import org.capstone.domain.Order;
import org.capstone.domain.Product;
import org.capstone.domain.SellerProfile;
import org.capstone.domain.User;
import org.capstone.domain.enums.OrderStatus;
import org.capstone.domain.enums.ProductStatus;
import org.capstone.domain.enums.Role;
import org.capstone.repository.jpa.CartRepository;
import org.capstone.repository.jpa.OrderRepository;
import org.capstone.repository.jpa.ProductRepository;
import org.capstone.repository.jpa.SellerProfileRepository;
import org.capstone.repository.jpa.UserRepository;
import org.capstone.service.CartService;
import org.capstone.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderServiceIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SellerProfileRepository sellerProfileRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private CartService cartService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderRepository orderRepository;

    @Test
    void placingOrderReservesStockCreatesOrderAndEmptiesCart() {
        TestData data = createTestData(5);
        cartService.addProduct(data.buyer().getId(), data.product().getId(), 2);

        Order order = orderService.placeOrder(data.buyer().getId());

        assertEquals(OrderStatus.CREATED, order.getStatus());
        assertEquals(new BigDecimal("15.00"), order.getTotalAmount());
        assertEquals(2, order.getItems().get(0).getQuantity());
        assertEquals(3, productRepository.findById(data.product().getId()).orElseThrow().getStockQuantity());
        Cart cart = cartRepository.findByUserId(data.buyer().getId()).orElseThrow();
        assertEquals(0, cart.getTotalAmount().compareTo(BigDecimal.ZERO));
        assertEquals(0, cart.getCartItems().size());

        Order cancelled = orderService.cancelOrder(order.getId(), data.buyer().getId());
        assertEquals(OrderStatus.CANCELLED, cancelled.getStatus());
        assertEquals(5, productRepository.findById(data.product().getId()).orElseThrow().getStockQuantity());
    }

    @Test
    void failedMultiProductOrderRollsBackEarlierStockReservations() {
        TestData data = createTestData(5);
        Product secondProduct = createProduct(data.sellerProfile(), 1);
        Cart cart = Cart.builder()
                .user(data.buyer())
                .updatedAt(LocalDateTime.now())
                .cartItems(new ArrayList<>(List.of(
                        CartItem.builder().product(data.product()).quantity(2).build(),
                        CartItem.builder().product(secondProduct).quantity(1).build())))
                .build();
        cart.calculateTotal();
        Cart savedCart = cartRepository.save(cart);
        secondProduct.setStockQuantity(0);
        productRepository.save(secondProduct);

        assertThrows(IllegalStateException.class, () -> orderService.placeOrder(data.buyer().getId()));

        assertEquals(5, productRepository.findById(data.product().getId()).orElseThrow().getStockQuantity());
        assertEquals(0, orderRepository.findAllByUserId(data.buyer().getId()).size());
        assertEquals(2, cartRepository.findById(savedCart.getId()).orElseThrow().getCartItems().size());
    }

    private TestData createTestData(int stock) {
        User buyer = userRepository.save(User.builder()
                .email("buyer-" + UUID.randomUUID() + "@example.test")
                .passwordHash("not-used-by-this-test")
                .role(Role.BUYER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build());
        User sellerUser = userRepository.save(User.builder()
                .email("seller-" + UUID.randomUUID() + "@example.test")
                .passwordHash("not-used-by-this-test")
                .role(Role.SELLER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build());
        SellerProfile seller = sellerProfileRepository.save(SellerProfile.builder()
                .user(sellerUser)
                .storeName("Test Store")
                .storeDescription("Testing")
                .contactInfo("test@example.test")
                .build());
        Product product = createProduct(seller, stock);
        return new TestData(buyer, seller, product);
    }

    private Product createProduct(SellerProfile seller, int stock) {
        return productRepository.save(Product.builder()
                .sellerProfile(seller)
                .title("Test product")
                .description("For integration test")
                .price(new BigDecimal("7.50"))
                .stockQuantity(stock)
                .status(ProductStatus.ACTIVE)
                .build());
    }

    private record TestData(User buyer, SellerProfile sellerProfile, Product product) {
    }
}
