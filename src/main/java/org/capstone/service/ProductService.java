package org.capstone.service;

import lombok.RequiredArgsConstructor;
import org.capstone.domain.Category;
import org.capstone.domain.Product;
import org.capstone.domain.SellerProfile;
import org.capstone.domain.enums.MetricEventType;
import org.capstone.domain.enums.ProductStatus;
import org.capstone.domain.enums.Role;
import org.capstone.repository.jdbc.CategoryRepository;
import org.capstone.repository.jdbc.ProductRepository;
import org.capstone.repository.jdbc.SellerProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final CategoryRepository categoryRepository;
    private final UserMetricEventService metricEventService;

    @Transactional
    public Product createProduct(Long sellerProfileId, Product product) {
        validateProduct(product);
        SellerProfile seller = getSellerProfile(sellerProfileId);

        product.setId(null);
        product.setSellerProfile(seller);
        product.setStatus(ProductStatus.PENDING_MODERATION);
        List<Category> categories = getCategories(product);
        product.setCategories(categories);
        product.setCategory(categories.isEmpty() ? null : categories.get(0));

        return productRepository.save(product);
    }

    @Transactional
    public Product updateProduct(Long sellerProfileId, Product changes) {
        if (changes == null || changes.getId() == null) {
            throw new IllegalArgumentException("Product and product ID are required.");
        }
        Product existing = getProductById(changes.getId());
        requireSellerOwnership(existing, sellerProfileId);
        validateProduct(changes);

        existing.setTitle(changes.getTitle());
        existing.setDescription(changes.getDescription());
        existing.setPrice(changes.getPrice());
        existing.setStockQuantity(changes.getStockQuantity());
        existing.setImageUrl(changes.getImageUrl());
        existing.setType(changes.getType());
        List<Category> categories = getCategories(changes);
        existing.setCategories(categories);
        existing.setCategory(categories.isEmpty() ? null : categories.get(0));

        return productRepository.save(existing);
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
        metricEventService.recordEvent(MetricEventType.VIEW, userId, productId, null, null);
        return product;
    }

    public List<Product> getProductsBySeller(Long sellerProfileId) {
        getSellerProfile(sellerProfileId);
        return productRepository.findBySellerId(sellerProfileId);
    }

    public List<Product> searchProducts(
            String keyword,
            Long categoryId,
            BigDecimal minimumPrice,
            BigDecimal maximumPrice,
            boolean availableOnly,
            Long userId) {
        validatePriceRange(minimumPrice, maximumPrice);
        if (categoryId != null && categoryRepository.findById(categoryId).isEmpty()) {
            throw new IllegalArgumentException("Category not found with ID: " + categoryId);
        }
        if (userId != null) {
            metricEventService.recordEvent(MetricEventType.SEARCH, userId, null, keyword, null);
        }

        String normalizedKeyword = keyword == null ? null : keyword.trim().toLowerCase();
        return productRepository.findAll().stream()
                .filter(product -> product.getStatus() == ProductStatus.ACTIVE)
                .filter(product -> matchesKeyword(product, normalizedKeyword))
                .filter(product -> matchesCategory(product, categoryId))
                .filter(product -> minimumPrice == null || product.getPrice().compareTo(minimumPrice) >= 0)
                .filter(product -> maximumPrice == null || product.getPrice().compareTo(maximumPrice) <= 0)
                .filter(product -> !availableOnly
                        || product.getStockQuantity() != null && product.getStockQuantity() > 0)
                .sorted(Comparator.comparing(Product::getId))
                .toList();
    }

    public List<Product> searchProducts(
            String keyword,
            Long categoryId,
            BigDecimal minimumPrice,
            BigDecimal maximumPrice,
            boolean availableOnly) {
        return searchProducts(keyword, categoryId, minimumPrice, maximumPrice, availableOnly, null);
    }

    @Transactional
    public void deleteProduct(Long sellerProfileId, Long productId) {
        Product product = getProductById(productId);
        requireSellerOwnership(product, sellerProfileId);
        productRepository.delete(product);
    }

    @Transactional
    public Product moderateProduct(Long productId, ProductStatus status) {
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
        for (Product product : productRepository.findBySellerId(sellerProfileId)) {
            product.setStatus(ProductStatus.SUSPENDED);
            productRepository.save(product);
        }
    }

    private void validateProduct(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null.");
        }
        if (product.getTitle() == null || product.getTitle().isBlank()) {
            throw new IllegalArgumentException("Product title cannot be empty.");
        }
        if (product.getPrice() == null || product.getPrice().signum() < 0) {
            throw new IllegalArgumentException("Product price must be zero or greater.");
        }
        if (product.getStockQuantity() == null || product.getStockQuantity() < 0) {
            throw new IllegalArgumentException("Product stock cannot be negative or null.");
        }
        if (product.getType() == null) {
            throw new IllegalArgumentException("Product type is required.");
        }
    }

    private SellerProfile getSellerProfile(Long sellerProfileId) {
        if (sellerProfileId == null) {
            throw new IllegalArgumentException("Seller profile ID cannot be null.");
        }
        SellerProfile seller = sellerProfileRepository.findById(sellerProfileId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Seller profile not found with ID: " + sellerProfileId));
        if (seller.getUser() == null
                || seller.getUser().getRole() != Role.SELLER
                || !Boolean.TRUE.equals(seller.getUser().getIsActive())) {
            throw new IllegalStateException("Only active seller accounts can manage products.");
        }
        return seller;
    }

    private Category getCategory(Category category) {
        if (category == null) {
            return null;
        }
        if (category.getId() == null) {
            throw new IllegalArgumentException("Category ID is required.");
        }
        return categoryRepository.findById(category.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Category not found with ID: " + category.getId()));
    }

    private List<Category> getCategories(Product product) {
        List<Category> requested = product.getCategories();
        if (requested == null || requested.isEmpty()) {
            return product.getCategory() == null ? List.of() : List.of(getCategory(product.getCategory()));
        }
        return requested.stream()
                .map(this::getCategory)
                .distinct()
                .toList();
    }

    private void requireSellerOwnership(Product product, Long sellerProfileId) {
        getSellerProfile(sellerProfileId);
        if (product.getSellerProfile() == null
                || !sellerProfileId.equals(product.getSellerProfile().getId())) {
            throw new IllegalStateException("Product does not belong to this seller.");
        }
    }

    private void requireActive(Product product) {
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new IllegalStateException("Product is not available in the catalog.");
        }
    }

    private void validatePriceRange(BigDecimal minimumPrice, BigDecimal maximumPrice) {
        if (minimumPrice != null && minimumPrice.signum() < 0
                || maximumPrice != null && maximumPrice.signum() < 0) {
            throw new IllegalArgumentException("Price filters cannot be negative.");
        }
        if (minimumPrice != null && maximumPrice != null && minimumPrice.compareTo(maximumPrice) > 0) {
            throw new IllegalArgumentException("Minimum price cannot exceed maximum price.");
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
        return product.getCategory() != null && categoryId.equals(product.getCategory().getId());
    }
}
