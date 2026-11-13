package org.capstone.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.capstone.domain.Category;
import org.capstone.domain.Product;
import org.capstone.domain.SellerProfile;
import org.capstone.domain.enums.ProductStatus;
import org.capstone.service.search.ProductSearchCriteria;
import org.capstone.repository.jpa.CategoryRepository;
import org.capstone.repository.jpa.ProductRepository;
import org.capstone.service.search.ProductSpecifications;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SellerProfileService sellerProfileService;


    public Slice<Product> getProductsSlice(ProductSearchCriteria criteria, Pageable pageable) {
        Specification<Product> specification = ProductSpecifications.withFilters(criteria);

        return productRepository.findBy(specification, q -> q
                .as(Product.class)
                .sortBy(pageable.getSort())
                .slice(pageable)
        );
    }

    @Transactional
    public Product createProduct(Long sellerProfileId, Product product) {
        SellerProfile seller = sellerProfileService.getProfileById(sellerProfileId);
        product.setSellerProfile(seller);

        return productRepository.save(product);
    }

    @Transactional
    public Product updateProduct(Long sellerProfileId, Long productId, Product changes) {
        if (changes == null || changes.getId() == null) {
            throw new IllegalArgumentException("Product and product ID are required.");
        }
        Product existing = getProductById(productId);
        requireSellerOwnership(existing, sellerProfileId);

        existing.setTitle(changes.getTitle());
        existing.setDescription(changes.getDescription());
        existing.setPrice(changes.getPrice());
        existing.setStockQuantity(changes.getStockQuantity());
        existing.setImageUrl(changes.getImageUrl());
        existing.setCategories(changes.getCategories());

        return productRepository.save(existing);
    }

    @Transactional
    public Product patchProduct(Long sellerProfileId, Long productId, Product patchProduct, boolean isAdmin) {
        Product existing = getProductById(productId);

        if (!isAdmin) {
            requireSellerOwnership(existing, sellerProfileId);
        }

        if (patchProduct.getTitle() != null) {
            existing.setTitle(patchProduct.getTitle());
        }
        if (patchProduct.getPrice() != null) {
            existing.setPrice(patchProduct.getPrice());
        }
        if (patchProduct.getStockQuantity() != null) {
            existing.setStockQuantity(patchProduct.getStockQuantity());
        }
        if (patchProduct.getStatus() != null) {
            existing.setStatus(patchProduct.getStatus());
        }
        if (patchProduct.getImageUrl() != null) {
            existing.setImageUrl(patchProduct.getImageUrl());
        }

        if (patchProduct.getCategories() != null) {
            syncCategories(existing, patchProduct.getCategories());
        }

        return productRepository.save(existing);
    }

    private void syncCategories(Product existing, Set<Category> incomingCategories) {
        Set<Long> targetIds = incomingCategories.stream()
                .map(Category::getId)
                .collect(Collectors.toSet());

        List<Category> foundCategories = categoryRepository.findAllById(targetIds);
        if (foundCategories.size() != targetIds.size()) {
            throw new EntityNotFoundException("One or more category IDs are invalid");
        }

        Set<Category> targetCategories = new HashSet<>(foundCategories);

        for (Category current : new HashSet<>(existing.getCategories())) {
            if (!targetCategories.contains(current)) {
                existing.removeCategory(current);
            }
        }

        for (Category target : targetCategories) {
            if (!existing.getCategories().contains(target)) {
                existing.addCategory(target);
            }
        }

    }

    public Product getProductById(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID cannot be null.");
        }

        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));
    }

    public Product viewProduct(Long productId, Long userId) {
        Product product = getProductById(productId);
        requireActive(product);
        return product;
    }

    public List<Product> getProductsBySeller(Long sellerProfileId) {
        return productRepository.findAllBySellerProfileId(sellerProfileId);
    }


    @Transactional
    public void deleteProduct(Long sellerProfileId, Long productId) {
        Product product = getProductById(productId);
        requireSellerOwnership(product, sellerProfileId);
        productRepository.delete(product);
    }

    @Transactional
    public Product moderateProductStatus(Long productId, ProductStatus status) {
        if (status != ProductStatus.ACTIVE && status != ProductStatus.SUSPENDED) {
            throw new IllegalArgumentException("Moderated products must be approved or suspended.");
        }
        Product product = getProductById(productId);
        product.setStatus(status);
        return productRepository.save(product);
    }

    @Transactional
    public void suspendProductsForSeller(Long sellerProfileId) {
        if (sellerProfileId == null) {
            throw new IllegalArgumentException("Seller profile ID cannot be null.");
        }
        for (Product product : productRepository.findAllBySellerProfileId(sellerProfileId)) {
            product.setStatus(ProductStatus.SUSPENDED);
            productRepository.save(product);
        }
    }

    private void requireSellerOwnership(Product product, Long sellerProfileId) {
        if (product.getSellerProfile() == null || !sellerProfileId.equals(product.getSellerProfile().getId())) {
            throw new IllegalStateException("Product does not belong to this seller.");
        }
    }

    private void requireActive(Product product) {
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new IllegalStateException("Product is not available in the catalog.");
        }
    }

    private boolean matchesKeyword(Product product, String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return true;
        }
        return contains(product.getTitle(), keyword) || contains(product.getDescription(), keyword);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private boolean matchesCategory(Product product, Long categoryId) {
        if (categoryId == null) {
            return true;
        }
        if (product.getCategories() != null
                && product.getCategories().stream()
                .anyMatch(category -> categoryId.equals(category.getId()))) {
            return true;
        }
        return false;
    }


}
