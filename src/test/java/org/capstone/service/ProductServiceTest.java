package org.capstone.service;

import org.capstone.domain.Category;
import org.capstone.domain.Product;
import org.capstone.domain.SellerProfile;
import org.capstone.domain.User;
import org.capstone.domain.enums.ProductStatus;
import org.capstone.domain.enums.ProductType;
import org.capstone.domain.enums.Role;
import org.capstone.repository.jdbc.CategoryRepository;
import org.capstone.repository.jdbc.ProductRepository;
import org.capstone.repository.jdbc.SellerProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock
    private ProductRepository productRepository;
    @Mock
    private SellerProfileRepository sellerProfileRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserMetricEventService metricEventService;

    @InjectMocks
    private ProductService productService;

    @Test
    void createProductSetsPendingModerationAndPersistsCategories() {
        Category category = Category.builder().id(2L).name("Fruit").build();
        Product product = validProduct();
        product.setCategory(category);
        SellerProfile seller = sellerProfile();
        when(sellerProfileRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product created = productService.createProduct(1L, product);

        assertEquals(ProductStatus.PENDING_MODERATION, created.getStatus());
        assertEquals(seller, created.getSellerProfile());
        assertEquals(List.of(category), created.getCategories());
        verify(productRepository).save(product);
    }

    @Test
    void searchProductsAppliesKeywordCategoryPriceAndAvailabilityFilters() {
        Category category = Category.builder().id(2L).name("Fruit").build();
        Product match = validProduct();
        match.setId(3L);
        match.setTitle("Fresh strawberries");
        match.setStatus(ProductStatus.ACTIVE);
        match.setCategory(category);
        Product suspended = validProduct();
        suspended.setId(4L);
        suspended.setStatus(ProductStatus.SUSPENDED);
        Product outOfRange = validProduct();
        outOfRange.setId(5L);
        outOfRange.setStatus(ProductStatus.ACTIVE);
        outOfRange.setPrice(new BigDecimal("25.00"));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));
        when(productRepository.findAll()).thenReturn(List.of(match, suspended, outOfRange));

        List<Product> products = productService.searchProducts(
                "STRAWBERR", 2L, new BigDecimal("4.00"), new BigDecimal("10.00"), true, null);

        assertEquals(List.of(match), products);
    }

    @Test
    void updateProductRejectsListingsOwnedByAnotherSeller() {
        Product existing = validProduct();
        existing.setId(5L);
        existing.setSellerProfile(sellerProfile());
        when(productRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(sellerProfileRepository.findById(9L))
                .thenReturn(Optional.of(sellerProfile(9L)));

        assertThrows(IllegalStateException.class, () -> productService.updateProduct(9L, validProductWithId(5L)));
    }

    private Product validProduct() {
        return Product.builder()
                .title("Strawberries")
                .price(new BigDecimal("5.00"))
                .stockQuantity(10)
                .type(ProductType.PHYSICAL_GOOD)
                .build();
    }

    private Product validProductWithId(Long id) {
        Product product = validProduct();
        product.setId(id);
        return product;
    }

    private SellerProfile sellerProfile() {
        return sellerProfile(1L);
    }

    private SellerProfile sellerProfile(Long id) {
        return SellerProfile.builder()
                .id(id)
                .user(User.builder().id(id).role(Role.SELLER).isActive(true).build())
                .build();
    }
}
