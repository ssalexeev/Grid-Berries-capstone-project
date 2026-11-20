package org.capstone.controller;

import org.capstone.controller.seller.SellerProfileController;
import org.capstone.domain.SellerProfile;
import org.capstone.domain.User;
import org.capstone.dto.request.SellerProfileCreateRequest;
import org.capstone.dto.response.SellerProfileResponse;
import org.capstone.security.AuthenticationService;
import org.capstone.service.SellerProfileService;
import org.capstone.util.mapper.SellerProfileMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SellerProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
class SellerProfileControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean AuthenticationService authenticationService;
    @MockitoBean SellerProfileService sellerProfileService;
    @MockitoBean SellerProfileMapper profileMapper;

    private final String validRequest = """
            {"storeName":"Berry Store","storeDescription":"Fresh berries","contactInfo":"seller@example.com"}
            """;

    @Test
    void getSellerProfile_ReturnsProfile() throws Exception {
        when(authenticationService.getSellerIdFromAuthenticationContext()).thenReturn(10L);
        SellerProfile profile = SellerProfile.builder().id(10L).storeName("Berry Store").build();
        when(sellerProfileService.getProfileById(10L)).thenReturn(profile);
        when(profileMapper.toResponse(profile)).thenReturn(
                SellerProfileResponse.builder().id(10L).storeName("Berry Store").build());

        mockMvc.perform(get("/api/v1/seller-profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.storeName").value("Berry Store"));
    }

    @Test
    void createSellerProfile_ReturnsProfile() throws Exception {
        User user = User.builder().id(3L).build();
        SellerProfile mapped = SellerProfile.builder().storeName("Berry Store").build();
        SellerProfile saved = SellerProfile.builder().id(10L).storeName("Berry Store").build();
        when(authenticationService.getUserFromAuthenticationContext()).thenReturn(user);
        when(profileMapper.toEntity(any(SellerProfileCreateRequest.class))).thenReturn(mapped);
        when(sellerProfileService.createProfile(user, mapped)).thenReturn(saved);
        when(profileMapper.toResponse(saved)).thenReturn(
                SellerProfileResponse.builder().id(10L).storeName("Berry Store").build());

        mockMvc.perform(post("/api/v1/seller-profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeName").value("Berry Store"));
    }

    @Test
    void updateSellerProfile_ReturnsProfile() throws Exception {
        when(authenticationService.getSellerIdFromAuthenticationContext()).thenReturn(10L);
        SellerProfile mapped = SellerProfile.builder().storeName("Updated Store").build();
        SellerProfile saved = SellerProfile.builder().id(10L).storeName("Updated Store").build();
        when(profileMapper.toEntity(any(SellerProfileCreateRequest.class))).thenReturn(mapped);
        when(sellerProfileService.updateProfile(10L, mapped)).thenReturn(saved);
        when(profileMapper.toResponse(saved)).thenReturn(
                SellerProfileResponse.builder().id(10L).storeName("Updated Store").build());

        mockMvc.perform(patch("/api/v1/seller-profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeName").value("Updated Store"));

        verify(sellerProfileService).updateProfile(10L, mapped);
    }
}
