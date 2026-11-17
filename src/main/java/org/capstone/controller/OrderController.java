package org.capstone.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.capstone.domain.Order;
import org.capstone.domain.User;
import org.capstone.domain.enums.OrderStatus;
import org.capstone.dto.request.PlaceOrderRequest;
import org.capstone.dto.response.OrderResponse;
import org.capstone.security.AuthenticationService;
import org.capstone.service.OrderService;
import org.capstone.util.mapper.order.OrderMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
@PreAuthorize("hasAnyRole('USER', 'SELLER')")
public class OrderController {
    private final AuthenticationService authenticationService;
    private final OrderService orderService;
    private final OrderMapper orderMapper;

    @GetMapping("/orders")
    public ResponseEntity<List<OrderResponse>> getBuyerOrders() {
        User user = authenticationService.getUserFromAuthenticationContext();
        List<OrderResponse> ordersHistory = orderService.getOrdersHistory(user.getId()).stream()
                .map(orderMapper::toResponse)
                .toList();
        return ResponseEntity.ok(ordersHistory);
    }

    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        User user = authenticationService.getUserFromAuthenticationContext();
        Order requestedOrder = orderMapper.toEntity(request);
        Order order = orderService.placeOrder(user, requestedOrder);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderMapper.toResponse(order));
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long orderId) {
        Order order = orderService.getOrder(orderId);

        return ResponseEntity.ok(orderMapper.toResponse(order));
    }

    @GetMapping("/seller/orders")
    public ResponseEntity<List<OrderResponse>> getSellerOrders() {
        Long sellerId = authenticationService.getSellerIdFromAuthenticationContext();

        List<OrderResponse> responses = orderService.getIncomingOrders(sellerId).stream().map(orderMapper::toResponse).toList();

        return ResponseEntity.ok(responses);
    }

    @PatchMapping("/orders/{orderId}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<OrderResponse> patchOrderStatus(
            @PathVariable Long orderId,
            @RequestParam("status") String status) {
        Long sellerId = authenticationService.getSellerIdFromAuthenticationContext();
        OrderStatus orderStatus = OrderStatus.fromString(status);

        Order order = orderService.updateStatus(orderId, sellerId, orderStatus);
        return ResponseEntity.ok(orderMapper.toResponse(order));
    }

    @PatchMapping("/orders/{orderId}")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long orderId) {
        User user = authenticationService.getUserFromAuthenticationContext();
        Order order = orderService.cancelOrder(orderId, user.getId());

        return ResponseEntity.ok(orderMapper.toResponse(order));
    }

    /* @PatchMapping("/orders/{orderId}")
     public ResponseEntity<OrderResponse> patchOrder(
             @PathVariable Long orderId,
             @Valid @RequestBody UpdateOrderRequest request) {
         orderService.
         return null;
     }*/
}
