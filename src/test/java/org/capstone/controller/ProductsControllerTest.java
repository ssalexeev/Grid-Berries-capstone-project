package org.capstone.controller;

import org.capstone.controller.product.ProductsController;
import org.capstone.domain.Product;
import org.capstone.domain.SellerProfile;
import org.capstone.security.AuthenticationService;
import org.capstone.service.ProductService;
import org.capstone.service.SellerProfileService;
import org.capstone.util.mapper.product.ProductMapperImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ProductMapperImpl.class)
class ProductsControllerTest {
    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    ProductService productService;
    @MockitoBean
    AuthenticationService authenticationService;


    private final String validProduct = """
            {
              "title":"Strawberries",
              "description":"Fresh",
              "price":5.99,
              "stockQuantity":10,
              "imageUrl":"https://example.com/berries.jpg",
              "categoryIds":[1]
            }
            """;

    @Test
    void getProducts_ReturnsSlice() throws Exception {
        Slice<Product> slice = new SliceImpl<>(
                List.of(Product.builder().id(1L).title("Strawberries").build()),
                PageRequest.of(0, 15),
                false);

        when(productService.getProductsSlice(any(), any())).thenReturn(slice);


        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Strawberries"));
    }

    @Test
    void createProduct_ReturnsCreatedLocation() throws Exception {
        when(authenticationService.getSellerIdFromAuthenticationContext()).thenReturn(7L);
        Product saved = Product.builder()
                .id(7L)
                .title("Strawberries")
                .build();

        when(productService.createProduct(eq(7L), any(Product.class))).thenReturn(saved);


        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProduct))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/products/7")))
                .andExpect(jsonPath("$.title").value("Strawberries"));
    }

    @Test
    void getProduct_ReturnsProduct() throws Exception {
        Product product = Product.builder().id(12L).title("Strawberries").build();
        when(productService.getProductById(12L)).thenReturn(product);



        mockMvc.perform(get("/api/v1/products/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(12));
    }

    @Test
    void updateProduct_ReturnsUpdatedProduct() throws Exception {
        when(authenticationService.getSellerIdFromAuthenticationContext()).thenReturn(7L);
        Product saved = Product.builder().id(12L).title("Updated").build();

        when(productService.updateProduct(eq(7L), eq(12L), any(Product.class))).thenReturn(saved);


        mockMvc.perform(put("/api/v1/products/12")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProduct))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated"));
    }

    @Test
    void patchProduct_ReturnsPatchedProduct() throws Exception {
        Product saved = Product.builder().id(12L).title("Patched").build();

        when(productService.patchProduct(eq(12L), any(Product.class))).thenReturn(saved);

        mockMvc.perform(patch("/api/v1/products/12")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProduct))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Patched"));
    }

    @Test
    void deleteProduct_ReturnsNoContent() throws Exception {
        when(authenticationService.getSellerIdFromAuthenticationContext()).thenReturn(7L);

        mockMvc.perform(delete("/api/v1/products/12"))
                .andExpect(status().isNoContent())
                .andExpect(content().string("Product Deleted"));

        verify(productService).deleteProduct(7L, 12L);
    }
}
