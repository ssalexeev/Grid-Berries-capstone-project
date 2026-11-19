package org.capstone.controller;

import org.capstone.controller.admin.AdminController;
import org.capstone.controller.exception.handler.GlobalExceptionHandler;
import org.capstone.domain.Category;
import org.capstone.domain.Product;
import org.capstone.domain.User;
import org.capstone.domain.enums.Role;
import org.capstone.security.AuthenticationService;
import org.capstone.service.ProductService;
import org.capstone.service.UserService;
import org.capstone.util.mapper.product.ProductMapperImpl;
import org.capstone.util.mapper.user.PasswordMapper;
import org.capstone.util.mapper.user.UserMapperImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        UserMapperImpl.class,
        ProductMapperImpl.class,
        PasswordMapper.class,
        BCryptPasswordEncoder.class,
        GlobalExceptionHandler.class
})
public class AdminControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    UserService userService;

    @MockitoBean
    ProductService productService;

    @MockitoBean
    AuthenticationService authenticationService;


    @Test
    void getUser_MapsAndReturnsUser() throws Exception {
        User user = User.builder()
                .email("user@example.com")
                .role(Role.BUYER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(userService.getUserByEmail("user@example.com")).thenReturn(user);

        mockMvc.perform(get("/api/v1/admin")
                        .param("userEmail", user.getEmail())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.role").value("BUYER"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void patch_ReturnsModeratedUser() throws Exception {
        Long userId = 42L;
        LocalDateTime createdAt = LocalDateTime.of(2026, 11, 18, 11, 30, 0);

        User patchedUser = User.builder()
                .id(userId)
                .email("user@example.com")
                .role(Role.SELLER)
                .isActive(false)
                .createdAt(createdAt)
                .build();

        when(userService.patchUser(eq(userId), any(User.class))).thenReturn(patchedUser);

        mockMvc.perform(patch("/api/v1/admin/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "role": "SELLER",
                              "isActive": false
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.role").value("SELLER"))
                .andExpect(jsonPath("$.isActive").value(false))
                .andExpect(jsonPath("$.createdAt").exists());

        verify(userService).patchUser(eq(userId), argThat(mappedUser ->
                mappedUser.getRole() == Role.SELLER &&
                        Boolean.FALSE.equals(mappedUser.getIsActive())
        ));
    }


    @Test
    void patch_ReturnsModeratedProduct() throws Exception {
        Long productId = 2L;

        Product patchedProduct = Product.builder()
                .id(productId)
                .title("test title")
                .description("changed description")
                .price(BigDecimal.ONE)
                .stockQuantity(5)
                .categories(Set.of(Category.builder().id(1L).build()))
                .build();

        when(productService.patchProduct(eq(productId), any(Product.class)))
                .thenReturn(patchedProduct);

        mockMvc.perform(patch("/api/v1/admin/product/{productId}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "title": "test title"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.title").value("test title"))
                .andExpect(jsonPath("$.description").value("changed description"))
                .andExpect(jsonPath("$.price").value(1))
                .andExpect(jsonPath("$.stockQuantity").value(5));

        verify(productService).patchProduct(eq(productId), any(Product.class));
    }


}