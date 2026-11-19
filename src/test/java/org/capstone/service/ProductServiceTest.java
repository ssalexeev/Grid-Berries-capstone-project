package org.capstone.service;

import org.capstone.domain.Category;
import org.capstone.domain.Product;
import org.capstone.domain.SellerProfile;
import org.capstone.domain.User;
import org.capstone.domain.enums.ProductStatus;
import org.capstone.domain.enums.Role;
import org.capstone.repository.jpa.CategoryRepository;
import org.capstone.repository.jpa.ProductRepository;
import org.capstone.service.search.ProductSearchCriteria;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock
    private ProductRepository productRepository;
    @Mock
    private SellerProfileService sellerProfileService;
    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void createProductSetsPendingModerationAndPersistsCategories() {
        Category category = Category.builder().id(2L).name("Fruit").build();
        Product product = validProduct();
        product.setCategories(Set.of(category));
        SellerProfile seller = sellerProfile();
        when(sellerProfileService.getProfileById(1L)).thenReturn(seller);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product created = productService.createProduct(1L, product);

        assertEquals(ProductStatus.PENDING_MODERATION, created.getStatus());
        assertEquals(seller, created.getSellerProfile());
        assertEquals(Set.of(category), created.getCategories());
        verify(productRepository).save(product);
    }

    @Test
    void getProductsSliceDelegatesSearchToRepository() {
        Product match = validProduct();
        match.setId(3L);
        match.setTitle("Fresh strawberries");
        match.setStatus(ProductStatus.ACTIVE);
        PageRequest pageable = PageRequest.of(0, 10);
        Slice<Product> expected = new SliceImpl<>(List.of(match), pageable, false);
        doReturn(expected).when(productRepository).findBy(any(Specification.class), any());

        Slice<Product> actual = productService.getProductsSlice(
                ProductSearchCriteria.builder()
                        .query("strawberry")
                        .categoryIds(List.of(2L))
                        .minPrice(new BigDecimal("4.00"))
                        .maxPrice(new BigDecimal("10.00"))
                        .build(),
                pageable);

        assertEquals(expected, actual);
    }

    @Test
    void updateProductRejectsListingsOwnedByAnotherSeller() {
        Product existing = validProduct();
        existing.setId(5L);
        existing.setSellerProfile(sellerProfile());
        when(productRepository.findById(5L)).thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class, () -> productService.updateProduct(9L, 5L, validProductWithId(5L)));
    }

    private Product validProduct() {
        return Product.builder()
                .title("Strawberries")
                .price(new BigDecimal("5.00"))
                .stockQuantity(10)
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
