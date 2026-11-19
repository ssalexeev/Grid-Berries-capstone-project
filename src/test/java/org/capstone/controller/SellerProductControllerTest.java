package org.capstone.controller;

import org.capstone.controller.seller.SellerProductController;
import org.capstone.domain.Product;
import org.capstone.dto.response.ProductResponse;
import org.capstone.security.AuthenticationService;
import org.capstone.service.ProductService;
import org.capstone.util.mapper.product.ProductMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SellerProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class SellerProductControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean ProductService productService;
    @MockitoBean ProductMapper productMapper;
    @MockitoBean AuthenticationService authenticationService;

    @Test
    void getSellerProducts_ReturnsProducts() throws Exception {
        when(authenticationService.getSellerIdFromAuthenticationContext()).thenReturn(7L);
        Product product = Product.builder().id(12L).title("Strawberries").build();
        when(productService.getProductsBySeller(7L)).thenReturn(List.of(product));
        when(productMapper.toResponse(product))
                .thenReturn(ProductResponse.builder().id(12L).title("Strawberries").build());

        mockMvc.perform(get("/api/v1/seller"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(12))
                .andExpect(jsonPath("$[0].title").value("Strawberries"));
    }
}
