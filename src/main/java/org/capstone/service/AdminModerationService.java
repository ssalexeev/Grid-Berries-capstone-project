package org.capstone.service;

import lombok.RequiredArgsConstructor;
import org.capstone.domain.Product;
import org.capstone.domain.User;
import org.capstone.domain.enums.ProductStatus;
import org.capstone.domain.enums.Role;
import org.capstone.repository.jdbc.SellerProfileRepository;
import org.capstone.repository.jdbc.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminModerationService {
    private final UserRepository userRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final ProductService productService;

    @Transactional
    public User suspendSeller(Long userId) {
        User user = getSeller(userId);
        user.setIsActive(false);
        User suspendedUser = userRepository.save(user);
        sellerProfileRepository.findByUserId(userId)
                .ifPresent(profile -> productService.suspendProductsForSeller(profile.getId()));
        return suspendedUser;
    }

    @Transactional
    public User reinstateSeller(Long userId) {
        User user = getSeller(userId);
        user.setIsActive(true);
        return userRepository.save(user);
    }

    public Product approveProduct(Long productId) {
        return productService.moderateProduct(productId, ProductStatus.ACTIVE);
    }

    public Product suspendProduct(Long productId) {
        return productService.moderateProduct(productId, ProductStatus.SUSPENDED);
    }

    private User getSeller(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        if (user.getRole() != Role.SELLER) {
            throw new IllegalStateException("Only seller accounts can be moderated.");
        }
        return user;
    }
}
