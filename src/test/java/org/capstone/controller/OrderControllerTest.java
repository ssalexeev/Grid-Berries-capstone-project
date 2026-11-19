package org.capstone.controller;

import org.capstone.domain.Order;
import org.capstone.domain.OrderItem;
import org.capstone.domain.User;
import org.capstone.dto.request.PlaceOrderRequest;
import org.capstone.dto.response.OrderResponse;
import org.capstone.security.AuthenticationService;
import org.capstone.service.OrderService;
import org.capstone.util.mapper.order.OrderMapper;
import org.capstone.util.mapper.order.OrderMapperImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        OrderMapperImpl.class
})
class OrderControllerTest {
    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    AuthenticationService authenticationService;
    @MockitoBean
    OrderService orderService;

    @Test
    void getBuyerOrders_ReturnsOrders() throws Exception {
        User user = User.builder().id(5L).build();
        Order order = Order.builder().id(20L).build();
        when(authenticationService.getUserFromAuthenticationContext()).thenReturn(user);
        when(orderService.getOrdersHistory(5L)).thenReturn(List.of(order));

        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(20));
    }

    @Test
    void placeOrder_ReturnsCreatedOrder() throws Exception {
        User user = User.builder().id(5L).build();
        Order saved = Order.builder()
                .id(21L)
                .items(List.of(OrderItem.builder().build()))
                .build();

        when(authenticationService.getUserFromAuthenticationContext())
                .thenReturn(user);
        when(orderService.placeOrder(eq(user), any(Order.class)))
                .thenReturn(saved);

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"items":[{"productId":9,"quantity":2}]}
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(21));

        verify(orderService).placeOrder(eq(user), any(Order.class));
    }

    @Test
    void getSellerOrders_ReturnsOrders() throws Exception {
        when(authenticationService.getSellerIdFromAuthenticationContext()).thenReturn(7L);
        when(orderService.getIncomingOrders(7L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/orders/seller/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
