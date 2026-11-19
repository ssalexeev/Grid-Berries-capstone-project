package org.capstone.service;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.capstone.domain.SellerProfile;
import org.capstone.domain.User;
import org.capstone.domain.enums.Role;
import org.capstone.repository.jpa.SellerProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SellerProfileServiceTest {

    @Mock
    private SellerProfileRepository sellerRepository;

    @InjectMocks
    private SellerProfileService sellerProfileService;

    @Test
    void createProfileRequiresAUserWithAnId() {
        assertThrows(IllegalArgumentException.class,
                () -> sellerProfileService.createProfile(null, new SellerProfile()));
        assertThrows(IllegalArgumentException.class,
                () -> sellerProfileService.createProfile(
                        User.builder().role(Role.SELLER).build(), new SellerProfile()));
        verifyNoInteractions(sellerRepository);
    }

    @Test
    void createProfileRejectsNonSellerAndInactiveUsers() {
        User buyer = User.builder().id(1L).role(Role.BUYER).isActive(true).build();
        User inactiveSeller = User.builder().id(2L).role(Role.SELLER).isActive(false).build();

        assertThrows(IllegalStateException.class,
                () -> sellerProfileService.createProfile(buyer, new SellerProfile()));
        assertThrows(IllegalStateException.class,
                () -> sellerProfileService.createProfile(inactiveSeller, new SellerProfile()));
        verifyNoInteractions(sellerRepository);
    }

    @Test
    void createProfileRejectsDuplicateProfiles() {
        User seller = seller();
        when(sellerRepository.existsSellerProfileByUser_Id(1L)).thenReturn(true);

        assertThrows(EntityExistsException.class,
                () -> sellerProfileService.createProfile(seller, new SellerProfile()));
        verify(sellerRepository).existsSellerProfileByUser_Id(1L);
        verify(sellerRepository, never()).save(any(SellerProfile.class));
    }

    @Test
    void createProfileAssociatesAndSavesTheSeller() {
        User seller = seller();
        SellerProfile profile = SellerProfile.builder()
                .storeName("Tech Store")
                .storeDescription("Best tech")
                .contactInfo("tech@store.com")
                .build();
        when(sellerRepository.existsSellerProfileByUser_Id(1L)).thenReturn(false);
        when(sellerRepository.save(profile)).thenReturn(profile);

        SellerProfile created = sellerProfileService.createProfile(seller, profile);

        assertEquals(seller, created.getUser());
        assertEquals("Tech Store", created.getStoreName());
        verify(sellerRepository).save(profile);
    }

    @Test
    void getProfileByIdThrowsWhenMissing() {
        when(sellerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> sellerProfileService.getProfileById(99L));
    }

    @Test
    void getProfileByUserIdRequiresAnIdAndFindsTheProfile() {
        assertThrows(IllegalArgumentException.class, () -> sellerProfileService.getProfileByUserId(null));
        verifyNoInteractions(sellerRepository);

        SellerProfile profile = SellerProfile.builder().id(8L).user(seller()).build();
        when(sellerRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

        assertEquals(profile, sellerProfileService.getProfileByUserId(1L));
    }

    @Test
    void updateProfileChangesProvidedFieldsAndPreservesNullFields() {
        SellerProfile existing = SellerProfile.builder()
                .id(10L)
                .storeName("Old Store")
                .storeDescription("Old description")
                .contactInfo("old@contact.com")
                .build();
        SellerProfile changes = SellerProfile.builder()
                .storeName("New Store")
                .contactInfo("new@contact.com")
                .build();
        when(sellerRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(sellerRepository.save(existing)).thenReturn(existing);

        SellerProfile updated = sellerProfileService.updateProfile(10L, changes);

        assertEquals("New Store", updated.getStoreName());
        assertEquals("Old description", updated.getStoreDescription());
        assertEquals("new@contact.com", updated.getContactInfo());
        verify(sellerRepository).save(existing);
    }

    @Test
    void deleteProfileDeletesTheLoadedProfile() {
        SellerProfile profile = SellerProfile.builder().id(10L).build();
        when(sellerRepository.findById(10L)).thenReturn(Optional.of(profile));

        sellerProfileService.deleteProfile(10L);

        verify(sellerRepository).delete(profile);
    }

    private User seller() {
        return User.builder().id(1L).role(Role.SELLER).isActive(true).build();
    }
}
