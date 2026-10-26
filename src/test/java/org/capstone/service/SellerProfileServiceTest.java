package org.capstone.service;

import org.capstone.domain.SellerProfile;
import org.capstone.domain.User;
import org.capstone.domain.enums.Role;
import org.capstone.repository.jdbc.SellerProfileRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SellerProfileServiceTest {

    @Mock
    private SellerProfileRepository sellerRepository;

    @InjectMocks
    private SellerProfileService sellerProfileService;

    @Nested
    class CreateProfileTests {

        @Test
        void createProfile_WhenUserIsNull_ThrowsIllegalArgumentException() {
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> sellerProfileService.createProfile(null, "My Store", "Desc", "contact@store.com")
            );

            assertEquals("SellerProfile must be associated with a valid User.", ex.getMessage());
            verifyNoInteractions(sellerRepository);
        }

        @Test
        void createProfile_WhenUserIdIsNull_ThrowsIllegalArgumentException() {
            User userWithoutId = User.builder().role(Role.SELLER).isActive(true).build();

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> sellerProfileService.createProfile(userWithoutId, "My Store", "Desc", "contact@store.com")
            );

            assertEquals("SellerProfile must be associated with a valid User.", ex.getMessage());
            verifyNoInteractions(sellerRepository);
        }

        @Test
        void createProfile_WhenUserIsNotSellerRole_ThrowsIllegalStateException() {
            User buyerUser = User.builder().id(1L).role(Role.BUYER).isActive(true).build();

            IllegalStateException ex = assertThrows(
                    IllegalStateException.class,
                    () -> sellerProfileService.createProfile(buyerUser, "My Store", "Desc", "contact@store.com")
            );

            assertEquals("Only users with SELLER role can create a seller profile.", ex.getMessage());
            verifyNoInteractions(sellerRepository);
        }

        @Test
        void createProfile_WhenUserIsInactive_ThrowsIllegalStateException() {
            User inactiveSeller = User.builder().id(1L).role(Role.SELLER).isActive(false).build();

            IllegalStateException ex = assertThrows(
                    IllegalStateException.class,
                    () -> sellerProfileService.createProfile(inactiveSeller, "My Store", "Desc", "contact@store.com")
            );

            assertEquals("Inactive users cannot create a seller profile.", ex.getMessage());
            verifyNoInteractions(sellerRepository);
        }

      @Test
        void createProfile_WhenStoreNameIsNullOrEmptyOrBlank_ThrowsIllegalArgumentException() {
            String invalidStoreName = "";
            User validSeller = User.builder().id(1L).role(Role.SELLER).isActive(true).build();

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> sellerProfileService.createProfile(validSeller, invalidStoreName, "Desc", "contact@store.com")
            );

            assertEquals("Store name cannot be empty.", ex.getMessage());
            verifyNoInteractions(sellerRepository);
        }

        @Test
        void createProfile_WhenProfileAlreadyExistsForUser_ThrowsIllegalStateException() {
            User validSeller = User.builder()
                    .id(1L)
                    .role(Role.SELLER)
                    .isActive(true)
                    .build();

            SellerProfile existingProfile = SellerProfile.builder()
                    .id(100L)
                    .user(validSeller)
                    .build();

            when(sellerRepository.findByUserId(1L)).thenReturn(Optional.of(existingProfile));

            IllegalStateException ex = assertThrows(
                    IllegalStateException.class,
                    () -> sellerProfileService.createProfile(validSeller, "My Store", "Desc", "contact@store.com")
            );

            assertEquals("Seller profile already exists for user ID: 1", ex.getMessage());
            verify(sellerRepository, times(1)).findByUserId(1L);
            verify(sellerRepository, never()).save(any());
        }

        @Test
        void createProfile_WhenValidInputs_SavesAndReturnsProfile() {
            User validSeller = User.builder()
                    .id(1L)
                    .role(Role.SELLER)
                    .isActive(true)
                    .build();

            when(sellerRepository.findByUserId(1L)).thenReturn(Optional.empty());
            when(sellerRepository.save(any(SellerProfile.class))).thenAnswer(inv -> inv.getArgument(0));

            SellerProfile result = sellerProfileService.createProfile(validSeller, "Tech Store", "Best tech", "tech@store.com");

            assertNotNull(result);
            assertEquals("Tech Store", result.getStoreName());
            assertEquals("Best tech", result.getStoreDescription());
            assertEquals("tech@store.com", result.getContactInfo());
            assertEquals(validSeller, result.getUser());

            verify(sellerRepository, times(1)).findByUserId(1L);
            verify(sellerRepository, times(1)).save(any(SellerProfile.class));
        }
    }

    @Nested
    class ReadProfileTests {

        @Test
        void getProfileById_WhenFound_ReturnsProfile() {
            Long profileId = 100L;
            SellerProfile profile = SellerProfile.builder().id(profileId).storeName("Store 100").build();

            when(sellerRepository.findById(profileId)).thenReturn(Optional.of(profile));

            SellerProfile result = sellerProfileService.getProfileById(profileId);

            assertEquals(profileId, result.getId());
            assertEquals("Store 100", result.getStoreName());
        }

        @Test
        void getProfileById_WhenNotFound_ThrowsIllegalArgumentException() {
            when(sellerRepository.findById(999L)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> sellerProfileService.getProfileById(999L)
            );

            assertEquals("Seller profile not found with ID: 999", ex.getMessage());
        }

        @Test
        void getProfileByUserId_WhenUserIdIsNull_ThrowsIllegalArgumentException() {
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> sellerProfileService.getProfileByUserId(null)
            );

            assertEquals("User ID cannot be null.", ex.getMessage());
            verifyNoInteractions(sellerRepository);
        }

        @Test
        void getProfileByUserId_WhenFound_ReturnsProfile() {
            Long userId = 5L;
            SellerProfile profile = SellerProfile.builder()
                    .id(200L)
                    .user(User.builder().id(userId).build())
                    .build();

            when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

            SellerProfile result = sellerProfileService.getProfileByUserId(userId);

            assertEquals(200L, result.getId());
            verify(sellerRepository, times(1)).findByUserId(userId);
        }

        @Test
        void getProfileByUserId_WhenNotFound_ThrowsIllegalArgumentException() {
            when(sellerRepository.findByUserId(88L)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> sellerProfileService.getProfileByUserId(88L)
            );

            assertEquals("Seller profile not found for user ID: 88", ex.getMessage());
        }

        @Test
        void getAllProfiles_ReturnsAllFromRepository() {
            when(sellerRepository.findAll()).thenReturn(List.of(new SellerProfile(), new SellerProfile()));

            List<SellerProfile> results = sellerProfileService.getAllProfiles();

            assertEquals(2, results.size());
            verify(sellerRepository, times(1)).findAll();
        }
    }

    @Nested
    class UpdateProfileTests {

        @Test
        void updateProfile_WhenAllFieldsValid_UpdatesAllFields() {
            Long profileId = 10L;
            SellerProfile existingProfile = SellerProfile.builder()
                    .id(profileId)
                    .storeName("Old Store")
                    .storeDescription("Old Desc")
                    .contactInfo("old@contact.com")
                    .build();

            when(sellerRepository.findById(profileId)).thenReturn(Optional.of(existingProfile));
            when(sellerRepository.save(existingProfile)).thenReturn(existingProfile);

            SellerProfile result = sellerProfileService.updateProfile(profileId, "New Store", "New Desc", "new@contact.com");

            assertEquals("New Store", result.getStoreName());
            assertEquals("New Desc", result.getStoreDescription());
            assertEquals("new@contact.com", result.getContactInfo());
            verify(sellerRepository, times(1)).save(existingProfile);
        }

        @Test
        void updateProfile_WhenStoreNameIsNullPropertyPreservesOldValue() {
            Long profileId = 10L;
            SellerProfile existingProfile = SellerProfile.builder()
                    .id(profileId)
                    .storeName("Keep Old Store")
                    .storeDescription("Old Desc")
                    .contactInfo("old@contact.com")
                    .build();

            when(sellerRepository.findById(profileId)).thenReturn(Optional.of(existingProfile));
            when(sellerRepository.save(existingProfile)).thenReturn(existingProfile);

            // Pass blank storeName, non-null desc/contact
            SellerProfile result = sellerProfileService.updateProfile(profileId, "   ", "New Desc", "new@contact.com");

            assertEquals("Keep Old Store", result.getStoreName()); // Preserved
            assertEquals("New Desc", result.getStoreDescription());
            assertEquals("new@contact.com", result.getContactInfo());
        }

        @Test
        void updateProfile_WhenProfileDoesNotExist_ThrowsIllegalArgumentException() {
            when(sellerRepository.findById(500L)).thenReturn(Optional.empty());

            assertThrows(
                    IllegalArgumentException.class,
                    () -> sellerProfileService.updateProfile(500L, "Store", "Desc", "Contact")
            );

            verify(sellerRepository, never()).save(any());
        }
    }

    @Nested
    class DeleteProfileTests {

        @Test
        void deleteProfile_WhenProfileExists_DeletesFromRepository() {
            Long profileId = 10L;
            SellerProfile existingProfile = SellerProfile.builder().id(profileId).build();

            when(sellerRepository.findById(profileId)).thenReturn(Optional.of(existingProfile));

            sellerProfileService.deleteProfile(profileId);

            verify(sellerRepository, times(1)).findById(profileId);
            verify(sellerRepository, times(1)).delete(existingProfile);
        }

        @Test
        void deleteProfile_WhenProfileDoesNotExist_ThrowsIllegalArgumentException() {
            when(sellerRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(
                    IllegalArgumentException.class,
                    () -> sellerProfileService.deleteProfile(999L)
            );

            verify(sellerRepository, never()).delete(any());
        }
    }
}
