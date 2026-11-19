package org.capstone.controller;

import org.capstone.domain.Cart;
import org.capstone.domain.Product;
import org.capstone.domain.User;
import org.capstone.dto.request.AddCartItemRequest;
import org.capstone.dto.response.CartResponse;
import org.capstone.security.AuthenticationService;
import org.capstone.service.CartService;
import org.capstone.service.ProductService;
import org.capstone.util.mapper.cart.CartMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
@AutoConfigureMockMvc(addFilters = false)
class CartControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean AuthenticationService authenticationService;
    @MockitoBean CartService cartService;
    @MockitoBean CartMapper cartMapper;
    @MockitoBean ProductService productService;

    @Test
    void getCart_ReturnsCart() throws Exception {
        User user = User.builder().id(5L).build();
        Cart cart = Cart.builder().id(1L).build();
        when(authenticationService.getUserFromAuthenticationContext()).thenReturn(user);
        when(cartService.getCart(5L)).thenReturn(cart);
        when(cartMapper.toResponse(cart)).thenReturn(new CartResponse());

        mockMvc.perform(get("/api/v1/cart")).andExpect(status().isOk());
    }

    @Test
    void addItem_ReturnsCart() throws Exception {
        User user = User.builder().id(5L).build();
        Product product = Product.builder().id(9L).build();
        Cart cart = Cart.builder().id(1L).build();
        when(authenticationService.getUserFromAuthenticationContext()).thenReturn(user);
        when(productService.getProductById(9L)).thenReturn(product);
        when(cartService.addItem(5L, product, 2)).thenReturn(cart);
        when(cartMapper.toResponse(cart)).thenReturn(new CartResponse());

        mockMvc.perform(post("/api/v1/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":9,"quantity":2}
                                """))
                .andExpect(status().isOk());

        verify(cartService).addItem(5L, product, 2);
    }

    @Test
    void patchQuantity_ReturnsUpdatedCart() throws Exception {
        User user = User.builder().id(5L).build();
        Cart cart = Cart.builder().id(1L).build();
        when(authenticationService.getUserFromAuthenticationContext()).thenReturn(user);
        when(cartService.getCart(5L)).thenReturn(cart);
        when(cartService.updateQuantity(cart, 3L, 4)).thenReturn(cart);
        when(cartMapper.toResponse(cart)).thenReturn(new CartResponse());

        mockMvc.perform(patch("/api/v1/cart/items/3").param("quantity", "4"))
                .andExpect(status().isOk());

        verify(cartService).updateQuantity(cart, 3L, 4);
    }

    @Test
    void removeItem_ReturnsNoContent() throws Exception {
        when(authenticationService.getUserFromAuthenticationContext()).thenReturn(User.builder().id(5L).build());

        mockMvc.perform(delete("/api/v1/cart/items/3"))
                .andExpect(status().isNoContent());

        verify(cartService).removeItem(5L, 3L);
    }

    @Test
    void clearCart_ReturnsNoContent() throws Exception {
        when(authenticationService.getUserFromAuthenticationContext()).thenReturn(User.builder().id(5L).build());

        mockMvc.perform(delete("/api/v1/cart/clear"))
                .andExpect(status().isNoContent());

        verify(cartService).clearUserCart(5L);
    }
}
