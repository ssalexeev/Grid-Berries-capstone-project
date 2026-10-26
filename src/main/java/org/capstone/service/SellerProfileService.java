package org.capstone.service;

import org.capstone.domain.SellerProfile;
import org.capstone.domain.User;
import org.capstone.domain.enums.Role;
import org.capstone.repository.jdbc.SellerProfileRepository;

import java.util.List;

public class SellerProfileService {

    private final SellerProfileRepository sellerRepository;

    public SellerProfileService(SellerProfileRepository sellerRepository) {
        this.sellerRepository = sellerRepository;
    }

    public SellerProfile createProfile(User user, String storeName, String description, String contactInfo) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("SellerProfile must be associated with a valid User.");
        }
        if (user.getRole() != Role.SELLER) {
            throw new IllegalStateException("Only users with SELLER role can create a seller profile.");
        }
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new IllegalStateException("Inactive users cannot create a seller profile.");
        }
        if (storeName == null || storeName.isBlank()) {
            throw new IllegalArgumentException("Store name cannot be empty.");
        }

        if (sellerRepository.findByUserId(user.getId()).isPresent()) {
            throw new IllegalStateException("Seller profile already exists for user ID: " + user.getId());
        }

        SellerProfile sellerProfile = SellerProfile.builder()
                .storeName(storeName)
                .storeDescription(description)
                .contactInfo(contactInfo)
                .user(user)
                .build();

        return sellerRepository.save(sellerProfile);
    }

    public SellerProfile getProfileById(Long id) {
        return sellerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Seller profile not found with ID: " + id));
    }

    public List<SellerProfile> getAllProfiles() {
        return sellerRepository.findAll();
    }

    public SellerProfile getProfileByUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null.");
        }

        return sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Seller profile not found for user ID: " + userId));
    }

    public SellerProfile updateProfile(Long profileId, String storeName, String description, String contactInfo) {
        SellerProfile existingProfile = getProfileById(profileId);

        if (storeName != null && !storeName.isBlank()) {
            existingProfile.setStoreName(storeName);
        }
        if (description != null) {
            existingProfile.setStoreDescription(description);
        }
        if (contactInfo != null) {
            existingProfile.setContactInfo(contactInfo);
        }

        return sellerRepository.save(existingProfile);
    }

    public void deleteProfile(Long id) {
        SellerProfile profile = getProfileById(id);
        sellerRepository.delete(profile);
    }
}