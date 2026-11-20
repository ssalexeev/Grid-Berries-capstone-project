package org.capstone.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.net.openssl.ciphers.Authentication;
import org.capstone.domain.Cart;
import org.capstone.domain.Product;
import org.capstone.dto.request.AddCartItemRequest;
import org.capstone.dto.response.CartResponse;
import org.capstone.security.AuthenticationService;
import org.capstone.service.CartService;
import org.capstone.service.ProductService;
import org.capstone.util.mapper.cart.CartMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cart")
public class CartController {
    private final AuthenticationService authenticationService;
    private final CartService cartService;
    private final CartMapper cartMapper;
    private final ProductService productService;


    @GetMapping
    public ResponseEntity<CartResponse> getCart() {
        Long userId = authenticationService.getUserFromAuthenticationContext().getId();
        Cart cart = cartService.getCart(userId);

        return ResponseEntity.ok(cartMapper.toResponse(cart));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(@Valid @RequestBody AddCartItemRequest request) {
        Long userId = authenticationService.getUserFromAuthenticationContext().getId();
        Product product = productService.getProductById(request.getProductId());
        Cart cart = cartService.addItem(userId, product, request.getQuantity());

        return ResponseEntity.ok(cartMapper.toResponse(cart));
    }

    @PatchMapping("/items/{itemId}")
    @Validated
    public ResponseEntity<CartResponse> patchItemQuantity(@PathVariable Long itemId,
                                                          @Min(1) @RequestParam("quantity") Integer quantity) {
        Long userId = authenticationService.getUserFromAuthenticationContext().getId();
        Cart cart = cartService.getCart(userId);
        Cart patchedCart = cartService.updateQuantity(cart, itemId, quantity);

        return ResponseEntity.ok(cartMapper.toResponse(patchedCart));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> removeItem(@PathVariable Long itemId) {
        Long userId = authenticationService.getUserFromAuthenticationContext().getId();
        cartService.removeItem(userId, itemId);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearCart() {
        Long userId = authenticationService.getUserFromAuthenticationContext().getId();
        cartService.clearUserCart(userId);

        return ResponseEntity.noContent().build();
    }
}
