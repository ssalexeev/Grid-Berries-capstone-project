package org.capstone.controller.seller;


import lombok.RequiredArgsConstructor;
import org.capstone.domain.SellerProfile;
import org.capstone.domain.User;
import org.capstone.dto.request.SellerProfileCreateRequest;
import org.capstone.dto.response.SellerProfileResponse;
import org.capstone.security.AuthenticationService;
import org.capstone.service.SellerProfileService;
import org.capstone.util.mapper.SellerProfileMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;


@RestController
@RequestMapping("/api/v1/seller-profile")
@RequiredArgsConstructor
public class SellerProfileController {
    private final AuthenticationService authenticationService;
    private final SellerProfileService sellerProfileService;
    private final SellerProfileMapper profileMapper;

    @GetMapping
    public ResponseEntity<SellerProfileResponse> getSellerProfile(){
        Long sellerId = authenticationService.getSellerIdFromAuthenticationContext();
        SellerProfile profile = sellerProfileService.getProfileById(sellerId);

        return ResponseEntity.ok(profileMapper.toResponse(profile));
    }

    @PostMapping
    public ResponseEntity<SellerProfileResponse> createSellerProfile(@RequestBody @Validated SellerProfileCreateRequest request){
        User user = authenticationService.getUserFromAuthenticationContext();

        SellerProfile profile = sellerProfileService.createProfile(user, profileMapper.toEntity(request));

        return ResponseEntity.ok(profileMapper.toResponse(profile));
    }


    //todo
    @PatchMapping
    public ResponseEntity<SellerProfileResponse> updateSellerProfile(@Validated @RequestBody SellerProfileCreateRequest request){
        Long sellerId = authenticationService.getSellerIdFromAuthenticationContext();
        SellerProfile sellerProfile = sellerProfileService.updateProfile(sellerId, profileMapper.toEntity(request));

        return ResponseEntity.ok(profileMapper.toResponse(sellerProfile));
    }

}
